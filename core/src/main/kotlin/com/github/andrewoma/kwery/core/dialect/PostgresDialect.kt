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

package com.github.andrewoma.kwery.core.dialect

import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import javax.xml.bind.DatatypeConverter

open class PostgresDialect : Dialect {

    override fun bind(value: Any, limit: Int) = when (value) {
        is String -> escapeSingleQuotedString(value.truncate(limit))
        is Timestamp -> timestampFormat.get().format(value)
        is Date -> "'$value'"
        is Time -> "'$value'"
        is java.sql.Array -> bindArray(value, limit, "array[", "]")
        is ByteArray -> "decode('${DatatypeConverter.printBase64Binary(value.truncate(limit))}','base64')"
        else -> value.toString()
    }

    override fun arrayBasedIn(name: String) = "= any(:$name)"

    override val supportsArrayBasedIn = true

    override val supportsAllocateIds = true

    override fun allocateIds(count: Int, sequence: String, columnName: String) =
            "select nextval('$sequence') as $columnName from generate_series(1, $count)"

    override val supportsFetchingGeneratedKeysByName = true

    override fun escapeColumnName(columnName: String): String =
            '"' + columnName.replace("\"", "\"\"") + '"'

}
