package org.jellyfin.androidtv.ui.navigation.drawer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the open/closed state of the app-wide navigation drawer. Activity-scoped (mirrors
 * [org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel]) so the activity's key handling and
 * the drawer composable share one instance.
 *
 * While [suppressed] - currently during video/photo playback - the drawer stays closed and cannot
 * be opened, so it never steals focus from an immersive screen.
 */
class NavigationDrawerViewModel : ViewModel() {
	private val _open = MutableStateFlow(false)
	val open = _open.asStateFlow()

	private val _suppressed = MutableStateFlow(false)
	val suppressed = _suppressed.asStateFlow()

	fun open() {
		if (!_suppressed.value) _open.value = true
	}

	fun close() {
		_open.value = false
	}

	fun toggle() {
		if (_open.value) close() else open()
	}

	fun setSuppressed(value: Boolean) {
		_suppressed.value = value
		if (value) _open.value = false
	}
}
