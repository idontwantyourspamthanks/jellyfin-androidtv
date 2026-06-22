package org.jellyfin.androidtv.ui.screentime

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.koin.compose.koinInject

private val Magenta = Color(0xFF990099)
private const val EPISODE_STEP = 1
private const val EPISODE_MAX = 50
private const val MINUTE_STEP = 15
private const val MINUTE_MAX = 600

@Composable
fun ScreenTimeLimitsDialog(
	visible: Boolean,
	onDismissRequest: () -> Unit,
) {
	val repository = koinInject<ScreenTimeRepository>()
	val state by repository.state.collectAsState()

	DialogBase(visible = visible, onDismissRequest = onDismissRequest) {
		// Seed the editable values from the stored limits each time the dialog opens. Changes apply
		// live as the parent steps them, so the home readout updates behind the dialog.
		var episodes by remember(visible) { mutableStateOf(state.maxEpisodes) }
		var minutes by remember(visible) { mutableStateOf(state.maxMinutes) }

		val firstStepperFocus = remember { FocusRequester() }
		LaunchedEffect(visible) { if (visible) firstStepperFocus.requestFocus() }

		Column(
			modifier = Modifier
				.width(480.dp)
				.clip(RoundedCornerShape(20.dp))
				.background(JellyfinTheme.colorScheme.surface)
				.padding(36.dp),
			verticalArrangement = Arrangement.spacedBy(20.dp),
		) {
			Text(
				text = stringResource(R.string.screen_time_title),
				fontSize = 28.sp,
				fontWeight = FontWeight.Bold,
				color = Color.White,
			)
			Text(
				text = stringResource(
					R.string.screen_time_today_summary,
					state.episodesToday,
					state.minutesToday,
				),
				fontSize = 14.sp,
				color = Color.White.copy(alpha = 0.6f),
			)

			StepperRow(
				modifier = Modifier.focusRequester(firstStepperFocus),
				label = stringResource(R.string.screen_time_max_episodes),
				value = episodes,
				valueText = if (episodes == 0) stringResource(R.string.screen_time_no_limit) else "$episodes",
				step = EPISODE_STEP,
				min = 0,
				max = EPISODE_MAX,
				onChange = {
					episodes = it
					repository.setLimits(it, minutes)
				},
			)

			StepperRow(
				label = stringResource(R.string.screen_time_max_minutes),
				value = minutes,
				valueText = if (minutes == 0) stringResource(R.string.screen_time_no_limit)
				else stringResource(R.string.screen_time_minutes_value, minutes),
				step = MINUTE_STEP,
				min = 0,
				max = MINUTE_MAX,
				onChange = {
					minutes = it
					repository.setLimits(episodes, it)
				},
			)

			Text(
				text = stringResource(R.string.screen_time_adjust_hint),
				fontSize = 13.sp,
				color = Color.White.copy(alpha = 0.5f),
			)

			Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
				Button(
					onClick = onDismissRequest,
					content = { Text(stringResource(R.string.screen_time_done)) },
				)
			}
		}
	}
}

/**
 * A D-pad friendly stepper: focus the pill, then press left/right to change the value. The +/-
 * glyphs are affordances; the value is the hero. Lights magenta with white content when focused.
 */
@Composable
private fun StepperRow(
	label: String,
	value: Int,
	valueText: String,
	step: Int,
	min: Int,
	max: Int,
	onChange: (Int) -> Unit,
	modifier: Modifier = Modifier,
) {
	val interaction = remember { MutableInteractionSource() }
	val focused by interaction.collectIsFocusedAsState()
	val containerColor = if (focused) Magenta else Color.White.copy(alpha = 0.08f)
	val contentColor = if (focused) Color.White else Color.White.copy(alpha = 0.85f)

	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text(text = label, fontSize = 15.sp, color = Color.White.copy(alpha = 0.7f))

		Row(
			modifier = modifier
				.fillMaxWidth()
				.clip(RoundedCornerShape(14.dp))
				.background(containerColor)
				.onKeyEvent { event ->
					if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
					when (event.key) {
						Key.DirectionLeft -> {
							onChange((value - step).coerceAtLeast(min))
							true
						}

						Key.DirectionRight -> {
							onChange((value + step).coerceAtMost(max))
							true
						}

						else -> false
					}
				}
				.focusable(interactionSource = interaction)
				.padding(horizontal = 24.dp, vertical = 16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			StepGlyph(symbol = "−", enabled = value > min, color = contentColor)
			Text(text = valueText, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = contentColor)
			StepGlyph(symbol = "+", enabled = value < max, color = contentColor)
		}
	}
}

@Composable
private fun StepGlyph(symbol: String, enabled: Boolean, color: Color) {
	Text(
		text = symbol,
		fontSize = 30.sp,
		fontWeight = FontWeight.Bold,
		color = if (enabled) color else color.copy(alpha = 0.3f),
	)
}
