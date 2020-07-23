package com.antware.joggerlogger

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

@Suppress("PrivatePropertyName")
class MapsFragment : Fragment() {

    private val INITIAL_ZOOM_LEVEL = 16.0f
    private val DEFAULT_MAP_TYPE = GoogleMap.MAP_TYPE_HYBRID

    private var map : GoogleMap? = null
    var currLocation : Location? = null

    private val locationResult = object : MyLocation.LocationResult() {

        override fun gotLocation(location: Location?) {
            currLocation = location
            moveCameraToCurrentLoc(location)
        }
    }

    private fun moveCameraToCurrentLoc(location: Location?) {
        val here = location?.latitude?.let { LatLng(it, location.longitude) }
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(here, INITIAL_ZOOM_LEVEL))
        Log.d(here?.latitude.toString() + ", " + here?.longitude.toString(), "No location data")
    }

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        map = googleMap

        currLocation?.latitude?.let { currLocation?.longitude?.let { it1 -> LatLng(it, it1) } }

        map!!.isMyLocationEnabled = true // TODO Add permission check?
        map!!.uiSettings.isMyLocationButtonEnabled = true
        map!!.mapType = DEFAULT_MAP_TYPE

        moveCameraToCurrentLoc(currLocation)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val myLocation = MyLocation()
        myLocation.getLocation(inflater.context, locationResult)

        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }
}