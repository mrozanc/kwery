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

package com.github.andrewoma.kwery.mappertest

import com.github.andrewoma.kwery.core.DefaultSession
import com.github.andrewoma.kwery.core.ManualTransaction
import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.core.dialect.Dialect
import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import com.zaxxer.hikari.HikariDataSource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import kotlin.properties.Delegates

val testDataSource = HikariDataSource().apply {
    driverClassName = "org.hsqldb.jdbc.JDBCDriver"
    jdbcUrl = "jdbc:hsqldb:mem:kwerydao"
}

abstract class AbstractSessionTest(val dataSource: javax.sql.DataSource = testDataSource, val dialect: Dialect = HsqlDialect()) {
    companion object {
        val initialised = hashMapOf<String, Any?>()
    }

    var transaction: ManualTransaction by Delegates.notNull()
    var session: Session by Delegates.notNull()

    open var startTransactionByDefault: Boolean = true
    open var rollbackTransactionByDefault: Boolean = false

    val name = TestName()
    @Rule fun name(): TestName = name // Annotating val directly doesn't work

    @Before fun setUp() {
        session = DefaultSession(dataSource.connection, dialect, LoggingInterceptor())
        if (startTransactionByDefault) {
            transaction = session.manualTransaction()
        }

        afterSessionSetup()
        println("==== Starting '${name.methodName}'")
    }

    open fun afterSessionSetup() {
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    fun <R> initialise(token: String, f: (Session) -> R): R {
        return initialised.getOrPut(token) { f(session) } as R
    }

    @After fun tearDown() {
        try {
            if (startTransactionByDefault) {
                if (rollbackTransactionByDefault || transaction.rollbackOnly) {
                    transaction.rollback()
                } else {
                    transaction.commit()
                }
            }
        } finally {
            session.connection.close()
        }
    }
}
