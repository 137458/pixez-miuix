package com.perol.pixez.shared.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * 平台相关的 SQLDelight 驱动工厂。
 * 每个旧 .db 文件通过 [fileName] 单独打开，保持与旧 Flutter 应用文件路径兼容。
 */
expect class DriverFactory {
    fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, fileName: String): SqlDriver

    /**
     * 关闭由 [createDriver] 创建的 [SqlDriver]，释放底层文件句柄与连接。
     * Repository / DatabaseHolder 层应在合适的生命周期点统一调用。
     */
    fun closeDriver(driver: SqlDriver)
}
