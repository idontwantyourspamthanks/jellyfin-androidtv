package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.databinding.ClockUserBugBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.screentime.ScreenTimeRepository
import org.jellyfin.androidtv.ui.screentime.ScreenTimeState
import org.jellyfin.androidtv.ui.screentime.readout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClockUserView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
	private val binding: ClockUserBugBinding = ClockUserBugBinding.inflate(LayoutInflater.from(context), this, true)
	private val userPreferences by inject<UserPreferences>()
	private val navigationRepository by inject<NavigationRepository>()
	private val screenTimeRepository by inject<ScreenTimeRepository>()

	private var screenTimeJob: Job? = null

	var isVideoPlayer = false
		set(value) {
			field = value
			updateClockVisibility()
		}

	val homeButton get() = binding.home

	init {
		updateClockVisibility()

		binding.home.setOnClickListener {
			navigationRepository.reset(Destinations.home, clearHistory = true)
		}
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()

		// Mirror today's screen time tally next to the clock, updating live as episodes play. The
		// readout is hidden inside the video player, like the home button.
		screenTimeRepository.refresh()
		val lifecycleOwner = findViewTreeLifecycleOwner() ?: return
		screenTimeJob = lifecycleOwner.lifecycleScope.launch {
			lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				screenTimeRepository.state.collect(::bindScreenTime)
			}
		}
	}

	override fun onDetachedFromWindow() {
		screenTimeJob?.cancel()
		screenTimeJob = null
		super.onDetachedFromWindow()
	}

	private fun bindScreenTime(state: ScreenTimeState) {
		binding.screenTimeReadout.text = state.readout()
		binding.screenTimeReadout.setTextColor(if (state.limitReached) LIMIT_REACHED_COLOR else Color.WHITE)
		binding.screenTimeReadout.isVisible = !isVideoPlayer
	}

	private fun updateClockVisibility() {
		val showClock = userPreferences[UserPreferences.clockBehavior]

		binding.clock.isVisible = when (showClock) {
			ClockBehavior.ALWAYS -> true
			ClockBehavior.NEVER -> false
			ClockBehavior.IN_VIDEO -> isVideoPlayer
			ClockBehavior.IN_MENUS -> !isVideoPlayer
		}

		binding.home.isVisible = !isVideoPlayer
		binding.screenTimeReadout.isVisible = !isVideoPlayer
	}

	private companion object {
		// Matches the home toolbar's limit-reached colour (design colorRed300).
		private val LIMIT_REACHED_COLOR = Color.parseColor("#FB7E7E")
	}
}
