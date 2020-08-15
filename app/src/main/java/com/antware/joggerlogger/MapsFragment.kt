package com.antware.joggerlogger

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
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
    private val INITIAL_ZOOM_LEVEL = 16.0f
    private val DEFAULT_MAP_TYPE = GoogleMap.MAP_TYPE_HYBRID

    private var map : GoogleMap? = null
    var currLocation : Location? = null
    private val model: LogViewModel by activityViewModels()
    private val locationResult = object : MyLocation2.BestLocationResult() {

        override fun gotLocation(location: Location?) {
            currLocation = location
            if (location != null) moveCameraToCurrentLoc(location)
        }
    }

    private fun moveCameraToCurrentLoc(location: Location?) {
        val here = location?.latitude?.let { LatLng(it, location.longitude) }
        requireActivity().runOnUiThread {
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(here, INITIAL_ZOOM_LEVEL))
            if (model.exerciseStatus == STARTED || model.exerciseStatus == RESUMED) {
                model.addWaypoint(
                    location?.time?.let {
                        Waypoint(
                            location,
                            model.exerciseStatus
                        )
                    }
                )
                if (!model.exerciseJustStarted()) addPolyline(here)
            }
        }
    }

    private fun addPolyline(startPoint: LatLng?) {
        val numWaypoints = model.waypoints.size
        val endPoint = getLatLng(model.waypoints[numWaypoints - 2].location)
        val options = PolylineOptions().color(ContextCompat.getColor(requireActivity(), R.color.colorPrimary))
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

        if (currLocation != null) moveCameraToCurrentLoc(currLocation)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val myLocation = MyLocation2(requireActivity())
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
            STARTED, RESUMED -> {
                if (model.waypoints.isEmpty())
                    map?.clear()
                val color = if (status == STARTED) R.color.colorPrimary else R.color.colorDisabled
                addCircle(STATUS_CHANGED_CIRCLE_RADIUS, Color.WHITE, color, CIRCLE_Z_INDEX, currLocation)
                model.addWaypoint(Waypoint(currLocation, status))
            }
            PAUSED -> addCircle(STATUS_CHANGED_CIRCLE_RADIUS, Color.WHITE, R.color.colorDisabled, CIRCLE_Z_INDEX, currLocation)
            else -> {
                if (model.waypoints.isEmpty()) return
                val location : Location? = if (model.status == STOPPED) currLocation else model.waypoints.last().location
                addCircle(STATUS_CHANGED_CIRCLE_RADIUS, Color.WHITE, R.color.colorAccent, CIRCLE_Z_INDEX, location)
            }
        }
    }

    private fun addCircle(
        radius: Double,
        strokeColor: Int,
        fillColorId: Int,
        zIndex: Float,
        location: Location?
    ) {
        if (location == null) return
        map?.addCircle(getLatLng(location)?.let {
            CircleOptions().center(it).radius(radius).strokeColor(strokeColor).fillColor(ContextCompat.getColor(requireActivity(),
                fillColorId)).zIndex(zIndex)
        })
    }

    private fun getLatLng(currLocation: Location?): LatLng? {
        return currLocation?.latitude?.let { LatLng(it, currLocation.longitude) }
    }
}