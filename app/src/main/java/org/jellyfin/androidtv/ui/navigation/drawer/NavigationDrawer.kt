package org.jellyfin.androidtv.ui.navigation.drawer

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.library.libraryIcon
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * App-wide left overlay navigation drawer. Built on [DialogBase] so it lives in its own Compose
 * window: it captures D-pad focus while open and releases it back to the Leanback content when
 * dismissed, which sidesteps the Compose/Leanback focus issues a same-window overlay would hit.
 */
@Composable
fun NavigationDrawer(
	visible: Boolean,
	libraries: List<BaseItemDto>,
	onDismissRequest: () -> Unit,
) {
	DialogBase(
		visible = visible,
		onDismissRequest = onDismissRequest,
		contentAlignment = Alignment.TopStart,
		enterTransition = slideInHorizontally { -it } + fadeIn(),
		exitTransition = slideOutHorizontally { -it } + fadeOut(),
	) {
		DrawerContent(
			libraries = libraries,
			onDismissRequest = onDismissRequest,
		)
	}
}

@Composable
private fun BoxScope.DrawerContent(
	libraries: List<BaseItemDto>,
	onDismissRequest: () -> Unit,
) {
	val navigationRepository = koinInject<NavigationRepository>()
	val settingsViewModel = koinActivityViewModel<SettingsViewModel>()
	val itemLauncher = koinInject<ItemLauncher>()
	val mediaManager = koinInject<MediaManager>()
	val sessionRepository = koinInject<SessionRepository>()
	val activity = LocalActivity.current

	// Focus lands on the first item (Home) when the drawer opens.
	val firstItemFocusRequester = remember { FocusRequester() }

	Column(
		modifier = Modifier
			.width(360.dp)
			.fillMaxHeight()
			.background(JellyfinTheme.colorScheme.surface)
			.verticalScroll(rememberScrollState())
			.focusGroup()
			// Right collapses the drawer, mirroring left-to-open from the content.
			.onPreviewKeyEvent { event ->
				if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
					onDismissRequest()
					true
				} else {
					false
				}
			}
			.padding(vertical = 24.dp),
		verticalArrangement = Arrangement.spacedBy(2.dp),
	) {
		Text(
			text = "KoalaPaw",
			color = Color.White,
			fontSize = 28.sp,
			fontWeight = FontWeight.Bold,
			modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
		)

		ListButton(
			modifier = Modifier.focusRequester(firstItemFocusRequester),
			leadingContent = { Icon(painterResource(R.drawable.ic_house), contentDescription = null) },
			headingContent = { Text(stringResource(R.string.lbl_home)) },
			onClick = {
				navigationRepository.navigate(Destinations.home, replace = true)
				onDismissRequest()
			},
		)

		libraries.forEach { library ->
			ListButton(
				leadingContent = { Icon(painterResource(libraryIcon(library.collectionType)), contentDescription = null) },
				headingContent = { Text(library.name.orEmpty()) },
				onClick = {
					itemLauncher.launchUserView(library)
					onDismissRequest()
				},
			)
		}

		ListButton(
			leadingContent = { Icon(painterResource(R.drawable.ic_house_edit), contentDescription = null) },
			headingContent = { Text(stringResource(R.string.lbl_edit_libraries)) },
			onClick = {
				navigationRepository.navigate(Destinations.libraryManager)
				onDismissRequest()
			},
		)

		ListButton(
			leadingContent = { Icon(painterResource(R.drawable.ic_settings), contentDescription = null) },
			headingContent = { Text(stringResource(R.string.lbl_settings)) },
			onClick = {
				settingsViewModel.show()
				onDismissRequest()
			},
		)

		ListButton(
			leadingContent = { Icon(painterResource(R.drawable.ic_user), contentDescription = null) },
			headingContent = { Text(stringResource(R.string.lbl_switch_user)) },
			onClick = {
				mediaManager.clearAudioQueue()
				sessionRepository.destroyCurrentSession()
				activity?.startActivity(ActivityDestinations.startup(activity))
				activity?.finishAfterTransition()
			},
		)
	}

	LaunchedEffect(Unit) { firstItemFocusRequester.requestFocus() }
}
