package com.company.shakeup

import SafetyApiService
import SafetyRequest
import SafetyResponse
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.company.shakeup.databinding.FragmentVerifierBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class VerifierFragment : Fragment(), OnMapReadyCallback {

    private var param1: String? = null
    private var param2: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var gender: String? = null
    private var age: String? = null
    private lateinit var mMap: GoogleMap
    private var selectedLatLng: LatLng? = null
    private var _verifierBinding: FragmentVerifierBinding? = null
    private val verifierBinding get() = _verifierBinding!!
    private lateinit var mapView: MapView
    private lateinit var api: SafetyApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        try {
            FirebaseApp.initializeApp(requireContext())
        } catch (e: IllegalStateException) {
            Log.d("VerifierFragment", "Firebase already initialized", e)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _verifierBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_verifier,
            container,
            false
        )
        return verifierBinding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize MapView
        mapView = verifierBinding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        verifierBinding.locationInput.setOnClickListener {
            Toast.makeText(requireContext(), "Déplacez la carte pour choisir une position", Toast.LENGTH_SHORT).show()
        }

        // Load user data
        loadUserData()

        // Time picker
        verifierBinding.timeInput.setOnClickListener {
            showTimePicker()
        }

        // Safety check button
        verifierBinding.btnCheckSafety.setOnClickListener {
            validateAndSubmit()
        }
        val baseUrl = "https://${getString(R.string.backend_ip)}/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(SafetyApiService::class.java)

        verifierBinding.mapView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Disable ScrollView immediately on touch
                    verifierBinding.scrollView.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Re-enable ScrollView after releasing the map
                    verifierBinding.scrollView.requestDisallowInterceptTouchEvent(false)
                }
                // No need for ACTION_MOVE - handled by the initial disallow
            }
            // Delegate touch events to the MapView
            false
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        configureMapSettings()
        setDefaultLocation()

        mMap.setOnMapClickListener { latLng ->
            selectedLatLng = latLng
            mMap.clear()
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.selected_location))
            )?.showInfoWindow()
            getAddressFromLatLng(latLng)
        }
    }

    private fun configureMapSettings() {
        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
            isTiltGesturesEnabled = true
            isRotateGesturesEnabled = true
            isMyLocationButtonEnabled = true
        }
    }

    private fun setDefaultLocation() {
        val marrakech = LatLng(31.6295, -7.9811)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marrakech, 12f))
    }

    private fun getAddressFromLatLng(latLng: LatLng) {
        if (!Geocoder.isPresent()) {
            showToast(getString(R.string.geocoder_not_available))
            return
        }

        Geocoder(requireContext(), Locale.getDefault()).runCatching {
            getFromLocation(latLng.latitude, latLng.longitude, 1)
        }.onSuccess { addresses ->
            addresses?.firstOrNull()?.let { address ->
                val addressText = address.getAddressLine(0) ?: return@let
                verifierBinding.locationInput.setText(addressText)
            }
        }.onFailure { e ->
            Log.e("VerifierFragment", "Geocoder error", e)
            showToast(getString(R.string.error_getting_address))
        }
    }

    private fun loadUserData() {
        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        gender = if (document.getString("sexe") == "Homme") "1" else "0"
                        age = document.getString("age")
                    } else {
                        showToast(getString(R.string.user_not_found))
                    }
                }
                .addOnFailureListener { e ->
                    showToast(getString(R.string.error_loading_data))
                    Log.e("VerifierFragment", "Firestore error", e)
                }
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                verifierBinding.timeInput.setText("%02d:%02d".format(hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun validateAndSubmit() {
        val timeStr = verifierBinding.timeInput.text.toString()
        val hour = timeStr.split(":").firstOrNull()?.toIntOrNull()

        if (hour == null || selectedLatLng == null || gender == null) {
            showToast(getString(R.string.fill_all_fields))
            return
        }

        Log.d("VerifierFragment",
            "Sending: LAT=${selectedLatLng!!.latitude}, " +
                    "LON=${selectedLatLng!!.longitude}, " +
                    "hour=$hour, gender=$gender, age=$age")

        // TODO: Send to FastAPI
        val request = SafetyRequest(
            latitude = selectedLatLng!!.latitude,
            longitude = selectedLatLng!!.longitude,
            hour = hour,
            gender = gender!!,
            age = age
        )

        api.checkSafety(request).enqueue(object : Callback<SafetyResponse> {
            override fun onResponse(call: Call<SafetyResponse>, response: Response<SafetyResponse>) {
                if (response.isSuccessful) {
                    val safety = response.body()
                    val message = if (safety?.safe == 0) {
                        "Ce lieu est sécurisé ✅"
                    } else {
                        "Attention : Ce lieu peut ne pas être sécurisé ⚠️"
                    }

                    Log.d("SafetyCheck", message)

                    verifierBinding.safeResult.text = message

                } else {
                    showToast("Server error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<SafetyResponse>, t: Throwable) {
                showToast("Network error: ${t.message}")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // MapView lifecycle methods
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        _verifierBinding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        fun newInstance(param1: String = "", param2: String = "") =
            VerifierFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}