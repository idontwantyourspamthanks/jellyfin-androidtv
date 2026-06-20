package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.FocusHighlightHelper
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.jellyfin.androidtv.databinding.PopupSeasonSelectionBinding
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

/**
 * A focusable overlay showing the seasons of a show as a horizontal strip of cards.
 * Left/right to browse, click to select (invokes [onSeasonSelected]), back to dismiss.
 */
class SeasonSelectionPopup(
	context: Context,
	private val anchor: View,
	private val onSeasonSelected: (BaseItemDto) -> Unit,
) {
	private val density = anchor.resources.displayMetrics.density
	private val binding = PopupSeasonSelectionBinding.inflate(LayoutInflater.from(context))
	private val seasonAdapter = ArrayObjectAdapter(CardPresenter(true, 180))
	private val popup = PopupWindow(
		binding.root,
		ViewGroup.LayoutParams.WRAP_CONTENT,
		ViewGroup.LayoutParams.WRAP_CONTENT,
	).apply {
		isFocusable = true
		isOutsideTouchable = true
		// Required for the popup to dismiss on outside touch / back
		setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
	}

	init {
		binding.seasonSelectionGrid.setHorizontalSpacing(dp(SPACING_DP))

		val bridgeAdapter = ItemBridgeAdapter(seasonAdapter)
		FocusHighlightHelper.setupBrowseItemFocusHighlight(bridgeAdapter, FocusHighlight.ZOOM_FACTOR_MEDIUM, false)
		bridgeAdapter.setAdapterListener(object : ItemBridgeAdapter.AdapterListener() {
			override fun onBind(viewHolder: ItemBridgeAdapter.ViewHolder) {
				viewHolder.viewHolder.view.setOnClickListener {
					val season = (viewHolder.item as? BaseItemDtoBaseRowItem)?.baseItem ?: return@setOnClickListener
					onSeasonSelected(season)
					dismiss()
				}
			}

			override fun onUnbind(viewHolder: ItemBridgeAdapter.ViewHolder) {
				viewHolder.viewHolder.view.setOnClickListener(null)
			}
		})
		binding.seasonSelectionGrid.adapter = bridgeAdapter
	}

	fun show(seasons: List<BaseItemDto>, currentSeasonId: UUID?) {
		seasonAdapter.clear()
		seasonAdapter.addAll(0, seasons.map { BaseItemDtoBaseRowItem(it) })

		// A floating card inset from the screen edges, sized to leave breathing room around it.
		val screenWidth = anchor.resources.displayMetrics.widthPixels
		val popupWidth = screenWidth - dp(SIDE_MARGIN_DP) * 2
		popup.width = popupWidth

		// Measure so we can place the card's bottom edge just above the button that summoned it.
		binding.root.measure(
			View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
		)

		val anchorLocation = IntArray(2)
		anchor.getLocationOnScreen(anchorLocation)
		val x = dp(SIDE_MARGIN_DP)
		val y = (anchorLocation[1] - binding.root.measuredHeight - dp(GAP_DP)).coerceAtLeast(dp(GAP_DP))

		popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)

		// The cards are rendered with ComposeView, whose fallback window-recomposer resolves the
		// ViewTree owners from the window's root view - here the PopupWindow's decor, which only
		// exists once shown. The cards attach on the next frame, so setting them now is in time.
		binding.root.rootView.apply {
			setViewTreeLifecycleOwner(anchor.findViewTreeLifecycleOwner())
			setViewTreeViewModelStoreOwner(anchor.findViewTreeViewModelStoreOwner())
			setViewTreeSavedStateRegistryOwner(anchor.findViewTreeSavedStateRegistryOwner())
		}

		val currentIndex = seasons.indexOfFirst { it.id == currentSeasonId }
		binding.seasonSelectionGrid.selectedPosition = if (currentIndex >= 0) currentIndex else 0
		binding.seasonSelectionGrid.requestFocus()
	}

	fun dismiss() {
		if (popup.isShowing) popup.dismiss()
	}

	private fun dp(value: Int) = (value * density).toInt()

	private companion object {
		const val SIDE_MARGIN_DP = 56
		const val GAP_DP = 16
		const val SPACING_DP = 20
	}
}
