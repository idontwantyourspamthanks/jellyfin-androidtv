package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.screentime.ScreenTimeRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber

/**
 * The home "Watched today" row: the episodes the kids have played today, most recent first. Like the
 * "Now playing" row it adds itself only when there's something to show and removes itself when empty,
 * so the home screen stays clean on a fresh day. Refreshed from [HomeRowsFragment.onResume].
 */
class HomeFragmentWatchedTodayRow(
	private val scope: CoroutineScope,
	private val api: ApiClient,
	private val screenTime: ScreenTimeRepository,
) : HomeFragmentRow {
	private val cardPresenter = CardPresenter()
	private var adapter: ArrayObjectAdapter? = null
	private var row: ListRow? = null

	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		update(context, rowsAdapter)
	}

	fun update(context: Context, rowsAdapter: MutableObjectAdapter<Row>) {
		scope.launch {
			val episodes = loadWatchedEpisodes()

			if (episodes.isEmpty()) {
				row?.let { rowsAdapter.remove(it) }
				row = null
				adapter = null
				return@launch
			}

			val adapter = adapter ?: ArrayObjectAdapter(cardPresenter).also { adapter = it }
			adapter.clear()
			for (episode in episodes) adapter.add(BaseItemDtoBaseRowItem(episode, staticHeight = true))

			val row = row ?: ListRow(
				HeaderItem(context.getString(R.string.lbl_watched_today)),
				adapter,
			).also { row = it }
			// Sits last, after Favourites, so appending keeps it in place.
			if (!rowsAdapter.contains(row)) rowsAdapter.add(row)
		}
	}

	private suspend fun loadWatchedEpisodes(): List<BaseItemDto> {
		val ids = screenTime.watchedEpisodeIds()
		if (ids.isEmpty()) return emptyList()

		val items = runCatching {
			withContext(Dispatchers.IO) {
				val response by api.itemsApi.getItems(
					ids = ids,
					fields = ItemRepository.browseFields,
					imageTypeLimit = 1,
					enableTotalRecordCount = false,
				)
				response.items
			}
		}.getOrElse { error ->
			Timber.w(error, "Failed to load watched-today episodes for the home row")
			return emptyList()
		}

		// getItems doesn't preserve the requested id order, so restore the most-recent-first order.
		val byId = items.associateBy { it.id }
		return ids.mapNotNull { byId[it] }
	}
}
