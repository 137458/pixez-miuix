package com.perol.pixez.shared.testutil

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** 测试统一 JSON 配置：与生产解析保持一致的宽松策略。 */
val TestJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

/** 一次被桩引擎捕获的请求事实。字段多于单个用例所需，避免各仓库测试各自再定义一份。 */
data class CapturedRequest(
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val query: Map<String, String>,
    val formParams: Map<String, String>,
    val headers: Map<String, String>,
)

/** 自定义回包：需要按 URL 路由或多响应序列时使用。 */
typealias StubHandler = suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData

/**
 * 仓库测试共用的 Ktor MockEngine 打桩夹具。
 *
 * 用法：
 * - 固定回包：构造后设置 [responseBody]/[responseStatus]，再 [client]；
 * - 按请求路由：把 handler 传给 [client]；
 * - 断言：读取 [requests]（记录全部请求事实）。
 *
 * 记录发生在 handler 之前，因此自定义回包也能拿到完整请求事实。
 */
class StubEngine(
    /** 默认回包的 Content-Type；网页抓取场景传 text/html。 */
    private val responseContentType: ContentType = ContentType.Application.Json,
    /** 是否安装 JSON ContentNegotiation；纯文本/下载场景传 false。 */
    private val installContentNegotiation: Boolean = true,
) {
    private val recorded = mutableListOf<CapturedRequest>()

    /** 已捕获的请求（按发生顺序）；暴露可变视图以便用例按需 removeAt。 */
    val requests: MutableList<CapturedRequest> get() = recorded

    /** 已捕获请求的 URL 列表，便于整体断言重试顺序。 */
    val urls: List<String> get() = recorded.map { it.url }

    /** 最后一次请求；无请求时抛 [NoSuchElementException]。 */
    val lastRequest: CapturedRequest get() = recorded.last()

    var responseBody: String = "{}"
    var responseStatus: HttpStatusCode = HttpStatusCode.OK

    fun client(handler: StubHandler? = null): HttpClient = HttpClient(MockEngine) {
        if (installContentNegotiation) {
            install(ContentNegotiation) { json(TestJson) }
        }
        engine {
            addHandler { request ->
                recorded += request.toCapturedRequest()
                handler?.invoke(this, request) ?: respond(
                    responseBody,
                    responseStatus,
                    headersOf(HttpHeaders.ContentType, responseContentType.toString()),
                )
            }
        }
    }

    fun clear() = recorded.clear()
}

/** 便捷构造：单一 JSON 回包 + 记录请求。 */
fun stubEngine(
    responseBody: String = "{}",
    responseStatus: HttpStatusCode = HttpStatusCode.OK,
    responseContentType: ContentType = ContentType.Application.Json,
    installContentNegotiation: Boolean = true,
): StubEngine = StubEngine(responseContentType, installContentNegotiation).apply {
    this.responseBody = responseBody
    this.responseStatus = responseStatus
}

/** 把一次请求展开为可断言的纯数据快照（查询参数、表单字段、请求头全部展开）。 */
fun HttpRequestData.toCapturedRequest(): CapturedRequest {
    val query = LinkedHashMap<String, String>()
    for (name in url.parameters.names()) {
        query[name] = url.parameters[name] ?: ""
    }
    val formParams = LinkedHashMap<String, String>()
    (body as? FormDataContent)?.formData?.entries()?.forEach { entry ->
        formParams[entry.key] = entry.value.joinToString(",")
    }
    val headerMap = LinkedHashMap<String, String>()
    for (name in headers.names()) {
        headerMap[name] = headers.getAll(name)?.joinToString(",") ?: ""
    }
    return CapturedRequest(
        method = method.value,
        url = url.toString(),
        host = url.host,
        path = url.encodedPath,
        query = query,
        formParams = formParams,
        headers = headerMap,
    )
}
