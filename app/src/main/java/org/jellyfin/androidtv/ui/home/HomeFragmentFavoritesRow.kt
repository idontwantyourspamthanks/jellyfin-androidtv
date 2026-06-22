package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import timber.log.Timber

/**
 * The home "Favourites" row: a "Surprise me" tile (plays a random episode of a random favourite)
 * followed by the user's favourite shows and films. Clicking a favourite series opens a small play
 * menu (handled in [HomeRowsFragment]); the surprise tile is a [GridButton] routed by its id.
 */
class HomeFragmentFavoritesRow(
	private val api: ApiClient,
	private val scope: CoroutineScope,
) : HomeFragmentRow {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val presenterSelector = ClassPresenterSelector().apply {
			// Match the poster cards (staticHeight 150, 2:3) so the surprise tile is the same shape.
			addClassPresenter(GridButton::class.java, GridButtonPresenter(width = 100, imageHeight = 150, centerIcon = true))
			addClassPresenter(BaseRowItem::class.java, cardPresenter)
		}
		val adapter = ArrayObjectAdapter(presenterSelector)
		adapter.add(GridButton(SURPRISE_ME_OPTION_ID, context.getString(R.string.lbl_surprise_me), R.drawable.ic_question_mark))

		val header = HeaderItem(rowsAdapter.size().toLong(), context.getString(R.string.lbl_favorites))
		rowsAdapter.add(ListRow(header, adapter))

		// Load favourites after the row is in place so the surprise tile shows immediately.
		scope.launch {
			val favorites = runCatching {
				withContext(Dispatchers.IO) {
					val response by api.itemsApi.getItems(
						isFavorite = true,
						includeItemTypes = setOf(BaseItemKind.SERIES, BaseItemKind.MOVIE),
						recursive = true,
						sortBy = setOf(ItemSortBy.SORT_NAME),
						fields = ItemRepository.browseFields,
						imageTypeLimit = 1,
						enableTotalRecordCount = false,
						limit = FAVORITES_LIMIT,
					)
					response.items
				}
			}.getOrElse { error ->
				Timber.w(error, "Failed to load favourites for the home row")
				emptyList()
			}

			for (favorite in favorites) adapter.add(BaseItemDtoBaseRowItem(favorite, staticHeight = true))
		}
	}

	companion object {
		const val SURPRISE_ME_OPTION_ID = 9001
		private const val FAVORITES_LIMIT = 50
	}
}
