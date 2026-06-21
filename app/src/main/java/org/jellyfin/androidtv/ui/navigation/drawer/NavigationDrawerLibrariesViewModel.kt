package org.jellyfin.androidtv.ui.navigation.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.jellyfin.androidtv.data.repository.LibraryDisplayPreferences
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Supplies the library list shown in the navigation drawer, in the user's chosen order and with
 * hidden libraries removed (see [LibraryDisplayPreferences]).
 *
 * [UserViewsRepository.views] is a cold flow that hits the server on every collection, so it is
 * cached here via [stateIn]. The user's order/visibility lives in an async preference that does not
 * emit on change, so [refresh] re-reads it - call it when the drawer opens so edits show up.
 */
class NavigationDrawerLibrariesViewModel(
	userViewsRepository: UserViewsRepository,
	private val userSettingPreferences: UserSettingPreferences,
) : ViewModel() {
	private val rawViews = userViewsRepository.views
		.map { it.toList() }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	private val refreshTrigger = MutableStateFlow(0)

	val libraries: StateFlow<List<BaseItemDto>> = combine(rawViews, refreshTrigger) { views, _ ->
		LibraryDisplayPreferences.applyVisible(views, currentState())
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	/** Re-read the order/visibility preference so recent edits are reflected. */
	fun refresh() {
		refreshTrigger.value++
	}

	private fun currentState() =
		LibraryDisplayPreferences.parse(userSettingPreferences[UserSettingPreferences.libraryDisplay])
}
