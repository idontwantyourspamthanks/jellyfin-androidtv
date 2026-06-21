package org.jellyfin.androidtv.ui.librarymanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.library.libraryIcon
import org.koin.androidx.compose.koinViewModel

/**
 * Full-screen editor for the order and visibility of the user's libraries. Reached from the
 * "Edit libraries" item in the navigation drawer. Changes save immediately (see [LibraryManagerViewModel]).
 */
class LibraryManagerFragment : Fragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	) = content {
		val viewModel = koinViewModel<LibraryManagerViewModel>()
		val rows by viewModel.rows.collectAsState()

		LibraryManagerScreen(
			rows = rows,
			onMoveUp = viewModel::moveUp,
			onMoveDown = viewModel::moveDown,
			onToggleHidden = viewModel::toggleHidden,
		)
	}
}

@Composable
private fun LibraryManagerScreen(
	rows: List<LibraryManagerViewModel.LibraryRow>,
	onMoveUp: (Int) -> Unit,
	onMoveDown: (Int) -> Unit,
	onToggleHidden: (String) -> Unit,
) {
	val firstControlFocusRequester = remember { FocusRequester() }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(start = 80.dp, top = 52.dp, end = 80.dp, bottom = 40.dp),
	) {
		Text(
			text = stringResource(R.string.lbl_edit_libraries),
			color = Color.White,
			fontSize = 32.sp,
			fontWeight = FontWeight.Bold,
		)
		Text(
			text = stringResource(R.string.lbl_edit_libraries_caption),
			style = JellyfinTheme.typography.listCaption,
			modifier = Modifier
				.padding(top = 6.dp)
				.widthIn(max = 620.dp),
		)

		LazyColumn(
			verticalArrangement = Arrangement.spacedBy(10.dp),
			modifier = Modifier.padding(top = 28.dp),
		) {
			itemsIndexed(rows, key = { _, row -> row.item.id.toString() }) { index, row ->
				LibraryEditorRow(
					row = row,
					isFirst = index == 0,
					isLast = index == rows.lastIndex,
					onMoveUp = { onMoveUp(index) },
					onMoveDown = { onMoveDown(index) },
					onToggleHidden = { onToggleHidden(row.item.id.toString()) },
					toggleModifier = if (index == 0) Modifier.focusRequester(firstControlFocusRequester) else Modifier,
				)
			}
		}
	}

	LaunchedEffect(rows.isNotEmpty()) {
		if (rows.isNotEmpty()) runCatching { firstControlFocusRequester.requestFocus() }
	}
}

@Composable
private fun LibraryEditorRow(
	row: LibraryManagerViewModel.LibraryRow,
	isFirst: Boolean,
	isLast: Boolean,
	onMoveUp: () -> Unit,
	onMoveDown: () -> Unit,
	onToggleHidden: () -> Unit,
	toggleModifier: Modifier,
) {
	// Hidden libraries are dimmed so the list still reads as "these are off".
	val contentAlpha = if (row.hidden) 0.45f else 1f

	Row(
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(14.dp),
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(16.dp))
			.background(JellyfinTheme.colorScheme.surface)
			.padding(horizontal = 18.dp, vertical = 12.dp),
	) {
		Box(
			contentAlignment = Alignment.Center,
			modifier = Modifier
				.size(44.dp)
				.clip(CircleShape)
				.background(JellyfinTheme.colorScheme.button)
				.alpha(contentAlpha),
		) {
			Icon(
				painter = painterResource(libraryIcon(row.item.collectionType)),
				contentDescription = null,
				modifier = Modifier.size(22.dp),
			)
		}

		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = row.item.name.orEmpty(),
				fontSize = 18.sp,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.alpha(contentAlpha),
			)
			if (row.hidden) {
				Text(
					text = stringResource(R.string.lbl_hidden).uppercase(),
					style = JellyfinTheme.typography.listOverline,
					color = JellyfinTheme.colorScheme.listOverline,
				)
			}
		}

		IconButton(
			onClick = onMoveUp,
			enabled = !isFirst,
		) {
			Icon(
				painter = painterResource(R.drawable.ic_chevron_up),
				contentDescription = stringResource(R.string.lbl_move_up),
			)
		}
		IconButton(
			onClick = onMoveDown,
			enabled = !isLast,
		) {
			Icon(
				painter = painterResource(R.drawable.ic_chevron_down),
				contentDescription = stringResource(R.string.lbl_move_down),
			)
		}
		Button(
			onClick = onToggleHidden,
			modifier = toggleModifier,
		) {
			Text(stringResource(if (row.hidden) R.string.lbl_show else R.string.lbl_hide))
		}
	}
}
