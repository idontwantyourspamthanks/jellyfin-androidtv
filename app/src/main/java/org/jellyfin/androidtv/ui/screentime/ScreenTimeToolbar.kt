package org.jellyfin.androidtv.ui.screentime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.shared.toolbar.Toolbar
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarButtons
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarClock
import org.koin.compose.koinInject

/**
 * Slim top bar for the home screen showing today's screen time tally and a button to set the daily
 * limits. The icon button is focusable so it can be reached by pressing up from the top home row.
 */
@Composable
fun ScreenTimeToolbar(
	modifier: Modifier = Modifier,
) {
	val repository = koinInject<ScreenTimeRepository>()
	val state by repository.state.collectAsState()
	var showDialog by remember { mutableStateOf(false) }

	// Re-read the tally whenever the bar appears so it reflects a midnight rollover or episodes
	// watched since it was last shown.
	LaunchedEffect(Unit) { repository.refresh() }

	Toolbar(
		modifier = modifier,
		start = {},
		end = {
			ToolbarButtons {
				Text(
					text = state.readout(),
					color = if (state.limitReached) JellyfinTheme.colorScheme.recording else Color.White,
					fontSize = 16.sp,
				)

				IconButton(
					onClick = { showDialog = true },
				) {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_screen_time),
						contentDescription = stringResource(R.string.screen_time_set_limits),
					)
				}

				ToolbarClock()
			}
		},
	)

	ScreenTimeLimitsDialog(
		visible = showDialog,
		onDismissRequest = { showDialog = false },
	)
}
