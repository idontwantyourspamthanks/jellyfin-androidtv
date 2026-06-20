package org.jellyfin.androidtv.ui.browsing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class BrowsingUtilsTests : FunSpec({
	test("createSeasonEpisodesRequest() requests only episodes for the given season") {
		val seasonId = UUID.randomUUID()

		val request = BrowsingUtils.createSeasonEpisodesRequest(seasonId)

		request.parentId shouldBe seasonId
		request.includeItemTypes shouldBe setOf(BaseItemKind.EPISODE)
		request.fields shouldBe ItemRepository.itemFields
	}
})
