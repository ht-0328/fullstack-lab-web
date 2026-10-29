package com.example.fullstacklab.bff.account

import com.example.fullstacklab.authclient.apis.CustomersApi
import com.example.fullstacklab.authclient.models.ChangePasswordRequest
import com.example.fullstacklab.authclient.models.RegisterCustomerRequest
import com.example.fullstacklab.bff.auth.CurrentUser
import com.example.fullstacklab.bff.authz.AuthzClient
import com.example.fullstacklab.bff.common.buildInternalRestClient
import com.example.fullstacklab.bff.generated.model.ChangePasswordInput
import com.example.fullstacklab.bff.generated.model.SignupInput
import com.example.fullstacklab.bff.generated.model.SignupResult
import com.example.fullstacklab.bff.task.ForbiddenException
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.Duration

@ConfigurationProperties("app.auth-api")
data class AuthApiProperties(
    val baseUrl: String,
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(5),
)

/** 認証 API の呼び出しには、auth リポジトリが OpenAPI から生成して publish した auth-client を使う */
@Configuration
class AuthApiClientConfig {
    @Bean
    fun customersApi(builder: RestClient.Builder, props: AuthApiProperties): CustomersApi =
        CustomersApi(buildInternalRestClient(builder, props.baseUrl, props.connectTimeout, props.readTimeout))
}

/**
 * 一般利用者のアカウント操作（BFF → 認証 API）。
 * - 会員登録は未認証の操作なので認可 API には問い合わせない（主体が無い）
 * - パスワード変更は認可 API に account:change-password を問い合わせ、許可されたときだけ認証 API を呼ぶ。
 *   対象の利用者 ID には検証済み JWT の sub を使い、他人のアカウントを指定できないようにする
 * 認証 API の 4xx（409 重複、403 現在のパスワード不一致）は BffExceptionHandler がそのまま画面へ返す。
 */
@Service
class AccountService(
    private val customersApi: CustomersApi,
    private val authzClient: AuthzClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun signup(input: SignupInput): SignupResult {
        val account = customersApi.registerCustomer(
            RegisterCustomerRequest(
                email = input.email,
                familyName = input.familyName,
                givenName = input.givenName,
                password = input.password,
            ),
        )
        log.info("customer registered: {}", account.id)
        return SignupResult(userId = account.id, email = account.email)
    }

    /** 画面の出し分け用（パスワード変更の導線）。実際の守りは changePassword() の認可 */
    fun canChangePassword(user: CurrentUser): Boolean = decide(user).allowed

    fun changePassword(user: CurrentUser, input: ChangePasswordInput) {
        val decision = decide(user)
        if (!decision.allowed) {
            throw ForbiddenException("account:change-password denied for ${user.id}: ${decision.reason}")
        }
        customersApi.changeCustomerPassword(
            user.id,
            ChangePasswordRequest(currentPassword = input.currentPassword, newPassword = input.newPassword),
        )
        log.info("password changed: {}", user.id)
    }

    private fun decide(user: CurrentUser) =
        authzClient.decide(user, ACTION_CHANGE_PASSWORD, ownerId = user.id, resourceType = "account")

    companion object {
        const val ACTION_CHANGE_PASSWORD = "account:change-password"
    }
}
