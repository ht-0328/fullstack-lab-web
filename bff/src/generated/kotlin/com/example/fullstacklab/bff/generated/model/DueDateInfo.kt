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
 * @param date 
 * @param businessDay 
 * @param holidayName 
 */
data class DueDateInfo(

    @field:Valid
    @param:JsonProperty("date", required = true)
    @get:JsonProperty("date", required = true) val date: java.time.LocalDate,

    @param:JsonProperty("businessDay", required = true)
    @get:JsonProperty("businessDay", required = true) val businessDay: kotlin.Boolean,

    @param:JsonProperty("holidayName")
    @get:JsonProperty("holidayName") val holidayName: kotlin.String? = null
) {

}

