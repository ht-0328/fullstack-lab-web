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
 * 画面の出し分け用。実際の守りは BFF と認可 API が担う
 * @param canCreateTask 
 * @param canChangePassword 
 */
data class Capabilities(

    @param:JsonProperty("canCreateTask", required = true)
    @get:JsonProperty("canCreateTask", required = true) val canCreateTask: kotlin.Boolean,

    @param:JsonProperty("canChangePassword", required = true)
    @get:JsonProperty("canChangePassword", required = true) val canChangePassword: kotlin.Boolean
) {

}

