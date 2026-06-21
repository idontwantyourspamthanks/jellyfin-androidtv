package org.jellyfin.androidtv.ui.library

import androidx.annotation.DrawableRes
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.CollectionType

/**
 * Drawable that best represents a library's [CollectionType], so films, TV, music and the rest are
 * each recognisable at a glance. Shared by the navigation drawer and the library editor.
 */
@DrawableRes
fun libraryIcon(collectionType: CollectionType?): Int = when (collectionType) {
	CollectionType.MOVIES, CollectionType.BOXSETS -> R.drawable.ic_movie
	CollectionType.TVSHOWS -> R.drawable.ic_tv
	CollectionType.MUSIC, CollectionType.PLAYLISTS -> R.drawable.ic_music_album
	CollectionType.LIVETV -> R.drawable.ic_tv_guide
	CollectionType.PHOTOS -> R.drawable.ic_photo
	CollectionType.BOOKS -> R.drawable.ic_folder
	else -> R.drawable.ic_grid
}
