package com.perol.pixez.shared.data.repository

/**
 * 本地历史类表的统一查询上限。
 *
 * 浏览历史、小说历史与下载历史都按条数把整表物化到内存再交给 UI；三处各写一份字面量时，
 * 上限会随局部改动悄悄漂移，导出、列表与设置页的口径随之不一致。调整上限只改这一处。
 */
internal const val HISTORY_QUERY_LIMIT = 1000L
