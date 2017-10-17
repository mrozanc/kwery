package com.github.andrewoma.kwery.mappertest

import com.github.andrewoma.kwery.mapper.Column
import com.github.andrewoma.kwery.mapper.SimpleConverter
import com.github.andrewoma.kwery.mapper.Table
import com.github.andrewoma.kwery.mapper.Value
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.assertEquals

class MapMapperTest {

    @Test
    fun testMapMapping() {
        val id = UUID.randomUUID()
        val now = LocalDateTime.now()
        val something = mapOf(
                "id" to id.toString(),
                "name" to "Whatever",
                "lastUpdated" to now.format(DateTimeFormatter.ISO_DATE_TIME)!!
        )

        val mapped = someTable.mapMapper()(something)

        assertEquals(id, mapped.id)
        assertEquals("Whatever", mapped.name)
        assertEquals(now, mapped.lastUpdated)
    }

    class Something(
            val id: UUID,
            val name: String,
            val lastUpdated: LocalDateTime
    )

    object someTable : Table<Something, UUID>(" won't ever be used ") {

        val Id
                by col(Something::id, name = "id", id = true, default = DefaultUuid, converter = UuidConverter, mapConverter = UUID::fromString)

        val Name
                by col(Something::name, name = "name", mapConverter = { it })

        val LastUpdated
                by col(Something::lastUpdated, name = "lastUpdated", mapConverter = { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) })


        override fun idColumns(id: UUID): Set<Pair<Column<Something, *>, *>> =
                setOf(Id of id)

        override fun create(value: Value<Something>): Something = Something(
                value of Id,
                value of Name,
                value of LastUpdated
        )

    }

}

private val DefaultUuid = UUID(0, 0)

object UuidConverter : SimpleConverter<UUID>(
        { row, name -> row.obj(name) as UUID }
)
