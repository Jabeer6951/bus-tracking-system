package com.college.bustracker.model

data class BusLog(
    val busId: String = "",
    val driverId: String = "",
    val date: String = "",
    val time: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val gateName: String = "",
    val timestamp: Long = 0L
)