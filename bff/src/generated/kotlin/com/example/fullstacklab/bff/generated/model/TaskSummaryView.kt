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
 * 一覧用。画面に不要な項目（説明文など）は含めない
 * @param id 
 * @param title 
 * @param createdByName 
 * @param dueDate 
 */
data class TaskSummaryView(

    @param:JsonProperty("id", required = true)
    @get:JsonProperty("id", required = true) val id: kotlin.Long,

    @param:JsonProperty("title", required = true)
    @get:JsonProperty("title", required = true) val title: kotlin.String,

    @param:JsonProperty("createdByName", required = true)
    @get:JsonProperty("createdByName", required = true) val createdByName: kotlin.String,

    @field:Valid
    @param:JsonProperty("dueDate")
    @get:JsonProperty("dueDate") val dueDate: java.time.LocalDate? = null
) {

}

