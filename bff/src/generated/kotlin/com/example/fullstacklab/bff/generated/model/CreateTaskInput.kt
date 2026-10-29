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
 * @param title 
 * @param description 
 * @param dueDate 
 */
data class CreateTaskInput(

    @get:Size(min=1,max=100)
    @param:JsonProperty("title", required = true)
    @get:JsonProperty("title", required = true) val title: kotlin.String,

    @get:Size(max=1000)
    @param:JsonProperty("description")
    @get:JsonProperty("description") val description: kotlin.String? = null,

    @field:Valid
    @param:JsonProperty("dueDate")
    @get:JsonProperty("dueDate") val dueDate: java.time.LocalDate? = null
) {

}

