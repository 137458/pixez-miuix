package com.perol.pixez.shared.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 本地库目录登记表不变量：库名、schema 与旧库补列清单必须同源，
 * 否则「实际打开的库文件名」与「补列清单」会脱节（小组件库历史缺陷）。
 */
class LocalDatabasesTest {

    @Test
    fun `每个候选库名都解析回同一条登记项`() {
        LocalDatabases.all.forEach { database ->
            database.candidateFileNames.forEach { name ->
                assertSame(
                    database,
                    LocalDatabases.byFileName(name),
                    "库名 $name 应解析到登记项 ${database.fileName}",
                )
            }
        }
    }

    @Test
    fun `未登记的文件名不做迁移`() {
        assertEquals(null, LocalDatabases.byFileName("unknown.db"))
    }

    @Test
    fun `不同登记项的库名不重复`() {
        val names = LocalDatabases.all.flatMap { it.candidateFileNames }
        assertEquals(names.size, names.toSet().size, "库名重复会让 byFileName 静默覆盖: $names")
    }

    @Test
    fun `小组件库主名与旧 Flutter 库同名且兼容过渡别名`() {
        val glance = LocalDatabases.glanceIllustPersist
        assertEquals("glanceillustpersist.db", glance.fileName)
        assertTrue("glance_illust_persist.db" in glance.candidateFileNames, "仍需读取过渡版本写出的库文件")
        assertEquals(
            listOf("original_url", "large_url"),
            glance.legacyRequiredColumns.getValue("glanceillustpersist"),
            "旧库补列清单必须挂在实开库名下",
        )
    }

    @Test
    fun `旧库补列清单覆盖全部历史缺列`() {
        assertEquals(listOf("medium"), LocalDatabases.task.legacyRequiredColumns.getValue("task"))
        assertEquals(
            listOf("title", "user_name"),
            LocalDatabases.illustPersist.legacyRequiredColumns.getValue("illustpersist"),
        )
    }

    @Test
    fun `登记表主名不为空且 schema 与库名一一对应`() {
        val schemas = LocalDatabases.all.map { it.schema }
        assertEquals(schemas.size, schemas.toSet().size, "同一 schema 不应登记成两个库")
        LocalDatabases.all.forEach { assertTrue(it.fileName.isNotBlank()) }
    }
}
