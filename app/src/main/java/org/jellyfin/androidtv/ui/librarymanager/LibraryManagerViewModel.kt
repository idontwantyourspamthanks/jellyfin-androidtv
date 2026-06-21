package org.jellyfin.androidtv.ui.librarymanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.LibraryDisplayPreferences
import org.jellyfin.androidtv.data.repository.LibraryDisplayState
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Backs the library editor: lists every library (including hidden ones) in the user's order and lets
 * them reorder and show/hide each. Every change is persisted to [UserSettingPreferences.libraryDisplay]
 * straight away, so there is no explicit save step.
 */
class LibraryManagerViewModel(
	private val userViewsRepository: UserViewsRepository,
	private val userSettingPreferences: UserSettingPreferences,
) : ViewModel() {
	data class LibraryRow(val item: BaseItemDto, val hidden: Boolean)

	private var viewsById: Map<String, BaseItemDto> = emptyMap()
	private var order: List<String> = emptyList()
	private var hidden: Set<String> = emptySet()

	private val _rows = MutableStateFlow<List<LibraryRow>>(emptyList())
	val rows = _rows.asStateFlow()

	init {
		viewModelScope.launch {
			val views = userViewsRepository.views.first().toList()
			viewsById = views.associateBy { it.id.toString() }
			val saved = LibraryDisplayPreferences.parse(userSettingPreferences[UserSettingPreferences.libraryDisplay])
			// Normalise so the editor shows all current libraries (new ones included) in order.
			order = LibraryDisplayPreferences.order(views, saved).map { it.id.toString() }
			hidden = saved.hidden.intersect(viewsById.keys)
			rebuild()
		}
	}

	fun moveUp(index: Int) {
		if (index <= 0 || index >= order.size) return
		order = order.toMutableList().apply { add(index - 1, removeAt(index)) }
		persistAndRebuild()
	}

	fun moveDown(index: Int) {
		if (index < 0 || index >= order.size - 1) return
		order = order.toMutableList().apply { add(index + 1, removeAt(index)) }
		persistAndRebuild()
	}

	fun toggleHidden(id: String) {
		hidden = if (id in hidden) hidden - id else hidden + id
		persistAndRebuild()
	}

	private fun persistAndRebuild() {
		val state = LibraryDisplayState(order = order, hidden = hidden)
		userSettingPreferences[UserSettingPreferences.libraryDisplay] = LibraryDisplayPreferences.encode(state)
		viewModelScope.launch { userSettingPreferences.commit() }
		rebuild()
	}

	private fun rebuild() {
		_rows.value = order.mapNotNull { id ->
			viewsById[id]?.let { LibraryRow(it, id in hidden) }
		}
	}
}
