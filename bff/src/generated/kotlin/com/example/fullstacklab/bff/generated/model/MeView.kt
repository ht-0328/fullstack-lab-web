package com.example.fullstacklab.bff.generated.model

import java.util.Objects
import com.example.fullstacklab.bff.generated.model.Capabilities
import com.example.fullstacklab.bff.generated.model.UserType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
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
 * @param userId 
 * @param name 
 * @param userType 
 * @param roles 
 * @param capabilities 
 */
data class MeView(

    @param:JsonProperty("userId", required = true)
    @get:JsonProperty("userId", required = true) val userId: kotlin.String,

    @param:JsonProperty("name", required = true)
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @field:Valid
    @param:JsonProperty("userType", required = true)
    @get:JsonProperty("userType", required = true) val userType: UserType,

    @param:JsonProperty("roles", required = true)
    @get:JsonProperty("roles", required = true) val roles: kotlin.collections.List<kotlin.String>,

    @field:Valid
    @param:JsonProperty("capabilities", required = true)
    @get:JsonProperty("capabilities", required = true) val capabilities: Capabilities
) {

}

