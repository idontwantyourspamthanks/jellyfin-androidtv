package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.browsing.BrowsingUtils
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Where a series' season list comes from. Split out from [SeasonRepository] so the cache policy can
 * be tested against a fake without touching the network.
 */
fun interface SeasonDataSource {
	suspend fun getSeasons(seriesId: UUID): List<BaseItemDto>
}

/**
 * Loads season lists from the Jellyfin server.
 */
class ApiSeasonDataSource(
	private val apiClient: ApiClient,
) : SeasonDataSource {
	override suspend fun getSeasons(seriesId: UUID): List<BaseItemDto> = withContext(Dispatchers.IO) {
		apiClient.tvShowsApi.getSeasons(BrowsingUtils.createSeasonsRequest(seriesId)).content
	}.items
}

/**
 * In-memory, session-lifetime cache of each series' season list. A show's seasons are small and
 * rarely change during a session, yet the season popup is opened repeatedly as the user navigates
 * around the show, so caching removes a network round-trip from every open after the first.
 *
 * Lives as long as the process; there is no eviction. The season list per show is tiny, so the
 * footprint is negligible and stale entries are corrected by [fetchSeasons] revalidating in place.
 */
interface SeasonRepository {
	/** Seasons cached for [seriesId] this session, or `null` if none have been loaded yet. */
	fun getCachedSeasons(seriesId: UUID): List<BaseItemDto>?

	/**
	 * Fetch the season list from the server and refresh the cache. Returns the seasons, or `null`
	 * if the request failed (the previous cache entry, if any, is left untouched).
	 */
	suspend fun fetchSeasons(seriesId: UUID): List<BaseItemDto>?
}

class SeasonRepositoryImpl(
	private val dataSource: SeasonDataSource,
) : SeasonRepository {
	private val cache = ConcurrentHashMap<UUID, List<BaseItemDto>>()

	override fun getCachedSeasons(seriesId: UUID): List<BaseItemDto>? = cache[seriesId]

	override suspend fun fetchSeasons(seriesId: UUID): List<BaseItemDto>? = try {
		dataSource.getSeasons(seriesId).also { cache[seriesId] = it }
	} catch (error: ApiClientException) {
		Timber.w(error, "Failed to load seasons for series $seriesId")
		null
	}
}
