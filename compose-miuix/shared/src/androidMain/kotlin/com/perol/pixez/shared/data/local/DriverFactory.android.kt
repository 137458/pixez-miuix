package com.perol.pixez.shared.data.local

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, fileName: String): SqlDriver {
        return AndroidSqliteDriver(schema, context, fileName)
    }
}
