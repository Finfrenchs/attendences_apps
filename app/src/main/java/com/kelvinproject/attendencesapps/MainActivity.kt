package com.kelvinproject.attendencesapps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.skyfishjy.library.RippleBackground


class MainActivity : AppCompatActivity() {

    companion object {
        const val ID_LOCATION_PERMISSION = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissionLocation()
        onClick()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ID_LOCATION_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Berhasil di ijinkan", Toast.LENGTH_SHORT).show()

                if (!isLocationEnable()) {
                    Toast.makeText(this, "Please, turn on your location (GPS)", Toast.LENGTH_SHORT)
                        .show()
                    //direct to location setting
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            } else {
                Toast.makeText(this, "Gagal di ijinkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionLocation() {
        if (checkPermission()) {
            if (!isLocationEnable()) {
                Toast.makeText(this, "Please, turn on your location (GPS)", Toast.LENGTH_SHORT)
                    .show()
                //direct to location setting
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        } else {
            requestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun isLocationEnable(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        ) {
            return true
        }
        return false
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            ID_LOCATION_PERMISSION
        )
    }

    private fun onClick() {
        val fabCheckIn = findViewById<FloatingActionButton>(R.id.fabCheckIn)
        fabCheckIn.setOnClickListener {
            loadScanLocation()
            /*Old Code deprecated*/
            /*Handler().postDelayed({
                getLastLocation()

            }, 4000)*/
            Handler(Looper.getMainLooper()).postDelayed({
                getLastLocation()
            }, 2000)
        }
    }

    private fun loadScanLocation() {
        val rippleBackground = findViewById<RippleBackground>(R.id.rippleBackground)
        val tvScanning = findViewById<TextView>(R.id.tvScanning)
        val tvCheckInSucces = findViewById<TextView>(R.id.tvCheckInSuccess)
        rippleBackground.startRippleAnimation()
        tvScanning.visibility = View.VISIBLE
        tvCheckInSucces.visibility = View.GONE
    }

    private fun stopScanLocation() {
        val rippleBackground = findViewById<RippleBackground>(R.id.rippleBackground)
        val tvScanning = findViewById<TextView>(R.id.tvScanning)
        rippleBackground.stopRippleAnimation()
        tvScanning.visibility = View.GONE
    }

    private fun getLastLocation() {
        if (checkPermission()) {
            if (isLocationEnable()) {
                LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { location ->
                    val currentLat = location.latitude
                    val currentLong = location.longitude

                    val tvCheckInSuccess = findViewById<TextView>(R.id.tvCheckInSuccess)
                    tvCheckInSuccess.visibility = View.VISIBLE
                    tvCheckInSuccess.text = "lat: $currentLat, lon: $currentLong "

                    stopScanLocation()
                }
            } else {
                Toast.makeText(this, "Please, turn on your location (GPS)", Toast.LENGTH_SHORT)
                    .show()
                //direct to location setting
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        } else {
            requestPermission()
        }
    }
}