package com.example.fullstacklab.bff.authz

import com.example.fullstacklab.authzclient.apis.DecisionsApi
import com.example.fullstacklab.authzclient.models.DecisionRequest
import com.example.fullstacklab.authzclient.models.DecisionResponse
import com.example.fullstacklab.authzclient.models.Resource
import com.example.fullstacklab.authzclient.models.Subject
import com.example.fullstacklab.authzclient.models.UserType
import com.example.fullstacklab.bff.auth.CurrentUser
import com.example.fullstacklab.bff.common.buildInternalRestClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Duration

@ConfigurationProperties("app.authz")
data class AuthzProperties(
    val baseUrl: String,
    val connectTimeout: Duration = Duration.ofSeconds(1),
    val readTimeout: Duration = Duration.ofSeconds(2),
)

class AuthorizationUnavailableException(cause: Throwable) : RuntimeException("authorization service unavailable", cause)

@Configuration
class AuthzClientConfig {
    @Bean
    fun decisionsApi(builder: RestClient.Builder, props: AuthzProperties): DecisionsApi =
        DecisionsApi(buildInternalRestClient(builder, props.baseUrl, props.connectTimeout, props.readTimeout))
}

/**
 * BFF → 認可 API。authz リポジトリが publish した authz-client（OpenAPI から生成）を使う。
 * 判定できないときは許可せず例外にする（fail-closed）。
 */
@Component
class AuthzClient(private val decisionsApi: DecisionsApi) {

    fun decide(user: CurrentUser, action: String, ownerId: String?, resourceType: String = "task"): DecisionResponse =
        try {
            decisionsApi.decide(
                DecisionRequest(
                    subject = Subject(
                        id = user.id,
                        userType = UserType.valueOf(user.userType.name),
                        roles = user.roles.toList(),
                    ),
                    action = action,
                    resource = Resource(type = resourceType, ownerId = ownerId),
                ),
            )
        } catch (e: RestClientException) {
            throw AuthorizationUnavailableException(e)
        }
}
