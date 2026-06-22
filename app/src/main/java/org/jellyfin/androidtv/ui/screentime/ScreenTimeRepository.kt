package org.jellyfin.androidtv.ui.screentime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.androidtv.preference.UserPreferences
import java.util.Calendar
import java.util.UUID

/**
 * A snapshot of today's screen time tally and the active daily limits.
 *
 * A limit of 0 means "no limit". [limitReached] is true once any set limit has been met, which is
 * the signal used to block further episode playback.
 */
data class ScreenTimeState(
	val episodesToday: Int = 0,
	val minutesToday: Int = 0,
	val maxEpisodes: Int = 0,
	val maxMinutes: Int = 0,
) {
	val hasLimits: Boolean get() = maxEpisodes > 0 || maxMinutes > 0

	val limitReached: Boolean
		get() = (maxEpisodes > 0 && episodesToday >= maxEpisodes) ||
			(maxMinutes > 0 && minutesToday >= maxMinutes)
}

/** Compact "episodes · minutes" readout, showing "used/max" for whichever limits are set. */
fun ScreenTimeState.readout(): String {
	val episodes = if (maxEpisodes > 0) "$episodesToday/$maxEpisodes" else "$episodesToday"
	val minutes = if (maxMinutes > 0) "$minutesToday/${maxMinutes}m" else "${minutesToday}m"
	return "$episodes · $minutes"
}

/**
 * Tracks how much the kids have watched today and enforces optional daily limits.
 *
 * Episodes are counted by whole runtime at the moment they start playing - we deliberately don't
 * measure how much of each episode was actually watched, which keeps this simple and errs on the
 * safe (over-counting) side for a screen time guardrail.
 *
 * The tally resets at local midnight. Rather than run a scheduled job we stamp each tally with the
 * day it belongs to and lazily zero it whenever a new day is observed, so the reset happens the
 * first time anything reads or writes after midnight.
 */
class ScreenTimeRepository(
	private val userPreferences: UserPreferences,
) {
	private val _state = MutableStateFlow(loadState())
	val state: StateFlow<ScreenTimeState> = _state.asStateFlow()

	/** Whether starting another episode right now would exceed a set limit. */
	fun isLimitReached(): Boolean = loadState().limitReached

	/** Records a started episode against today's tally. [runTimeTicks] may be null. */
	fun recordEpisode(id: UUID, runTimeTicks: Long?) {
		rolloverIfNewDay()
		val minutes = ((runTimeTicks ?: 0L) / TICKS_PER_MINUTE).toInt()
		userPreferences[UserPreferences.screenTimeEpisodesToday] =
			userPreferences[UserPreferences.screenTimeEpisodesToday] + 1
		userPreferences[UserPreferences.screenTimeMinutesToday] =
			userPreferences[UserPreferences.screenTimeMinutesToday] + minutes
		val watched = userPreferences[UserPreferences.screenTimeWatchedIds]
		userPreferences[UserPreferences.screenTimeWatchedIds] =
			if (watched.isBlank()) id.toString() else "$watched,$id"
		refresh()
	}

	/** Distinct episodes started today, most recent first. */
	fun watchedEpisodeIds(): List<UUID> {
		rolloverIfNewDay()
		return userPreferences[UserPreferences.screenTimeWatchedIds]
			.split(',')
			.filter { it.isNotBlank() }
			.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
			.asReversed()
			.distinct()
	}

	/** Sets today's limits. They persist to future days until changed. 0 clears a limit. */
	fun setLimits(maxEpisodes: Int, maxMinutes: Int) {
		userPreferences[UserPreferences.screenTimeMaxEpisodes] = maxEpisodes.coerceAtLeast(0)
		userPreferences[UserPreferences.screenTimeMaxMinutes] = maxMinutes.coerceAtLeast(0)
		refresh()
	}

	/** Re-reads from storage, picking up a midnight rollover or external changes. */
	fun refresh() {
		_state.value = loadState()
	}

	private fun loadState(): ScreenTimeState {
		rolloverIfNewDay()
		return ScreenTimeState(
			episodesToday = userPreferences[UserPreferences.screenTimeEpisodesToday],
			minutesToday = userPreferences[UserPreferences.screenTimeMinutesToday],
			maxEpisodes = userPreferences[UserPreferences.screenTimeMaxEpisodes],
			maxMinutes = userPreferences[UserPreferences.screenTimeMaxMinutes],
		)
	}

	private fun rolloverIfNewDay() {
		val today = todayKey()
		if (userPreferences[UserPreferences.screenTimeCountDay] != today) {
			userPreferences[UserPreferences.screenTimeCountDay] = today
			userPreferences[UserPreferences.screenTimeEpisodesToday] = 0
			userPreferences[UserPreferences.screenTimeMinutesToday] = 0
			userPreferences[UserPreferences.screenTimeWatchedIds] = ""
		}
	}

	private fun todayKey(): Int = Calendar.getInstance().run {
		get(Calendar.YEAR) * 1000 + get(Calendar.DAY_OF_YEAR)
	}

	private companion object {
		private const val TICKS_PER_MINUTE = 600_000_000L
	}
}
