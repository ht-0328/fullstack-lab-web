package com.example.fullstacklab.bff.common

import org.slf4j.MDC
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

/** 内部サービス（API / 認可 API）呼び出し用の RestClient。タイムアウトとリクエスト ID の引き継ぎを共通化する */
fun buildInternalRestClient(
    builder: RestClient.Builder,
    baseUrl: String,
    connectTimeout: Duration,
    readTimeout: Duration,
): RestClient {
    val requestFactory = SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(connectTimeout)
        setReadTimeout(readTimeout)
    }
    return builder
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .requestInterceptor { request, body, execution ->
            MDC.get(RequestIdFilter.MDC_KEY)?.let { request.headers.set(RequestIdFilter.HEADER, it) }
            execution.execute(request, body)
        }
        .build()
}
