package com.example.fullstacklab.bff.account

import com.example.fullstacklab.authclient.apis.CustomersApi
import com.example.fullstacklab.authzclient.apis.DecisionsApi
import com.example.fullstacklab.bff.auth.CurrentUser
import com.example.fullstacklab.bff.auth.UserType
import com.example.fullstacklab.bff.authz.AuthzClient
import com.example.fullstacklab.bff.generated.model.ChangePasswordInput
import com.example.fullstacklab.bff.generated.model.SignupInput
import com.example.fullstacklab.bff.task.ForbiddenException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.never
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * BFF → 認可 API / 認証 API の通信を確認する。
 * 呼び出しには publish された生成クライアント（auth-client / authz-client）を使い、HTTP 層を MockRestServiceServer で差し替える。
 */
class AccountServiceTest {
    private lateinit var auth: MockRestServiceServer
    private lateinit var authz: MockRestServiceServer
    private lateinit var service: AccountService

    private val alice = CurrentUser("alice-id", "alice", UserType.CUSTOMER, setOf("customer"))
    private val dave = CurrentUser("dave-id", "dave", UserType.STAFF, setOf("task-admin"))

    @BeforeEach
    fun setUp() {
        val authBuilder = RestClient.builder().baseUrl("http://auth.test")
        val authzBuilder = RestClient.builder().baseUrl("http://authz.test")
        auth = MockRestServiceServer.bindTo(authBuilder).build()
        authz = MockRestServiceServer.bindTo(authzBuilder).build()
        service = AccountService(CustomersApi(authBuilder.build()), AuthzClient(DecisionsApi(authzBuilder.build())))
    }

    private fun expectDecision(allowed: Boolean, ownerId: String) {
        authz.expect(requestTo("http://authz.test/authz/v1/decisions"))
            .andExpect(jsonPath("$.action").value("account:change-password"))
            .andExpect(jsonPath("$.resource.type").value("account"))
            .andExpect(jsonPath("$.resource.ownerId").value(ownerId))
            .andRespond(withSuccess("""{"allowed":$allowed,"reason":"test"}""", MediaType.APPLICATION_JSON))
    }

    @Test
    fun `signup registers the customer through the auth api without consulting authz`() {
        auth.expect(requestTo("http://auth.test/auth/v1/customers"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.email").value("taro@example.com"))
            .andExpect(jsonPath("$.familyName").value("山田"))
            .andExpect(jsonPath("$.givenName").value("太郎"))
            .andExpect(jsonPath("$.password").value("password-1"))
            .andRespond(
                withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                    .body("""{"id":"new-id","email":"taro@example.com","familyName":"山田","givenName":"太郎"}"""),
            )
        authz.expect(never(), requestTo("http://authz.test/authz/v1/decisions"))

        val result = service.signup(SignupInput("taro@example.com", "山田", "太郎", "password-1"))

        assertEquals("new-id", result.userId)
        assertEquals("taro@example.com", result.email)
        auth.verify()
        authz.verify()
    }

    @Test
    fun `duplicate email surfaces as the auth api's 409`() {
        auth.expect(requestTo("http://auth.test/auth/v1/customers"))
            .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body("""{"status":409}"""))

        assertFailsWith<HttpClientErrorException.Conflict> {
            service.signup(SignupInput("alice@example.com", "山田", "太郎", "password-1"))
        }
    }

    @Test
    fun `staff cannot change password here and the auth api is not called`() {
        expectDecision(allowed = false, ownerId = "dave-id")
        auth.expect(never(), requestTo("http://auth.test/auth/v1/customers/dave-id/password"))

        assertFailsWith<ForbiddenException> { service.changePassword(dave, ChangePasswordInput("old", "password-2")) }
        auth.verify()
    }

    @Test
    fun `customer changes own password using the jwt subject as the account id`() {
        expectDecision(allowed = true, ownerId = "alice-id")
        auth.expect(requestTo("http://auth.test/auth/v1/customers/alice-id/password"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonPath("$.currentPassword").value("password-1"))
            .andExpect(jsonPath("$.newPassword").value("password-2"))
            .andRespond(withNoContent())

        service.changePassword(alice, ChangePasswordInput("password-1", "password-2"))
        auth.verify()
        authz.verify()
    }

    @Test
    fun `capability follows the authz decision`() {
        expectDecision(allowed = true, ownerId = "alice-id")
        assertEquals(true, service.canChangePassword(alice))
    }
}
