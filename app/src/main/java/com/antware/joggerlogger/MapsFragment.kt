package com.antware.joggerlogger

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.antware.joggerlogger.LogViewModel.Waypoint
import com.antware.joggerlogger.LogViewModel.ExerciseStatus.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

@Suppress("PrivatePropertyName", "SameParameterValue")
class MapsFragment : Fragment() {

    private val CIRCLE_Z_INDEX = 1F
    private val STATUS_CHANGED_CIRCLE_RADIUS = 10.0
    private val POLYLINE_WIDTH = 25F
    private val STOP_EXERCISE_MARKER_COLOR = 0F
    private val INITIAL_ZOOM_LEVEL = 16.0f
    private val DEFAULT_MAP_TYPE = GoogleMap.MAP_TYPE_HYBRID
    private val START_EXERCISE_MARKER_COLOR = 171F

    private var map : GoogleMap? = null
    var currLocation : Location? = null
    private val model: LogViewModel by activityViewModels()
    private val locationResult = object : MyLocation.LocationResult() {

        override fun gotLocation(location: Location?) {
            currLocation = location
            moveCameraToCurrentLoc(location)
        }
    }

    private fun moveCameraToCurrentLoc(location: Location?) {
        val here = location?.latitude?.let { LatLng(it, location.longitude) }
        requireActivity().runOnUiThread {
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(here, INITIAL_ZOOM_LEVEL))
            if (model.exerciseStatus == STARTED) {
                model.addWaypoint(Waypoint(location, model.exerciseStatus))
                addPolyline(here)
            }
        }
        //Log.d(here?.latitude.toString() + ", " + here?.longitude.toString(), "No location data")
    }

    private fun addPolyline(startPoint: LatLng?) {
        val numWaypoints = model.waypoints.size
        val endPoint = if (numWaypoints > 1) getLatLng(model.waypoints[numWaypoints - 2].location) else startPoint
        val options = PolylineOptions().color(ContextCompat.getColor(requireActivity(), R.color.colorPrimary))
           // .pattern(listOf(Dot())).width(POLYLINE_WIDTH)
        map?.addPolyline(options.add(startPoint, endPoint))
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
        model.getStatus().observe(viewLifecycleOwner, Observer { status ->
            onStatusChanged(status)
        })
    }

    private fun onStatusChanged(status: LogViewModel.ExerciseStatus?) {
        when (status) {
            STARTED -> {
                addCircle(STATUS_CHANGED_CIRCLE_RADIUS, Color.WHITE, R.color.colorPrimary, CIRCLE_Z_INDEX)
            }
            else -> {
                addCircle(STATUS_CHANGED_CIRCLE_RADIUS, Color.WHITE, R.color.colorAccent, CIRCLE_Z_INDEX)
                model.addWaypoint(Waypoint(currLocation, status))
            }
        }
        model.addWaypoint(Waypoint(currLocation, status))
    }

    private fun addCircle(radius: Double, strokeColor: Int, fillColorId: Int, zIndex: Float) {
        map?.addCircle(getLatLng(this.currLocation)?.let {
            CircleOptions().center(it).radius(radius).strokeColor(strokeColor).fillColor(ContextCompat.getColor(requireActivity(),
                fillColorId)).zIndex(zIndex)
        })
    }

    private fun getLatLng(currLocation: Location?): LatLng? {
        return currLocation?.latitude?.let { LatLng(it, currLocation.longitude) }
    }
}