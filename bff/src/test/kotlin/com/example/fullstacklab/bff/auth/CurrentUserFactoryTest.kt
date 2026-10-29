package com.example.fullstacklab.bff.auth

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.assertEquals

/** IdP ごとにロールのクレームの場所が違っても、同じ形の CurrentUser になることを確認する */
class CurrentUserFactoryTest {
    private val factory = CurrentUserFactory(
        AuthProperties(
            issuers = mapOf(
                "customer" to IssuerProperties(
                    issuerUri = "http://idp.test/realms/customer", jwkSetUri = "unused", audience = "c",
                    userType = UserType.CUSTOMER, rolesClaim = "realm_access.roles",
                ),
                "staff" to IssuerProperties(
                    issuerUri = "http://idp.test/realms/staff", jwkSetUri = "unused", audience = "s",
                    userType = UserType.STAFF, rolesClaim = "groups",
                ),
            ),
        ),
    )

    private fun jwt(issuer: String, claims: Map<String, Any>) =
        Jwt.withTokenValue("token").header("alg", "RS256")
            .issuer(issuer).subject("user-1").claims { it.putAll(claims) }.build()

    @Test
    fun `reads keycloak realm roles`() {
        val user = factory.from(
            jwt(
                "http://idp.test/realms/customer",
                mapOf("preferred_username" to "alice", "realm_access" to mapOf("roles" to listOf("customer"))),
            ),
        )
        assertEquals(CurrentUser("user-1", "alice", UserType.CUSTOMER, setOf("customer")), user)
    }

    @Test
    fun `reads okta style groups claim`() {
        val user = factory.from(
            jwt("http://idp.test/realms/staff", mapOf("preferred_username" to "carol", "groups" to listOf("task-viewer"))),
        )
        assertEquals(CurrentUser("user-1", "carol", UserType.STAFF, setOf("task-viewer")), user)
    }
}
