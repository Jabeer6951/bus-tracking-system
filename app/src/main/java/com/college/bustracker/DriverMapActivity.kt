package com.college.bustracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DriverMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var btnStartBus: Button
    private lateinit var btnStopBus: Button
    private lateinit var tvDriverName: TextView
    private lateinit var tvBusId: TextView
    private lateinit var tvStatus: TextView
    private lateinit var database: DatabaseReference
    private lateinit var locationRef: DatabaseReference

    private var driverId: String = ""
    private var busId: String = ""
    private var driverName: String = "Driver"
    private var busMarker: Marker? = null
    private var isMapReady = false
    private var isFirstLocation = true

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 101
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_map)

        driverId = intent.getStringExtra("driverId") ?: ""
        busId = normalizeBusId(intent.getStringExtra("busId"))

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        btnStartBus = findViewById(R.id.btnStartBus)
        btnStopBus = findViewById(R.id.btnStopBus)
        tvDriverName = findViewById(R.id.tvDriverName)
        tvBusId = findViewById(R.id.tvBusId)
        tvStatus = findViewById(R.id.tvStatus)

        tvDriverName.text = "Driver: Loading..."
        tvBusId.text = "Bus: $busId"
        tvStatus.text = "Status: OFFLINE"
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        database = FirebaseDatabase.getInstance().getReference("drivers")
        locationRef = FirebaseDatabase.getInstance()
            .getReference("buses")
            .child(busId)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestNotificationPermissionIfNeeded()
        loadHeaderData()
        setupMenu()

        btnStartBus.setOnClickListener {
            if (driverId.isEmpty() || busId.isEmpty()) {
                Toast.makeText(this, "Driver ID or Bus ID missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkPermissionsAndStartTracking()
        }

        btnStopBus.setOnClickListener {
            stopBusTracking()
        }
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun loadHeaderData() {
        val headerView = navigationView.getHeaderView(0)
        val txtName = headerView.findViewById<TextView>(R.id.txtDriverNameHeader)
        val imgPhoto = headerView.findViewById<ImageView>(R.id.imgDriverPhotoHeader)

        database.child(driverId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                driverName = snapshot.child("name").getValue(String::class.java) ?: "Driver"
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: ""

                txtName.text = driverName
                tvDriverName.text = "Driver: $driverName"
                tvBusId.text = "Bus: $busId"

                if (photoUrl.isNotEmpty()) {
                    Glide.with(this@DriverMapActivity).load(photoUrl).into(imgPhoto)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriverMapActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupMenu() {
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menuProfile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("driverId", driverId)
                    intent.putExtra("busId", busId)
                    startActivity(intent)
                }

                R.id.menuFeedback -> {
                    val intent = Intent(this, FeedbackActivity::class.java)
                    intent.putExtra("driverId", driverId)
                    intent.putExtra("busId", busId)
                    startActivity(intent)
                }

                R.id.menuLogout -> {
                    stopBusTracking()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = false
        }

        val defaultLocation = LatLng(16.6915, 80.3633)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

        listenBusLocation()
    }

    private fun listenBusLocation() {
        if (busId.isEmpty()) return

        locationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val lat = snapshot.child("latitude").value?.toString()?.toDoubleOrNull() ?: 0.0
                val lng = snapshot.child("longitude").value?.toString()?.toDoubleOrNull() ?: 0.0
                val status = snapshot.child("status").getValue(String::class.java) ?: "offline"

                if (lat != 0.0 && lng != 0.0 && isMapReady) {
                    val currentLatLng = LatLng(lat, lng)

                    if (busMarker != null) {
                        busMarker?.remove()
                    }

                    busMarker = mMap.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Bus $busId")
                    )

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                }

                if (status.equals("online", ignoreCase = true)) {
                    tvStatus.text = "Status: LIVE"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(this@DriverMapActivity, android.R.color.holo_green_dark)
                    )
                } else {
                    tvStatus.text = "Status: OFFLINE"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(this@DriverMapActivity, android.R.color.holo_red_dark)
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriverMapActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkPermissionsAndStartTracking() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        startBusTracking()
    }

    private fun startBusTracking() {
        val serviceIntent = Intent(this, LocationForegroundService::class.java)
        serviceIntent.putExtra("driverId", driverId)
        serviceIntent.putExtra("busId", busId)
        serviceIntent.putExtra("driverName", driverName)

        // NEW
        serviceIntent.putExtra("routeName", busId)
        serviceIntent.putExtra("startLocation", "College")
        serviceIntent.putExtra("endLocation", "Destination")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Live tracking started", Toast.LENGTH_SHORT).show()
    }

    private fun stopBusTracking() {
        val stopIntent = Intent(this, LocationForegroundService::class.java)
        stopIntent.action = LocationForegroundService.ACTION_STOP

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(stopIntent)
        } else {
            startService(stopIntent)
        }

        Toast.makeText(this, "Bus stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (isMapReady &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = false
            }
            startBusTracking()
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}