package com.example.smartfarmadvisor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.smartfarmadvisor.data.FarmDatabase
import com.example.smartfarmadvisor.data.FarmEntity
import com.example.smartfarmadvisor.databinding.ActivityAddFarmBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import java.util.Locale

class AddFarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFarmBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var detectedLatitude: Double? = null
    private var detectedLongitude: Double? = null

    private var locationCallback: LocationCallback? = null

    private val cropOptions = arrayOf(
        "Rice", "Wheat", "Maize", "Cotton", "Sugarcane",
        "Groundnut", "Tomato", "Chilli", "Other"
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            getCurrentLocation()
        } else {
            binding.txtLocationStatus.text = "Permission denied. Please grant location permissions in device settings."
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupCropSpinner()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDetectLocation.setOnClickListener {
            checkAndDetectLocation()
        }

        binding.btnSaveFarm.setOnClickListener {
            saveFarm()
        }
    }

    private fun setupCropSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            cropOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCrop.adapter = adapter
    }

    private fun checkAndDetectLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            getCurrentLocation()
        } else {
            binding.txtLocationStatus.text = "Requesting location permissions..."
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            binding.txtLocationStatus.text = "GPS/Location is disabled. Please turn on Location in device settings."
            Toast.makeText(this, "Please enable GPS on your device", Toast.LENGTH_LONG).show()
            return
        }

        binding.txtLocationStatus.text = "Detecting real-time GPS location..."
        binding.btnDetectLocation.isEnabled = false

        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    updateLocationUI(location.latitude, location.longitude)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            updateLocationUI(lastLoc.latitude, lastLoc.longitude)
                        } else {
                            requestActiveLocationUpdates()
                        }
                    }.addOnFailureListener {
                        requestActiveLocationUpdates()
                    }
                }
            }.addOnFailureListener {
                requestActiveLocationUpdates()
            }
        } catch (e: SecurityException) {
            binding.btnDetectLocation.isEnabled = true
            binding.txtLocationStatus.text = "Location permission error: ${e.localizedMessage}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestActiveLocationUpdates() {
        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).setMinUpdateIntervalMillis(1000L)
            .setMaxUpdates(1)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    updateLocationUI(location.latitude, location.longitude)
                } else {
                    binding.btnDetectLocation.isEnabled = true
                    binding.txtLocationStatus.text = "Location unavailable. Ensure GPS has sky visibility and try again."
                }
                stopLocationUpdates()
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            binding.btnDetectLocation.isEnabled = true
            binding.txtLocationStatus.text = "Permission error: ${e.localizedMessage}"
        }
    }

    private fun updateLocationUI(lat: Double, lon: Double) {
        detectedLatitude = lat
        detectedLongitude = lon

        binding.txtLatitude.text = String.format(Locale.US, "%.5f", lat)
        binding.txtLongitude.text = String.format(Locale.US, "%.5f", lon)
        binding.txtLocationStatus.text = "Real-time GPS location detected successfully! ✓"
        binding.btnDetectLocation.isEnabled = true
        binding.btnDetectLocation.text = "🔄 Refresh GPS Location"
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun saveFarm() {
        val farmName = binding.etFarmName.text.toString().trim()
        val cropName = binding.spinnerCrop.selectedItem?.toString() ?: cropOptions[0]

        if (farmName.isEmpty()) {
            binding.etFarmName.error = "Please enter farm name"
            binding.etFarmName.requestFocus()
            return
        }

        val lat = detectedLatitude
        val lon = detectedLongitude

        if (lat == null || lon == null) {
            Toast.makeText(this, "Please click 'Detect GPS Location' to retrieve coordinates before saving", Toast.LENGTH_LONG).show()
            return
        }

        val farm = FarmEntity(
            farmName = farmName,
            cropName = cropName,
            latitude = lat,
            longitude = lon,
            createdAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            FarmDatabase.getDatabase(this@AddFarmActivity)
                .farmDao()
                .insertFarm(farm)

            Toast.makeText(this@AddFarmActivity, "Farm registered successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
