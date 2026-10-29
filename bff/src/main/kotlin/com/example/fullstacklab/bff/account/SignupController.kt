package com.example.fullstacklab.bff.account

import com.example.fullstacklab.bff.generated.api.SignupApi
import com.example.fullstacklab.bff.generated.model.SignupInput
import com.example.fullstacklab.bff.generated.model.SignupResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * 会員登録。画面との契約（openapi/bff.yaml）から生成した SignupApi を実装する。
 * BFF で唯一、未認証で呼べるエンドポイント（SecurityConfig で permitAll。入口の Nginx / ALB も認証なしで転送する）。
 */
@RestController
class SignupController(private val accountService: AccountService) : SignupApi {

    override fun signup(signupInput: SignupInput): ResponseEntity<SignupResult> =
        ResponseEntity.status(HttpStatus.CREATED).body(accountService.signup(signupInput))
}
