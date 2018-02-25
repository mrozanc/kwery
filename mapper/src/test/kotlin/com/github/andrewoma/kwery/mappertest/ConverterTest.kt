package com.github.andrewoma.kwery.mappertest

import com.github.andrewoma.kwery.core.Row
import com.github.andrewoma.kwery.mapper.Column
import com.github.andrewoma.kwery.mapper.Table
import com.github.andrewoma.kwery.mapper.Value
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.ResultSet
import java.util.*
import kotlin.reflect.KType
import kotlin.test.assertEquals


class ConverterTest {

    private val ih = InvocationHandler { _, _, _ -> error("unused") }
    private val enumSetProp: Set<Thread.State> = EnumSet.noneOf(Thread.State::class.java)

    @Test
    fun enumSetConverterTest() {
        val converter = TestTable.testConverter<Set<Thread.State>>(this::enumSetProp.returnType)
        val connection = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Connection::class.java), ih) as Connection

        val empty = converter.to(connection, EnumSet.noneOf(Thread.State::class.java))
        assertEquals("", empty)
        assertEquals(emptySet(), converter.from(Row(StringResultSet("")), ""))

        val single = converter.to(connection, EnumSet.of(Thread.State.RUNNABLE))
        assertEquals("RUNNABLE", single)
        assertEquals(setOf(Thread.State.RUNNABLE), converter.from(Row(StringResultSet("RUNNABLE")), ""))

        val several = converter.to(connection, EnumSet.of(Thread.State.RUNNABLE, Thread.State.WAITING))
        assertEquals("RUNNABLE|WAITING", several)
        assertEquals(setOf(Thread.State.RUNNABLE, Thread.State.WAITING), converter.from(Row(StringResultSet("RUNNABLE|WAITING")), ""))
    }

    private object TestTable : Table<Any, Nothing?>("unused") {
        override fun idColumns(id: Nothing?): Set<Pair<Column<Any, *>, *>> = error("unused")
        override fun create(value: Value<Any>): Any = error("unused")
        fun <T> testConverter(type: KType) = converter<T>(type)
    }

    private inner class StringResultSet(private val value: String) :
            ResultSet by Proxy.newProxyInstance(StringResultSet::class.java.classLoader, arrayOf(ResultSet::class.java), ih) as ResultSet {
        override fun getString(columnLabel: String?): String = value
    }

}
