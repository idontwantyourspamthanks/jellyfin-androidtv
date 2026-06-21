package org.jellyfin.androidtv.data.repository

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

/**
 * Records each call and returns whatever it is told to, so the cache policy can be exercised
 * without a real server. A queue of responses lets a test simulate the list changing between fetches.
 */
private class FakeSeasonDataSource : SeasonDataSource {
	var callCount = 0
	var failure: Throwable? = null
	val responses = ArrayDeque<List<BaseItemDto>>()

	override suspend fun getSeasons(seriesId: UUID): List<BaseItemDto> {
		callCount++
		failure?.let { throw it }
		return responses.removeFirstOrNull() ?: emptyList()
	}
}

private fun season() = mockk<BaseItemDto>()

class SeasonRepositoryTests : FunSpec({
	test("getCachedSeasons() returns null before anything is fetched") {
		val repository = SeasonRepositoryImpl(FakeSeasonDataSource())

		repository.getCachedSeasons(UUID.randomUUID()) shouldBe null
	}

	test("fetchSeasons() returns the fetched seasons and populates the cache") {
		val seriesId = UUID.randomUUID()
		val seasons = listOf(season(), season())
		val dataSource = FakeSeasonDataSource().apply { responses.add(seasons) }
		val repository = SeasonRepositoryImpl(dataSource)

		repository.fetchSeasons(seriesId) shouldBe seasons
		repository.getCachedSeasons(seriesId) shouldBe seasons
	}

	test("a cache hit serves the stored list without hitting the data source again") {
		val seriesId = UUID.randomUUID()
		val dataSource = FakeSeasonDataSource().apply { responses.add(listOf(season())) }
		val repository = SeasonRepositoryImpl(dataSource)

		repository.fetchSeasons(seriesId)
		repository.getCachedSeasons(seriesId)
		repository.getCachedSeasons(seriesId)

		dataSource.callCount shouldBe 1
	}

	test("fetchSeasons() revalidates the cache in place with newer data") {
		val seriesId = UUID.randomUUID()
		val original = listOf(season())
		val updated = listOf(season(), season())
		val dataSource = FakeSeasonDataSource().apply {
			responses.add(original)
			responses.add(updated)
		}
		val repository = SeasonRepositoryImpl(dataSource)

		repository.fetchSeasons(seriesId)
		repository.fetchSeasons(seriesId)

		repository.getCachedSeasons(seriesId) shouldBe updated
	}

	test("fetchSeasons() returns null and keeps the previous cache when the request fails") {
		val seriesId = UUID.randomUUID()
		val cached = listOf(season())
		val dataSource = FakeSeasonDataSource().apply { responses.add(cached) }
		val repository = SeasonRepositoryImpl(dataSource)
		repository.fetchSeasons(seriesId)

		dataSource.failure = ApiClientException("network down")

		repository.fetchSeasons(seriesId) shouldBe null
		repository.getCachedSeasons(seriesId) shouldBe cached
	}

	test("the cache is keyed per series") {
		val peppa = UUID.randomUUID()
		val bluey = UUID.randomUUID()
		val peppaSeasons = listOf(season())
		val blueySeasons = listOf(season(), season())
		val dataSource = FakeSeasonDataSource().apply {
			responses.add(peppaSeasons)
			responses.add(blueySeasons)
		}
		val repository = SeasonRepositoryImpl(dataSource)

		repository.fetchSeasons(peppa)
		repository.fetchSeasons(bluey)

		repository.getCachedSeasons(peppa) shouldBe peppaSeasons
		repository.getCachedSeasons(bluey) shouldBe blueySeasons
	}
})
