package com.college.bustracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var busMarker: Marker? = null
    private var isTrackingStarted = false
    private val busId = "BUS-01"
    
    // Fixed College Location
    private val collegeLocation = LatLng(17.3850, 78.4867)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val startBus = findViewById<Button>(R.id.startBus)
        val chip = findViewById<Chip>(R.id.busStartedChip)

        startBus.setOnClickListener {
            chip.visibility = View.VISIBLE
            if (!isTrackingStarted) {
                isTrackingStarted = true
                startLocationUpdates()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        // Add a permanent marker for the College
        mMap.addMarker(
            MarkerOptions()
                .position(collegeLocation)
                .title("College Campus")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
        
        // Show the college on startup
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(collegeLocation, 14f))
        
        // Show my blue dot location if permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(2000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val latLng = LatLng(location.latitude, location.longitude)

                // Update the BUS marker position ONLY
                if (busMarker == null) {
                    busMarker = mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Bus $busId")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_icon))
                    )
                } else {
                    busMarker?.position = latLng
                }

                // REMOVED camera move/animate commands to prevent map "jumping" 
                // when tracking starts or updates.

                val ref = FirebaseDatabase.getInstance().getReference("busLocation").child(busId)
                val mapData = mapOf(
                    "busName" to busId,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "status" to "online",
                    "updatedAt" to System.currentTimeMillis()
                )
                ref.setValue(mapData)
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}