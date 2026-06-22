package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.FrameLayout
import androidx.core.view.isVisible
import org.jellyfin.androidtv.databinding.ViewRowDetailsBinding

class DetailRowView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	val binding = ViewRowDetailsBinding.inflate(LayoutInflater.from(context), this, true)

	/**
	 * Keeps track of the last selected button and reselect it when navigating back to the buttons row.
	 */
	private val buttonsHierarchyChangeListener = object : OnHierarchyChangeListener {

		private var lastFocusedButton: View? = null

		private val focusChangeListener = OnFocusChangeListener { view, hasFocus ->
			// Restore last focused button when navigating back to button row
			if (hasFocus && lastFocusedButton != null) {
				lastFocusedButton?.requestFocus()
				lastFocusedButton = null
			}
			view.post {
				// Store last focused button when navigating away from button row
				if (binding.fdButtonRow.focusedChild == null) {
					lastFocusedButton = view
				}
			}
		}

		override fun onChildViewAdded(parent: View?, child: View?) {
			child?.onFocusChangeListener = focusChangeListener
		}

		override fun onChildViewRemoved(parent: View?, child: View?) {
			child?.onFocusChangeListener = null
		}
	}

	init {
		binding.fdButtonRow.setOnHierarchyChangeListener(buttonsHierarchyChangeListener)
		binding.mainImage.clipToOutline = true
	}

	/**
	 * Bridge focus between the button row and the favourite heart by the title. The event passes
	 * through here on its way to the focused button, so we can claim Up/Down before leanback treats
	 * them as row navigation. Down from the buttons still falls through to the row below.
	 */
	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		val heart = binding.fdFavoriteButton
		if (event.action == KeyEvent.ACTION_DOWN && heart.isVisible) {
			when (event.keyCode) {
				KeyEvent.KEYCODE_DPAD_UP ->
					if (binding.fdButtonRow.hasFocus()) return heart.requestFocus()
				KeyEvent.KEYCODE_DPAD_DOWN ->
					if (heart.isFocused) return binding.fdButtonRow.requestFocus()
			}
		}
		return super.dispatchKeyEvent(event)
	}
}
