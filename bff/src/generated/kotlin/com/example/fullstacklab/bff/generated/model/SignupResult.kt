package com.example.fullstacklab.bff.generated.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import jakarta.validation.Valid

/**
 * 
 * @param userId ログイン後の JWT の sub と同じ
 * @param email 
 */
data class SignupResult(

    @param:JsonProperty("userId", required = true)
    @get:JsonProperty("userId", required = true) val userId: kotlin.String,

    @param:JsonProperty("email", required = true)
    @get:JsonProperty("email", required = true) val email: kotlin.String
) {

}

