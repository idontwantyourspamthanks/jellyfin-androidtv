package org.jellyfin.androidtv.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.leanback.app.RowsSupportFragment
import org.jellyfin.androidtv.R
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

		Column(modifier = Modifier.padding(top = 20.dp)) {
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
					.padding(horizontal = 48.dp)
					.focusRequester(textInputFocusRequester),
			)

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
