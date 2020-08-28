package com.antware.joggerlogger

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.antware.joggerlogger.LogViewModel.ExerciseStatus
import com.antware.joggerlogger.LogViewModel.ExerciseStatus.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.JointType.DEFAULT


@Suppress("PrivatePropertyName", "SameParameterValue")
class MapsFragment : Fragment() {

    private val COMPLETE_ROUTE_PADDING: Int = 50
    private val CIRCLE_Z_INDEX = 2F
    private val POLYLINE_Z_INDEX = 1F
    private val CIRCLE_RADIUS_METERS = 8.0
    private val POLYLINE_OUTER_STROKE_WIDTH = 20F
    private val POLYLINE_INNER_STROKE_WIDTH = 10F
    private val OUTLINE_COLOR = R.color.colorBackground
    private val SHAPE_COLOR = R.color.colorPrimaryDark
    private val INITIAL_ZOOM_LEVEL = 16.0f
    private val DEFAULT_MAP_TYPE = GoogleMap.MAP_TYPE_HYBRID

    private var innerPolylines = arrayListOf<Polyline>()
    private var outerPolylines = arrayListOf<Polyline>()
    private var map : GoogleMap? = null
    var currLocation : Location? = null
    private var clearMap: Boolean = false
    private var centerCurrLocation: Boolean = true
    private val model: LogViewModel by activityViewModels()
    private var locationRequested: Boolean = false
    private val locationResult = object : LogLocationManager.BestLocationResult() {

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d("MapsFragment",
                        "Altitude: " + location?.altitude + ", Accuracy: " + location?.verticalAccuracyMeters)
                }
                model.addWaypoint(location?.time?.let { Waypoint(location, model.exerciseStatus) })
                if (!model.exerciseJustStarted()) {
                    val lastLocation = LatLng(model.waypoints.secondLast.latitude, model.waypoints.secondLast.longitude)
                    if (model.waypoints.size == 2 || model.waypoints.secondLast.status == RESUMED)
                        initNewPolyline(lastLocation, here);
                    else {
                        addPolylinePoint(here)
                    }
                }
            }
        }
    }

    private fun addPolylinePoint(currentPosition: LatLng?) {
        val polyLinePoints: MutableList<LatLng> = outerPolylines.last().points
        if (currentPosition != null) polyLinePoints.add(currentPosition)
        outerPolylines.last().points = polyLinePoints
        innerPolylines.last().points = polyLinePoints
    }

    @Suppress("ConstantConditionIf")
    private fun initNewPolyline(startPoint: LatLng?, endPoint: LatLng?){
        var options: PolylineOptions? = PolylineOptions().color(getColor(OUTLINE_COLOR)).width(
            POLYLINE_OUTER_STROKE_WIDTH)
            .zIndex(POLYLINE_Z_INDEX).jointType(DEFAULT)
        if (options != null) {
            map?.addPolyline(options.add(startPoint, endPoint))?.let { outerPolylines.add(it) }
        }
        options = PolylineOptions().color(getColor(SHAPE_COLOR)).width(POLYLINE_INNER_STROKE_WIDTH).zIndex(
            POLYLINE_Z_INDEX)
            .jointType(DEFAULT)
        map?.addPolyline(options.add(startPoint, endPoint))?.let { innerPolylines.add(it) }
    }

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        map = googleMap

        currLocation?.latitude?.let { currLocation?.longitude?.let { it1 -> LatLng(it, it1) } }

        map!!.isMyLocationEnabled = true
        map!!.uiSettings.isMyLocationButtonEnabled = true
        map!!.mapType = DEFAULT_MAP_TYPE

        if (clearMap) {
            map!!.clear()
            clearMap = false
        }
        else {
            if (isAdded && currLocation != null) update(currLocation)
            if (model.waypoints.isNotEmpty()) {
                drawCompletedRoute()
                centerCurrLocation = false
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (!locationRequested) {
            val myLocation = LogLocationManager(requireActivity() as MainActivity?, context)
            myLocation.getLocation(inflater.context, locationResult)
            locationRequested = true
        }
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        model.getStatus().observe(viewLifecycleOwner, Observer { status ->
            currLocation?.let { onStatusChanged(status, true, Waypoint(it, status), CIRCLE_RADIUS_METERS) }
        })
    }

    private fun onStatusChanged(status: ExerciseStatus?, addWaypoints: Boolean, waypoint: Waypoint,
        circleRadius: Double) {
        when (status) {
            STARTED, RESUMED -> {
                if (currLocation == null)
                    if (model.waypoints.isEmpty())
                        map?.clear()
                val color = if (status == STARTED) SHAPE_COLOR else R.color.colorDisabled
                waypoint.latLng?.let { addCircle(circleRadius, OUTLINE_COLOR, color, CIRCLE_Z_INDEX, it) }
                if (addWaypoints) model.addWaypoint(waypoint)
            }
            PAUSED -> waypoint.latLng?.let {
                addCircle(circleRadius, OUTLINE_COLOR, R.color.colorDisabled, CIRCLE_Z_INDEX, it)
            }
            else -> {
                if (model.waypoints.isEmpty()) return
                waypoint.latLng?.let { addCircle(circleRadius, OUTLINE_COLOR, R.color.colorAccent, CIRCLE_Z_INDEX, it)
                }
            }
        }
    }

    private fun getColor(color: Int): Int { return ContextCompat.getColor(requireContext(), color) }

    private fun addCircle(radius: Double, strokeColorId: Int, fillColorId: Int, zIndex: Float, latLng: LatLng) {
        map?.addCircle(latLng.let {
            CircleOptions().center(it).radius(radius).strokeColor(getColor(strokeColorId)).fillColor(getColor(
                fillColorId))
                .zIndex(zIndex).strokeWidth((POLYLINE_OUTER_STROKE_WIDTH - POLYLINE_INNER_STROKE_WIDTH) / 2)
        })
    }

    private fun drawCompletedRoute() {
        val prevMapSize = getMapDiagonalMeters(map)
        map?.moveCamera(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(), COMPLETE_ROUTE_PADDING))
        val mapSizeCoefficient = getMapDiagonalMeters(map) / prevMapSize
        val circleRadius = CIRCLE_RADIUS_METERS * mapSizeCoefficient
        var currStatus: ExerciseStatus? = null
        for ((i, wayPoint) in model.waypoints.withIndex()) {
            val currLatLng = wayPoint.latLng
            if (wayPoint.status != currStatus) {
                currStatus = wayPoint.status
                onStatusChanged(currStatus, false, wayPoint, circleRadius)
                if (currStatus == STARTED || currStatus == RESUMED) continue
            }
            if (i == 1 || model.waypoints.secondLast.status == RESUMED)
                initNewPolyline(model.waypoints[i - 1].latLng, currLatLng)
            else if (i > 1) {
                addPolylinePoint(currLatLng)
            }
        }
    }

    private fun getMapDiagonalMeters(map: GoogleMap?): Float {
        val farLeft = map?.projection?.visibleRegion?.farLeft
        val farRight = map?.projection?.visibleRegion?.farRight
        val results = FloatArray(3)
        farLeft?.latitude?.let {
            if (farRight != null) Location.distanceBetween(it, farLeft.longitude, farRight.latitude, farRight.longitude,
                results)
        }
        return results[0]
    }

    private fun getLatLngBounds(): LatLngBounds? {
        var latLngBounds : LatLngBounds? = null
        for ((i, wayPoint) in model.waypoints.withIndex()) {
            val latLng = wayPoint.latLng
            latLngBounds = if (i == 0) LatLngBounds(latLng, latLng) else latLngBounds?.including(latLng)
        }
        return latLngBounds
    }

    fun reset() {
        centerCurrLocation = true
        clearMap = true
    }

}