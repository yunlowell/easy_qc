package com.example.easyqc3.model

data class SimpleHistoryItem(
    val referenceLength: Double,
    val measuredValue: Double = 0.0,
    val measurementUnit: String = "",
    val result: String = "test",
    val userId: String = "test",
    val measurementDateTime: String = ""
)