package com.kelvinproject.attendencesapps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.FirebaseDatabase
import com.skyfishjy.library.RippleBackground
import java.lang.Math.toRadians
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*


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

                    //cek sixe yang di keluarkan getAddresses
                    /*Log.d("coba", "size: ${getAddresses().size}")
                    for (address: Address in getAddresses()) {
                        Log.d("coba", "lat: ${address.latitude}, lon: ${address.longitude}")
                        Log.d("coba", "postcode: ${address.postalCode}, phone: ${address.phone}")
                    }*/

                    val distance = calculateDistance(
                        currentLat,
                        currentLong,
                        getAddresses()[0].latitude,
                        getAddresses()[0].longitude
                    ) * 1000

                    if (distance < 1000.0) {
                        showDialogForm()
                    } else {
                        tvCheckInSuccess.visibility = View.VISIBLE
                        //tvCheckInSuccess.text = "lat: $currentLat, lon: $currentLong "
                        tvCheckInSuccess.text = "Out of range"
                    }
                    //Log.d("coba", "Current Location: $currentLat, $currentLong")
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

    private fun showDialogForm() {
        val dialogForm = LayoutInflater.from(this)
            .inflate(R.layout.layout_dialog_form, null)
        //val etName = dialogForm.findViewById<EditText>(R.id.etName).text.toString()
        AlertDialog.Builder(this)
            .setView(dialogForm)
            .setCancelable(false)
            .setPositiveButton("Submit") { dialog, _ ->
                val name = dialogForm.findViewById<EditText>(R.id.etName).text.toString()
                //Toast.makeText(this, "name: $name", Toast.LENGTH_SHORT).show()
                inputDataToFirebase(name)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun inputDataToFirebase(name: String) {
        val user = User(name, getCurrentDate())

        val database =
            FirebaseDatabase.getInstance("https://attendences-online-apps-default-rtdb.asia-southeast1.firebasedatabase.app")
        val attendanceRef = database.getReference("log_attendance")
        attendanceRef.child(name).setValue(user)
            .addOnSuccessListener {
                val tvCheckInSuccess = findViewById<TextView>(R.id.tvCheckInSuccess)
                tvCheckInSuccess.visibility = View.VISIBLE
                tvCheckInSuccess.text = "Check-In Success"
            }
            .addOnFailureListener {
                Toast.makeText(this, "${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCurrentDate(): String {
        val currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(currentTime)
    }

    private fun getAddresses(): List<Address> {
        val destination = "Balai Besar Wilayah Sungai Brantas"
        val geocode = Geocoder(this, Locale.getDefault())
        return geocode.getFromLocationName(destination, 100)
    }

    //Hitung Jarak
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6372.8 // in kilometers
        val radiansLat1 = toRadians(lat1)
        val radianLat2 = toRadians(lat2)
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        return 2 * r * asin(
            sqrt(
                sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(radiansLat1) * cos(
                    radianLat2
                )
            )
        )
    }
}