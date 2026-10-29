package com.example.fullstacklab.bff.auth

import org.springframework.boot.context.properties.ConfigurationProperties

enum class UserType { CUSTOMER, STAFF }

/**
 * 受け付ける JWT の発行元（IdP）ごとの設定。
 * ローカル: customer = Keycloak の realm、staff = Okta の代わりの Keycloak の realm
 * AWS:      customer = Keycloak、staff = Okta
 */
@ConfigurationProperties("app.auth")
data class AuthProperties(
    /** JWT を受け取るヘッダー。ALB が IdP のアクセストークンを入れるヘッダー名に合わせている */
    val tokenHeader: String = "x-amzn-oidc-accesstoken",
    val issuers: Map<String, IssuerProperties> = emptyMap(),
)

data class IssuerProperties(
    /** トークンの iss と一致すべき値（ブラウザから見た IdP の URL） */
    val issuerUri: String,
    /** 公開鍵の取得先（コンテナから見た IdP の URL。ローカルでは issuerUri とホストが異なる） */
    val jwkSetUri: String,
    /** aud に含まれるべき値。別のアプリ向けに発行されたトークンを拒否するため */
    val audience: String,
    val userType: UserType,
    /** ロールが入っているクレーム（ドット区切りで入れ子を指定）。IdP によって場所が違う */
    val rolesClaim: String,
    val nameClaim: String = "preferred_username",
)
