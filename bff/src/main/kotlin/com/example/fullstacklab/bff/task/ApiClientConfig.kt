package com.example.fullstacklab.bff.task

import com.example.fullstacklab.apiclient.apis.TasksApi
import com.example.fullstacklab.bff.common.buildInternalRestClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Duration

@ConfigurationProperties("app.api")
data class ApiProperties(
    val baseUrl: String,
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(5),
)

/**
 * API の呼び出しには、api リポジトリが OpenAPI から生成して publish した api-client を使う。
 * BFF 側で組み立てるのは RestClient（接続先・タイムアウト・リクエスト ID の引き継ぎ）だけ。
 */
@Configuration
class ApiClientConfig {
    @Bean
    fun tasksApi(builder: RestClient.Builder, props: ApiProperties): TasksApi =
        TasksApi(buildInternalRestClient(builder, props.baseUrl, props.connectTimeout, props.readTimeout))
}
