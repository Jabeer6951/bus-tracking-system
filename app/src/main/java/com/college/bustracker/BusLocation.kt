package com.college.bustracker

data class BusLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "offline",
    val updatedAt: Long = 0L
)