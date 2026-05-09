package com.college.bustracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.database.FirebaseDatabase

class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private var driverId: String = ""
    private var busId: String = ""
    private var driverName: String = "Driver"

    private var routeName: String = ""
    private var startLocation: String = ""
    private var endLocation: String = ""

    // IMPORTANT
    private var isServiceRunning = false

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(2000L).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            stopTrackingNow()
            return START_NOT_STICKY
        }

        driverId = intent?.getStringExtra("driverId") ?: ""
        busId = normalizeBusId(intent?.getStringExtra("busId"))
        driverName = intent?.getStringExtra("driverName") ?: "Driver"
        routeName = intent?.getStringExtra("routeName") ?: ""
        startLocation = intent?.getStringExtra("startLocation") ?: ""
        endLocation = intent?.getStringExtra("endLocation") ?: ""

        if (busId.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        isServiceRunning = true

        FirebaseDatabase.getInstance()
            .getReference("serviceDebug")
            .child(busId)
            .setValue(
                mapOf(
                    "message" to "Service started",
                    "driverId" to driverId,
                    "driverName" to driverName,
                    "time" to System.currentTimeMillis()
                )
            )

        startForeground(1, createNotification())
        startLocationUpdates()

        return START_NOT_STICKY
    }

    private fun normalizeBusId(rawBusId: String?): String {
        if (rawBusId.isNullOrBlank()) return ""
        val number = rawBusId.filter { it.isDigit() }
        return if (number.isNotEmpty()) {
            "BUS-" + number.padStart(2, '0')
        } else {
            rawBusId.trim().uppercase()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            FirebaseDatabase.getInstance()
                .getReference("serviceDebug")
                .child(busId)
                .child("error")
                .setValue("ACCESS_FINE_LOCATION not granted")
            stopTrackingNow()
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!isServiceRunning) return

                val location = locationResult.lastLocation ?: return
                updateLocationToFirebase(location)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun updateLocationToFirebase(location: Location) {
        if (!isServiceRunning) return

        val mapData = mapOf(
            "busName" to busId,
            "driverId" to driverId,
            "driverName" to driverName,
            "routeName" to routeName,
            "startLocation" to startLocation,
            "endLocation" to endLocation,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "speed" to location.speed.toDouble(),
            "status" to "online",
            "updatedAt" to System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance()
            .getReference("buses")
            .child(busId)
            .updateChildren(mapData)
    }

    private fun stopTrackingNow() {
        isServiceRunning = false

        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        if (busId.isNotEmpty()) {
            FirebaseDatabase.getInstance()
                .getReference("buses")
                .child(busId)
                .child("status")
                .setValue("offline")
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val channelId = "bus_tracking_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bus Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bus Tracking Running")
            .setContentText("Tracking $busId")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isServiceRunning = false

        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}