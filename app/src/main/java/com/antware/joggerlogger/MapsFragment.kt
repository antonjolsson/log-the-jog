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
import com.antware.joggerlogger.LogViewModel.*
import com.antware.joggerlogger.LogViewModel.ExerciseStatus.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

@Suppress("PrivatePropertyName", "SameParameterValue")
class MapsFragment : Fragment() {

    private val COMPLETE_ROUTE_PADDING: Int = 50
    private val CIRCLE_Z_INDEX = 1F
    private val CIRCLE_RADIUS_METERS = 10.0
    private val INITIAL_ZOOM_LEVEL = 16.0f
    private val DEFAULT_MAP_TYPE = GoogleMap.MAP_TYPE_HYBRID

    private var map : GoogleMap? = null
    var currLocation : Location? = null
    private var centerCurrLocation: Boolean = true
    private val model: LogViewModel by activityViewModels()
    private val locationResult = object : MyLocation2.BestLocationResult() {

        override fun gotLocation(location: Location?) {
            currLocation = location
            if (isAdded && location != null) update(location)
        }
    }

    private fun update(location: Location?) {
        val here = location?.latitude?.let { LatLng(it, location.longitude) }
        requireActivity().runOnUiThread {
            if (centerCurrLocation) map?.moveCamera(CameraUpdateFactory.newLatLngZoom(here, INITIAL_ZOOM_LEVEL))
            if (model.exerciseStatus == STARTED || model.exerciseStatus == RESUMED) {
                model.addWaypoint(location?.time?.let {
                        Waypoint(location, model.exerciseStatus) })
                if (!model.exerciseJustStarted()) {
                    val numWaypoints = model.waypoints.size
                    val lastLocation = getLatLng(model.waypoints[numWaypoints - 2].location)
                    addPolyline(here, lastLocation)}
            }
        }
    }

    private fun addPolyline(startPoint: LatLng?, endPoint: LatLng?) {
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

        if (isAdded && currLocation != null) update(currLocation)
        if (model.waypoints.isNotEmpty()) {
            drawCompletedRoute()
            centerCurrLocation = false
        }
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
            currLocation?.let { onStatusChanged(status, true, it, CIRCLE_RADIUS_METERS) }
        })
    }

    private fun onStatusChanged(
        status: ExerciseStatus?,
        addWaypoints: Boolean,
        location: Location,
        circleRadius: Double
    ) {
        when (status) {
            STARTED, RESUMED -> {
                if (model.waypoints.isEmpty())
                    map?.clear()
                val color = if (status == STARTED) R.color.colorPrimary else R.color.colorDisabled
                addCircle(circleRadius, Color.WHITE, color, CIRCLE_Z_INDEX, location)
                if (addWaypoints) model.addWaypoint(Waypoint(location, status))
            }
            PAUSED -> addCircle(circleRadius, Color.WHITE, R.color.colorDisabled, CIRCLE_Z_INDEX, location)
            else -> {
                if (model.waypoints.isEmpty()) return
                addCircle(circleRadius, Color.WHITE, R.color.colorAccent, CIRCLE_Z_INDEX, location)
            }
        }
    }

    private fun addCircle(radius: Double, strokeColor: Int, fillColorId: Int, zIndex: Float, location: Location?) {
        if (location == null) return
        map?.addCircle(getLatLng(location)?.let {
            CircleOptions().center(it).radius(radius).strokeColor(strokeColor).fillColor(ContextCompat.getColor(requireActivity(),
                fillColorId)).zIndex(zIndex)
        })
    }

    private fun getLatLng(currLocation: Location?): LatLng? {
        return currLocation?.latitude?.let { LatLng(it, currLocation.longitude) }
    }

    private fun drawCompletedRoute() {
        val prevMapSize = getMapDiagonalMeters(map)
        map?.moveCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(), COMPLETE_ROUTE_PADDING))
        val circleRadius = CIRCLE_RADIUS_METERS * getMapDiagonalMeters(map) / prevMapSize
        var currStatus: ExerciseStatus? = null
        for ((i, wayPoint) in model.waypoints.withIndex()) {
            val currLatLng = getLatLng(wayPoint.location)
            if (wayPoint.status != currStatus) {
                currStatus = wayPoint.status
                onStatusChanged(currStatus, false, wayPoint.location, circleRadius)
                if (currStatus == STARTED || currStatus == RESUMED) continue
            }
            if (i > 0) addPolyline(getLatLng(model.waypoints[i - 1].location), currLatLng)
        }
    }

    private fun getMapDiagonalMeters(map: GoogleMap?): Float {
        val farLeft = map?.projection?.visibleRegion?.farLeft
        val farRight = map?.projection?.visibleRegion?.farRight
        val results = FloatArray(3)
        farLeft?.latitude?.let {
            if (farRight != null) Location.distanceBetween(it, farLeft.longitude, farRight.latitude, farRight.longitude, results)
        }
        return results[0]
    }

    private fun getLatLngBounds(): LatLngBounds? {
        var latLngBounds : LatLngBounds? = null
        for ((i, wayPoint) in model.waypoints.withIndex()) {
            val latLng = getLatLng(wayPoint.location)
            latLngBounds = if (i == 0) LatLngBounds(latLng, latLng) else latLngBounds?.including(latLng)
        }
        return latLngBounds
    }

}