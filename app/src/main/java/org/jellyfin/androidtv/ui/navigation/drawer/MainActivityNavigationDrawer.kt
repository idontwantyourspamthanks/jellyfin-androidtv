package org.jellyfin.androidtv.ui.navigation.drawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.ui.navigation.NavigationAction
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.AudioNowPlayingFragment
import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment
import org.jellyfin.androidtv.ui.player.photo.PhotoPlayerFragment
import org.jellyfin.androidtv.ui.player.video.VideoPlayerFragment
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import kotlin.reflect.KClass

/**
 * Entry point for the navigation drawer, added to [org.jellyfin.androidtv.ui.browsing.MainActivity]'s
 * Compose content (mirrors `MainActivitySettings`). Observes navigation to suppress the drawer on
 * immersive (player) screens and feeds the library list into [NavigationDrawer].
 */
@Composable
fun MainActivityNavigationDrawer() {
	val viewModel = koinActivityViewModel<NavigationDrawerViewModel>()
	val librariesViewModel = koinActivityViewModel<NavigationDrawerLibrariesViewModel>()
	val navigationRepository = koinInject<NavigationRepository>()

	val open by viewModel.open.collectAsState()
	val libraries by librariesViewModel.libraries.collectAsState()

	// Re-read the saved order/visibility each time the drawer opens so edits show up immediately.
	LaunchedEffect(open) {
		if (open) librariesViewModel.refresh()
	}

	// Hide and lock the drawer while a player/photo screen is the current destination. Navigating
	// into a player suppresses it; navigating away or backing out (e.g. exiting playback) releases it.
	LaunchedEffect(Unit) {
		navigationRepository.currentAction.collect { action ->
			when (action) {
				is NavigationAction.NavigateFragment ->
					viewModel.setSuppressed(action.destination.fragment in IMMERSIVE_FRAGMENTS)
				NavigationAction.GoBack -> viewModel.setSuppressed(false)
				NavigationAction.Nothing -> Unit
			}
		}
	}

	NavigationDrawer(
		visible = open,
		libraries = libraries,
		onDismissRequest = viewModel::close,
	)
}

/** Destinations the drawer must stay closed for - it would steal focus from playback. */
private val IMMERSIVE_FRAGMENTS: Set<KClass<out Fragment>> = setOf(
	CustomPlaybackOverlayFragment::class,
	VideoPlayerFragment::class,
	PhotoPlayerFragment::class,
	AudioNowPlayingFragment::class,
)
