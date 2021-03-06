package com.antware.joggerlogger

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.antware.joggerlogger.LocationService.ServiceBinder
import com.antware.joggerlogger.LogViewModel.ExerciseStatus
import com.antware.joggerlogger.LogViewModel.ExerciseStatus.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.JointType.DEFAULT

/**
 * Class for map logic.
 * @author Anton J Olsson
 */
@Suppress("PrivatePropertyName", "SameParameterValue")
class MapsFragment : Fragment() {

    companion object { const val TAG: String = "MapsFragment" }

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
    private var centerCurrLocation: Boolean = true
    private val model: LogViewModel by activityViewModels()

    /**
     * Location callback.
     */
    private val locationResult = object : LocationService.BestLocationResult() {
        override fun gotLocation(location: Location?) {
            currLocation = location
            if (isAdded && location != null) update(location)
        }
    }

    /**
     * If location isn't null, wraps it in a Waypoint and adds it to the viewmodel.
     */
    private fun update(location: Location?) {
        val here = location?.latitude?.let { LatLng(it, location.longitude) }
        requireActivity().runOnUiThread {
            if (centerCurrLocation) map?.moveCamera(CameraUpdateFactory.newLatLngZoom(here, INITIAL_ZOOM_LEVEL))
            if (model.exerciseStatus == STARTED || model.exerciseStatus == RESUMED) {
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
        if (outerPolylines.isEmpty()) return
        val polyLinePoints: MutableList<LatLng> = outerPolylines.last().points
        if (currentPosition != null) polyLinePoints.add(currentPosition)
        outerPolylines.last().points = polyLinePoints
        innerPolylines.last().points = polyLinePoints
    }

    /**
     * Initializes two polylines, one white outer and one cyan inner.
     */
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

    /**
     * If viewmodel is reloaded, redraws the route.
     */
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
        map!!.clear()

        if (isAdded && currLocation != null) update(currLocation)

        if (model.waypoints.isNotEmpty()) {
            if (model.waypoints.last.status == STOPPED_AFTER_PAUSED)
                centerCurrLocation = false
            if (model.isReloaded) map!!.setOnMapLoadedCallback {
                redrawRoute(!centerCurrLocation)
            }
            else redrawRoute(!centerCurrLocation)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (requireActivity() as MainActivity).addLocationResultListener(inflater.context, locationResult)
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    /**
     * Adds observer for exercise status change.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        model.getStatus().observe(viewLifecycleOwner, { status ->
            currLocation?.let { onStatusChanged(status, true, Waypoint(it, status), CIRCLE_RADIUS_METERS) }
        })
    }

    /**
     * Logic for viewodel status change.
     */
    private fun onStatusChanged(status: ExerciseStatus?, addWaypoints: Boolean, waypoint: Waypoint,
        circleRadius: Double) {
        when (status) {
            STARTED, RESUMED -> onExerciseStarted(status, waypoint, circleRadius, addWaypoints)
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

    /**
     * Initializes the map.
     */
    private fun onExerciseStarted(status: ExerciseStatus?, waypoint: Waypoint, circleRadius: Double,
        addWaypoints: Boolean) {
        if (currLocation == null)
            if (model.waypoints.isEmpty())
                map?.clear()
        val color = if (status == STARTED) SHAPE_COLOR else R.color.colorDisabled
        waypoint.latLng?.let { addCircle(circleRadius, OUTLINE_COLOR, color, CIRCLE_Z_INDEX, it) }
        if (addWaypoints) model.addWaypoint(waypoint)
    }

    private fun getColor(color: Int): Int { return ContextCompat.getColor(requireContext(), color) }

    /**
     * Adds a circle to the map.
     */
    private fun addCircle(radius: Double, strokeColorId: Int, fillColorId: Int, zIndex: Float, latLng: LatLng) {
        map?.addCircle(latLng.let {
            CircleOptions().center(it).radius(radius).strokeColor(getColor(strokeColorId)).fillColor(getColor(
                fillColorId))
                .zIndex(zIndex).strokeWidth((POLYLINE_OUTER_STROKE_WIDTH - POLYLINE_INNER_STROKE_WIDTH) / 2)
        })
    }

    /**
     * Redraws the route upon recreation of the activity or when exercise is complete.
     */
    private fun redrawRoute(routeCompleted: Boolean) {
        val circleRadius = getCircleRadius(routeCompleted)
        var currStatus: ExerciseStatus? = null
        for ((i, wayPoint) in model.waypoints.withIndex()) {
            val currLatLng = wayPoint.latLng
            if (wayPoint.status != currStatus) {
                currStatus = wayPoint.status
                onStatusChanged(currStatus, false, wayPoint, circleRadius)
                if (currStatus == STARTED || currStatus == RESUMED) continue
            }
            if (i == 1 || model.waypoints.size > 1 && model.waypoints.secondLast.status == RESUMED)
                initNewPolyline(model.waypoints[i - 1].latLng, currLatLng)
            else if (i > 1) {
                addPolylinePoint(currLatLng)
            }
        }
    }

    /**
     * Gets circle radius base on map scale - assuring circles always are of the same size.
     */
    private fun getCircleRadius(routeCompleted: Boolean): Double {
        val ongoingRouteMapSize = getMapDiagonalMeters(map)
        val latLngBounds = getLatLngBounds()

        if (routeCompleted && latLngBounds?.southwest != latLngBounds?.northeast) map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(getLatLngBounds(), COMPLETE_ROUTE_PADDING))
        val mapSizeCoefficient = getMapDiagonalMeters(map) / ongoingRouteMapSize
        val circleRadius = if (model.mapCircleRadius > 0) model.mapCircleRadius
                                    else CIRCLE_RADIUS_METERS * mapSizeCoefficient
        if (routeCompleted) model.mapCircleRadius = circleRadius
        return circleRadius
    }

    /**
     * Computes the displayed map diagonal in meters.
     */
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

    /**
     * Get the bounds of the map to display when exercise is completed.
     */
    private fun getLatLngBounds(): LatLngBounds? {
        var latLngBounds : LatLngBounds? = null
        for ((i, wayPoint) in model.waypoints.withIndex()) {
            val latLng = wayPoint.latLng
            latLngBounds = if (i == 0) LatLngBounds(latLng, latLng) else latLngBounds?.including(latLng)
        }
        return latLngBounds
    }

}