/*
 * Copyright (c) 2015 Andrew O'Malley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.andrewoma.kwery.core

import org.junit.Test
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectUpdateTest : AbstractFilmSessionTest() {

    @Test fun `insert with generated keys should fetch key`() {
        val max = maxId("actor_id", "actor")
        val actor = Actor("Kate", "Beckinsale")

        val inserted = insert(actor)
        assertTrue(inserted.id > max)

        assertValue(actor, inserted)
    }

    @Test fun `insert with null values should be supported`() {
        val actor = Actor("Kate", null)
        assertValue(actor, insert(actor))
    }

    @Test fun `insert with strings requiring escaping should be supported`() {
        val actor = Actor("Andrew", "O'Malley")
        assertValue(actor, insert(actor))
    }

    @Test fun `insert with explicit id should be supported`() {
        val actor = Actor("Kate", "Beckinsale", 100)
        deleteActor(actor.id)
        assertValue(actor, insert(actor))
    }

    @Test fun `delete should remove existing row`() {
        val actor = insert(Actor("Kate", "Beckinsale"))
        assertEquals(1, selectActors(setOf(actor.id)).size)
        deleteActor(actor.id)
        assertEquals(0, selectActors(setOf(actor.id)).size)
    }

    @Test(expected = IllegalArgumentException::class) fun `missing select parameters should be rejected`() {
        session.select("select * from actor where actor_id = :actor_id") {}
    }

    @Test(expected = SQLException::class) fun `bad SQL should be rejected`() {
        session.select("select * from junk") {}
    }

    @Test fun `in clauses should support null values`() {
        val actor = insert(Actor("Kate", "Beckinsale"))
        val actor2 = insert(Actor("Kate", "Beckinsale"))
        assertEquals(1, selectActors(setOf(actor.id, null)).size)
        assertEquals(1, selectActors(setOf(null, actor2.id)).size)
        assertEquals(2, selectActors(setOf(null, actor2.id, null, actor.id, null)).size)
    }

    @Test fun `forEach should call back for each row`() {
        val actors = insertActors()

        val ids = actors.map { it.id }.toSet()

        val fetched = hashSetOf<Int>()

        val sql = "select actor_id from actor where actor_id in (:ids)"
        session.forEach(sql, mapOf("ids" to ids)) { row ->
            fetched.add(row.int("actor_id"))
        }

        assertEquals(ids, fetched)
    }

    @Test fun `stream should collect all rows`() {
        val actors = insertActors()

        val ids = actors.map { it.id }.toSet()

        val sql = "select actor_id from actor where actor_id in (:ids)"

        val fetched = session.asSequence(sql, mapOf("ids" to ids)) { seq ->
            seq.map { it.int("actor_id") }.toSet()
        }

        assertEquals(ids, fetched)
    }

    @Test fun `should support bound parameters with dollar sign`() {
        val actor = Actor("Bi\$lly", "\$Thorn$")
        val inserted = insert(actor)
        val found = selectActors(setOf(inserted.id)).first()
        assertEquals(found.firstName, actor.firstName)
        assertEquals(found.lastName, actor.lastName)
    }

    private fun assertValue(oldValue: Actor, newValue: Actor) {
        assertEquals(oldValue.copy(id = newValue.id), newValue)

        val fetched = selectActors(setOf(newValue.id)).first()
        assertEquals(newValue, fetched)
    }

    @Test fun `should support multiple collection parameters`() {
        val actors1 = insertActors()
        val actors2 = insertActors()
        val actors3 = insertActors()

        val sql = "select actor_id from actor where actor_id in (:ids1) or actor_id in (:ids2) or actor_id in (:ids3)"

        val params = mapOf("ids1" to actors1.ids, "ids2" to actors2.ids, "ids3" to actors3.ids)
        val fetched = session.select(sql, params) { row -> row.int("actor_id") }

        assertEquals(fetched, (actors1 + actors2 + actors3).ids)
    }

    @Test fun `should support mixture of parameters with collection last`() {
        val actors = insertActors()

        val sql = "select actor_id from actor where actor_id = :id or actor_id in (:ids)"

        val params = mapOf("id" to actors.first().id, "ids" to actors.drop(1).ids)
        val fetched = session.select(sql, params) { row -> row.int("actor_id") }

        assertEquals(fetched, actors.ids)
    }

    @Test fun `should support mixture of parameters with collection first`() {
        val actors = insertActors()

        val sql = "select actor_id from actor where actor_id in (:ids) or actor_id = :id"

        val params = mapOf("id" to actors.first().id, "ids" to actors.drop(1).ids)
        val fetched = session.select(sql, params) { row -> row.int("actor_id") }

        assertEquals(fetched, actors.ids)
    }

    private fun insertActors() = listOf(
            insert(Actor("Kate", "Beckinsale")),
            insert(Actor("Kate", "Winslet")),
            insert(Actor("Tony", "Stark"))
    )

    private val List<Actor>.ids
        get() = this.map { it.id }
}