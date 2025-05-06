package com.example.easyqc3.model

data class HistoryItem(
    val referenceLength: Double,
    val tolerance: Double,
    val measurementUnit: String = "",
    val measuredValue: Double = 0.0,
    val result: String = "test",
    val userId: String = "test",
    val measurementDateTime: String = "",
    //val imageThumbnail: String = "test"
)

data class MeasurementSetting(
    val referenceLength: Double,
    val tolerance: Double,
    val unit: String
)
