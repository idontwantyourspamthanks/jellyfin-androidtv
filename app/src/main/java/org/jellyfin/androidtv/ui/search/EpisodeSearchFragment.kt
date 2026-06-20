package org.jellyfin.androidtv.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.leanback.app.RowsSupportFragment
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.search.composable.SearchTextInput
import org.jellyfin.androidtv.util.Utils
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Standalone screen to search the episodes of a single show. Reached from the show/season/episode
 * detail pages. A text input over a horizontal carousel of matching episodes.
 */
class EpisodeSearchFragment : Fragment() {
	companion object {
		const val EXTRA_SERIES_ID = "seriesId"
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val viewModel = koinViewModel<EpisodeSearchViewModel>()
		val delegate = koinInject<SearchFragmentDelegate> { parametersOf(requireContext()) }
		var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
		val textInputFocusRequester = remember { FocusRequester() }
		val resultFocusRequester = remember { FocusRequester() }

		LaunchedEffect(Unit) {
			Utils.uuidOrNull(arguments?.getString(EXTRA_SERIES_ID))?.let { viewModel.setSeries(it) }
			textInputFocusRequester.requestFocus()

			viewModel.searchResultsFlow.collect { episodes ->
				delegate.showResults(listOf(SearchResultGroup(R.string.lbl_episodes, episodes)))
			}
		}

		var includeDescriptions by rememberSaveable { mutableStateOf(false) }

		Column(modifier = Modifier.padding(top = 20.dp)) {
			Row(
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.focusRestorer()
					.focusGroup()
					.padding(horizontal = 48.dp),
			) {
				SearchTextInput(
					query = query.text,
					onQueryChange = {
						query = query.copy(text = it)
						viewModel.searchDebounced(query.text)
					},
					onQuerySubmit = {
						viewModel.searchImmediately(query.text)
						// Move focus away from the field so vendors with a fullscreen keyboard don't soft-lock
						resultFocusRequester.requestFocus()
					},
					modifier = Modifier
						.weight(1f)
						.focusRequester(textInputFocusRequester),
				)

				DescriptionToggle(
					checked = includeDescriptions,
					onToggle = {
						includeDescriptions = !includeDescriptions
						viewModel.setIncludeDescriptions(includeDescriptions)
						viewModel.searchImmediately(query.text)
					},
				)
			}

			var rowsSupportFragment by remember { mutableStateOf<RowsSupportFragment?>(null) }

			AndroidFragment<RowsSupportFragment>(
				modifier = Modifier
					.focusGroup()
					.focusRequester(resultFocusRequester)
					.focusProperties {
						onExit = {
							val isFirstRowSelected = rowsSupportFragment?.selectedPosition?.let { it <= 0 } ?: false
							if (requestedFocusDirection != FocusDirection.Up || !isFirstRowSelected) {
								cancelFocusChange()
							} else {
								rowsSupportFragment?.selectedPosition = 0
								rowsSupportFragment?.verticalGridView?.clearFocus()
							}
						}
					}
					.padding(top = 16.dp)
					.fillMaxSize(),
				onUpdate = { fragment ->
					rowsSupportFragment = fragment
					fragment.adapter = delegate.rowsAdapter
					fragment.onItemViewClickedListener = delegate.onItemViewClickedListener
					fragment.onItemViewSelectedListener = delegate.onItemViewSelectedListener
				}
			)
		}
	}
}

@Composable
private fun DescriptionToggle(
	checked: Boolean,
	onToggle: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val interactionSource = remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val shape = RoundedCornerShape(percent = 30)

	val background = if (focused) JellyfinTheme.colorScheme.buttonFocused else JellyfinTheme.colorScheme.button
	val foreground = if (focused) JellyfinTheme.colorScheme.onButtonFocused else JellyfinTheme.colorScheme.onButton

	Row(
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalAlignment = Alignment.CenterVertically,
		modifier = modifier
			.clip(shape)
			.background(background)
			.clickable(interactionSource = interactionSource, indication = null) { onToggle() }
			.padding(horizontal = 14.dp, vertical = 12.dp),
	) {
		Checkbox(
			checked = checked,
			containerColor = foreground,
			contentColor = background,
		)
		Text(
			text = stringResource(R.string.lbl_search_descriptions),
			color = foreground,
		)
	}
}
