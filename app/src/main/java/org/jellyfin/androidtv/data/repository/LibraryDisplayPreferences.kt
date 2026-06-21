package org.jellyfin.androidtv.data.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * The user's chosen library order and hidden set, persisted (per-user) as JSON in a preference.
 *
 * @param order library ids (as strings) in display order; libraries missing from this list are
 *   treated as new and shown after the ordered ones.
 * @param hidden library ids the user has chosen to hide.
 */
@Serializable
data class LibraryDisplayState(
	val order: List<String> = emptyList(),
	val hidden: Set<String> = emptySet(),
)

/**
 * Applies a [LibraryDisplayState] to a server-provided library list. Used in both places libraries
 * are shown - the navigation drawer and the home "My Media" row - so they stay consistent.
 */
object LibraryDisplayPreferences {
	private val json = Json { ignoreUnknownKeys = true }

	fun parse(serialized: String): LibraryDisplayState =
		if (serialized.isBlank()) LibraryDisplayState()
		else runCatching { json.decodeFromString<LibraryDisplayState>(serialized) }
			.getOrDefault(LibraryDisplayState())

	fun encode(state: LibraryDisplayState): String = json.encodeToString(state)

	/**
	 * All libraries in the user's order, with any not yet in [LibraryDisplayState.order] appended in
	 * their original server order. Hidden libraries are kept (for the editor, which must show them).
	 */
	fun order(views: List<BaseItemDto>, state: LibraryDisplayState): List<BaseItemDto> {
		val byId = views.associateBy { it.id.toString() }
		val ordered = state.order.mapNotNull(byId::get)
		val remaining = views.filterNot { it.id.toString() in state.order }
		return ordered + remaining
	}

	/** Visible libraries only, in the user's order. Used by the drawer and the My Media row. */
	fun applyVisible(views: List<BaseItemDto>, state: LibraryDisplayState): List<BaseItemDto> =
		order(views, state).filterNot { it.id.toString() in state.hidden }
}
