package org.jellyfin.androidtv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Searches episodes within a single show. Unlike [SearchViewModel] this is scoped to one series and
 * only returns episodes.
 */
class EpisodeSearchViewModel(
	private val searchRepository: SearchRepository
) : ViewModel() {
	companion object {
		private val debounceDuration = 600.milliseconds
	}

	private var searchJob: Job? = null
	private var previousQuery: String? = null
	private var seriesId: UUID? = null
	private var includeDescriptions = false

	// Cache of all episodes, loaded lazily the first time a description search runs.
	private var allEpisodes: List<BaseItemDto>? = null

	private val _searchResultsFlow = MutableStateFlow<List<BaseItemDto>>(emptyList())
	val searchResultsFlow = _searchResultsFlow.asStateFlow()

	fun setSeries(seriesId: UUID) {
		this.seriesId = seriesId
	}

	fun setIncludeDescriptions(value: Boolean) {
		includeDescriptions = value
		// Force the next search to run even if the query text is unchanged
		previousQuery = null
	}

	fun searchImmediately(query: String) = searchDebounced(query, 0.milliseconds)

	fun searchDebounced(query: String, debounce: Duration = debounceDuration) {
		val trimmed = query.trim()
		if (trimmed == previousQuery) return
		previousQuery = trimmed

		searchJob?.cancel()

		if (trimmed.isBlank()) {
			_searchResultsFlow.value = emptyList()
			return
		}

		searchJob = viewModelScope.launch {
			delay(debounce)

			_searchResultsFlow.value = if (includeDescriptions) {
				// searchTerm doesn't match overviews, so filter the show's episodes locally
				cachedEpisodes().filter { episode ->
					episode.name?.contains(trimmed, ignoreCase = true) == true ||
						episode.overview?.contains(trimmed, ignoreCase = true) == true
				}
			} else {
				searchRepository.search(trimmed, setOf(BaseItemKind.EPISODE), seriesId).getOrNull().orEmpty()
			}
		}
	}

	private suspend fun cachedEpisodes(): List<BaseItemDto> {
		allEpisodes?.let { return it }
		val seriesId = seriesId ?: return emptyList()
		return searchRepository.getEpisodes(seriesId).getOrNull().orEmpty().also { allEpisodes = it }
	}
}
