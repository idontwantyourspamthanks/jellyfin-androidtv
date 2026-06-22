package org.jellyfin.androidtv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import java.text.Normalizer
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
		private val DIACRITICS = Regex("\\p{Mn}+")
		private val APOSTROPHES = Regex("['‘’ʼ`´]")
	}

	private var searchJob: Job? = null
	private var previousQuery: String? = null
	private var seriesId: UUID? = null

	// Cache of all episodes paired with a pre-folded "name + overview" haystack, loaded lazily the
	// first time a search runs. Folding once here keeps per-keystroke filtering cheap.
	private var allEpisodes: List<SearchableEpisode>? = null

	private data class SearchableEpisode(val episode: BaseItemDto, val haystack: String)

	private val _searchResultsFlow = MutableStateFlow<List<BaseItemDto>>(emptyList())
	val searchResultsFlow = _searchResultsFlow.asStateFlow()

	fun setSeries(seriesId: UUID) {
		this.seriesId = seriesId
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

			// The server's searchTerm doesn't match overviews, so filter the show's episodes
			// locally to search titles and descriptions together. Both sides are folded so that
			// curly apostrophes, accents and the like don't stop a match.
			val needle = trimmed.foldForSearch()
			_searchResultsFlow.value = cachedEpisodes()
				.filter { it.haystack.contains(needle) }
				.map { it.episode }
		}
	}

	private suspend fun cachedEpisodes(): List<SearchableEpisode> {
		allEpisodes?.let { return it }
		val seriesId = seriesId ?: return emptyList()
		return searchRepository.getEpisodes(seriesId).getOrNull().orEmpty()
			.map { episode ->
				val haystack = "${episode.name.orEmpty()} ${episode.overview.orEmpty()}".foldForSearch()
				SearchableEpisode(episode, haystack)
			}
			.also { allEpisodes = it }
	}

	/**
	 * Canonicalises text for forgiving matching: strips diacritics, drops apostrophe variants
	 * entirely (so "pigs" matches "Pig's") and lowercases.
	 */
	private fun String.foldForSearch(): String =
		Normalizer.normalize(this, Normalizer.Form.NFKD)
			.replace(DIACRITICS, "")
			.replace(APOSTROPHES, "")
			.lowercase()
}
