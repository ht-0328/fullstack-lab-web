package com.example.fullstacklab.bff.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.web.SecurityFilterChain

/**
 * BFF は JWT を検証するリソースサーバーとして動く。
 * ログイン画面やセッション Cookie は前段（AWS: ALB、ローカル: oauth2-proxy）が担当し、
 * BFF は前段が付与したアクセストークンの署名・発行元・宛先・有効期限を自分で確かめる。
 */
@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity, props: AuthProperties): SecurityFilterChain {
        // iss の値を見て、その発行元用の検証器を選ぶ（未知の発行元は拒否）
        val managers = props.issuers.values.associate { it.issuerUri to authenticationManager(it) }
        val issuerResolver = JwtIssuerAuthenticationManagerResolver(
            AuthenticationManagerResolver<String> { issuer ->
                managers[issuer] ?: throw InvalidBearerTokenException("untrusted issuer: $issuer")
            },
        )

        http {
            // BFF はセッションも Cookie も扱わないため CSRF トークンは使わない。
            // ブラウザの Cookie を持つのは前段のプロキシで、その Cookie は SameSite=Lax、
            // かつ BFF は JSON しか受け付けないので、別サイトからのフォーム送信は成立しない。
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/actuator/health/**", permitAll)
                // 会員登録だけは未認証で受け付ける（入口の Nginx / ALB も認証なしで転送する）。それ以外は JWT が必須
                authorize(HttpMethod.POST, "/bff/signup", permitAll)
                authorize(anyRequest, authenticated)
            }
            oauth2ResourceServer {
                authenticationManagerResolver = issuerResolver
                bearerTokenResolver = BearerTokenResolver { request -> request.getHeader(props.tokenHeader) }
            }
        }
        return http.build()
    }

    private fun authenticationManager(issuer: IssuerProperties): AuthenticationManager {
        val decoder = NimbusJwtDecoder.withJwkSetUri(issuer.jwkSetUri).build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(issuer.issuerUri),
                audienceValidator(issuer.audience),
            ),
        )
        return ProviderManager(JwtAuthenticationProvider(decoder))
    }

    private fun audienceValidator(audience: String) = OAuth2TokenValidator<Jwt> { jwt ->
        if (audience in jwt.audience.orEmpty()) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "audience must contain $audience", null))
        }
    }
}
