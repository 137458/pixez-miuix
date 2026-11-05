package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.network.TrustedUrlPolicy
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * 列表分页接缝：由 [nextUrl] 决定取首页还是取下一页，URL 校验与请求方式集中在此。
 *
 * - [nextUrl] 为空白：调用 [firstPage] 构造首页请求（各仓库的端点、查询参数与首屏缓存语义留在调用侧）。
 * - [nextUrl] 非空：先经 [TrustedUrlPolicy.apiPaginationUrl] 校验 HTTPS、受信任 host 与无凭据，
 *   再以原样 URL 直取——服务端返回的 next_url 自带全部查询参数，不得重新拼参。
 *
 * 6 个列表仓库曾各自复制这组三分支 22 处：任一处漏写校验即绕过 URL 白名单
 * （CONTEXT §2「URL 必须经 TrustedUrlPolicy 校验」不变量），故收敛为单点。
 */
internal suspend inline fun <reified T> HttpClient.pagedRequest(
    nextUrl: String?,
    crossinline firstPage: suspend () -> T,
): T = if (nextUrl.isNullOrBlank()) firstPage() else pagedGet(nextUrl)

/**
 * 直取受信任的分页 URL：供「仅翻页」入口（nextUrl 非空且无首页语义）使用，同样强制过白名单。
 */
internal suspend inline fun <reified T> HttpClient.pagedGet(nextUrl: String): T =
    get(TrustedUrlPolicy.apiPaginationUrl(nextUrl)).body()
