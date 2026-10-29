package com.example.fullstacklab.bff.generated.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
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
 * AVAILABLE = 取得できた、NO_DUE_DATE = 期限日なし、UNAVAILABLE = 外部サービス障害で取得できなかった（縮退）
 * Values: AVAILABLE,NO_DUE_DATE,UNAVAILABLE
 */
enum class DueDateInfoStatus(@get:JsonValue val value: kotlin.String) {

    AVAILABLE("AVAILABLE"),
    NO_DUE_DATE("NO_DUE_DATE"),
    UNAVAILABLE("UNAVAILABLE");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): DueDateInfoStatus {
            return values().firstOrNull{ it.value == value }
                ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'DueDateInfoStatus'")
        }
    }
}
