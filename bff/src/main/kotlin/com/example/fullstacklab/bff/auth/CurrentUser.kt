package com.example.fullstacklab.bff.auth

import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

/** 検証済み JWT から組み立てた、IdP の違いを吸収した利用者情報 */
data class CurrentUser(
    val id: String,
    val name: String,
    val userType: UserType,
    val roles: Set<String>,
)

@Component
class CurrentUserFactory(private val props: AuthProperties) {

    /** SecurityConfig が検証した JWT（SecurityContext の principal）から、今のリクエストの利用者を得る */
    fun current(): CurrentUser {
        val jwt = SecurityContextHolder.getContext().authentication?.principal as? Jwt
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated")
        return from(jwt)
    }

    fun from(jwt: Jwt): CurrentUser {
        // SecurityConfig で既知の発行元しか通さないため、ここで見つからないことはない
        val issuer = props.issuers.values.first { it.issuerUri == jwt.issuer.toString() }
        val subject = requireNotNull(jwt.subject) { "sub claim is required" }
        return CurrentUser(
            id = subject,
            name = jwt.getClaimAsString(issuer.nameClaim) ?: subject,
            userType = issuer.userType,
            roles = rolesAt(jwt.claims, issuer.rolesClaim),
        )
    }

    /** "realm_access.roles"（Keycloak）や "groups"（Okta）のようなパスでロールの配列を取り出す */
    private fun rolesAt(claims: Map<String, Any>, path: String): Set<String> {
        var node: Any? = claims
        for (key in path.split('.')) {
            node = (node as? Map<*, *>)?.get(key)
        }
        return (node as? Collection<*>)?.mapNotNull { it?.toString() }?.toSet().orEmpty()
    }
}
