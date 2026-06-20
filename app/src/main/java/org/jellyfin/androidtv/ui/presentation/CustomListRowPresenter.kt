package org.jellyfin.androidtv.ui.presentation

import android.view.View
import androidx.core.view.isVisible
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.RowPresenter
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter

open class CustomListRowPresenter @JvmOverloads constructor(
	private val topPadding: Int? = null
) : ListRowPresenter() {
	init {
		headerPresenter = CustomRowHeaderPresenter()
	}

	override fun isUsingDefaultShadow() = false

	override fun onSelectLevelChanged(holder: RowPresenter.ViewHolder) = Unit

	override fun onBindRowViewHolder(holder: RowPresenter.ViewHolder, item: Any) {
		super.onBindRowViewHolder(holder, item)

		val view = holder.view?.parent as? View ?: return
		if (topPadding != null) view.setPadding(view.paddingLeft, topPadding, view.paddingRight, view.paddingBottom)

		// Hide header view when the item doesn't have one
		holder.headerViewHolder.view.isVisible = !(item is ListRow && item.headerItem == null)

		// Apply a row's preferred scroll position once its items are present (e.g. an episode's
		// season row focused on the next episode). The fragment also applies this on data-load for
		// rows already on screen; whichever runs first wins via the "applied" guard.
		applyPreferredScrollPosition(holder, item)
	}

	private fun applyPreferredScrollPosition(holder: RowPresenter.ViewHolder, item: Any) {
		val rowAdapter = (item as? ListRow)?.adapter as? ItemRowAdapter ?: return
		val position = rowAdapter.preferredScrollPosition ?: return
		if (rowAdapter.isPreferredScrollApplied || rowAdapter.size() == 0) return

		val gridView = (holder as ListRowPresenter.ViewHolder).gridView
		val clamped = position.coerceIn(0, rowAdapter.size() - 1)
		gridView.post { gridView.selectedPosition = clamped }
		rowAdapter.isPreferredScrollApplied = true
	}
}
