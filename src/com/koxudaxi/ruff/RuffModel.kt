package com.koxudaxi.ruff

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Location(
    val row: Int,
    val column: Int
)

@Serializable
data class Fix(
    val content: String,
    val location: Location,
    @SerialName("end_location") val endLocation: Location,
    val message: String? = null
)
@Serializable
data class Result(
    val code: String,
    val message: String,
    val fix: Fix?,
    val location: Location,
    @SerialName("end_location") val endLocation: Location,
    val filename: String
)
