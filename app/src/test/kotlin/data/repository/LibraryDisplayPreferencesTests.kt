package org.jellyfin.androidtv.data.repository

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

private fun view(id: UUID) = mockk<BaseItemDto> {
	every { this@mockk.id } returns id
}

class LibraryDisplayPreferencesTests : FunSpec({
	val a = UUID.randomUUID()
	val b = UUID.randomUUID()
	val c = UUID.randomUUID()
	val views = listOf(view(a), view(b), view(c))
	fun ids(items: List<BaseItemDto>) = items.map { it.id }

	test("order() with an empty state keeps the server order") {
		ids(LibraryDisplayPreferences.order(views, LibraryDisplayState())) shouldBe listOf(a, b, c)
	}

	test("order() applies the saved order") {
		val state = LibraryDisplayState(order = listOf(c.toString(), a.toString(), b.toString()))
		ids(LibraryDisplayPreferences.order(views, state)) shouldBe listOf(c, a, b)
	}

	test("order() appends libraries missing from the saved order, in server order") {
		// Only b is ordered; a and c are "new" and must follow, keeping their server order.
		val state = LibraryDisplayState(order = listOf(b.toString()))
		ids(LibraryDisplayPreferences.order(views, state)) shouldBe listOf(b, a, c)
	}

	test("order() ignores saved ids that no longer exist") {
		val gone = UUID.randomUUID().toString()
		val state = LibraryDisplayState(order = listOf(gone, b.toString()))
		ids(LibraryDisplayPreferences.order(views, state)) shouldBe listOf(b, a, c)
	}

	test("applyVisible() removes hidden libraries") {
		val state = LibraryDisplayState(hidden = setOf(b.toString()))
		ids(LibraryDisplayPreferences.applyVisible(views, state)) shouldBe listOf(a, c)
	}

	test("applyVisible() combines order and hidden") {
		val state = LibraryDisplayState(
			order = listOf(c.toString(), b.toString(), a.toString()),
			hidden = setOf(c.toString()),
		)
		ids(LibraryDisplayPreferences.applyVisible(views, state)) shouldBe listOf(b, a)
	}

	test("parse() of a blank string is the empty state") {
		LibraryDisplayPreferences.parse("") shouldBe LibraryDisplayState()
	}

	test("parse() of malformed JSON falls back to the empty state") {
		LibraryDisplayPreferences.parse("not json {{{") shouldBe LibraryDisplayState()
	}

	test("encode() then parse() round-trips") {
		val state = LibraryDisplayState(
			order = listOf(a.toString(), b.toString()),
			hidden = setOf(c.toString()),
		)
		LibraryDisplayPreferences.parse(LibraryDisplayPreferences.encode(state)) shouldBe state
	}
})
