package com.salman.mymapbox

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.location.LocationCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mapbox.maps.logD
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    Callback<DirectionsResponse>, PermissionsListener {
    lateinit var sourceTV: TextView
    lateinit var destinationTV: TextView
    lateinit var distanceTv: TextView
    lateinit var fabConfirm:FloatingActionButton
    lateinit var fabSearch: FloatingActionButton
    var origin: Point = Point.fromLngLat(90.39, 23.75)
    var destination: Point = Point.fromLngLat(90.37, 23.73)
    var permissionMapbox: PermissionsManager? = null
    var locationCompat: LocationCompat? = null
    lateinit var mapView: MapView
    lateinit var navigation: MapboxNavigation
    var mapboxMap: MapboxMap? = null
    var client: MapboxDirections? = null
    var home: CarmenFeature? = null
    var work: CarmenFeature? = null
    var permissionsManager: PermissionsManager? = null
    var c = 0
    var x = 0
    var distance = 0.0
    var address = ""
    var formatDistance = ""
    var startLocation = ""
    var endLocation = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.app_token))
        setContentView(R.layout.activity_main)
        navigation = MapboxNavigation(this, getString(R.string.app_token))

        sourceTV = findViewById(R.id.sourceTV)
        destinationTV = findViewById(R.id.destinationTV)
        distanceTv = findViewById(R.id.distanceTV)
        fabConfirm = findViewById(R.id.fab_confirm)
        fabSearch = findViewById(R.id.fab_search)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        

    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS, Style.OnStyleLoaded {  style: Style ->

            // function to show users location
            enableLocationComponent(style)

            // function to initialize place autocomplete location search
            initSearch()

            initConfirm()

            // function to add default locations in autocomplete location search
            addUserLocation()

            val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_marker, null)
            val mBitmap = BitmapUtils.getBitmapFromDrawable(drawable)

            // add the symbol layer icon to map for future use
//            style.addImage(SYMBOL_ICON_ID, mBitmap!!)

            // create an empty GeoJson source using the empty feature collection
            setupSource(style)

            // setup a new symbol layer for displaying the searched location's feature coordinates
            setupLayer(style)

            initSource(style)
            
            // setup design layer
            initLayers(style)

            cameraPosition(mapboxMap, origin)

            // show route between two points
//            getRoute(origin, destination)

            mapboxMap.addOnMapClickListener { point: LatLng ->
                var source: LatLng ? = null
                val markerOptions = MarkerOptions()
                val symbolManager = SymbolManager(mapView, mapboxMap, style)
                if (c == 0) { // used to count no. of click on the map
                    origin = Point.fromLngLat(point.longitude, point.latitude)
                    source = point
                    markerOptions.apply {
                        position(point)
                        title("Source")
                    }
                    mapboxMap.addMarker(markerOptions)
                    // fetch location details, place name from lat and long
                    reverseGeocodeFunc(point, c)
                }

                if (c == 1) {
                    destination = Point.fromLngLat(point.longitude, point.latitude)
                    style.addImage(SYMBOL_ICON_ID, mBitmap!!)
                    symbolManager.create(
                        SymbolOptions().withLatLng(LatLng(point.latitude, point.longitude))
                            .withIconImage(SYMBOL_ICON_ID)
                    )
//                    markerOptions.apply {
//                        position(point)
//                        title("Destination")
//                    }
//                    mapboxMap.addMarker(markerOptions)
                    reverseGeocodeFunc(point, c)
                    getRoute(origin, destination)
//                    val d = point.distanceTo(source!!)
                }

                if (c > 1) {
                    c = 0
                    recreate()
                }

                c++
                true
            }

        })
    }

    private fun setupSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(GEO_JSON_SOURCE_LAYER_ID))
    }

    private fun setupLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(SymbolLayer(SYMBOL_LAYER_ID, GEO_JSON_SOURCE_LAYER_ID)
            .withProperties(
                PropertyFactory.iconImage(SYMBOL_ICON_ID),
                PropertyFactory.iconOffset(arrayOf(0f, -8f))
            )
        )
    }

    private fun addUserLocation() {
        home = CarmenFeature.builder()
            .text("Zigatola Post Ofc.")
            .geometry(Point.fromLngLat(90.370586, 23.738185))
            .placeName("Zigatola Post Office Dhaka 1209")
            .id("mapbox-home")
            .properties(JsonObject())
            .build()

        work = CarmenFeature.builder()
            .text("Head Office Com.")
            .geometry(Point.fromLngLat(90.37163, 23.74793))
            .placeName("Head Office Communication")
            .id("mapbox-work")
            .properties(JsonObject())
            .build()
    }

    private fun initConfirm() {
        fabConfirm.setOnClickListener {
            navigationRoute()
        }
    }
    
    private fun initSearch() {
        fabSearch.setOnClickListener {
            val placeOptions = PlaceOptions.builder()
                .backgroundColor(Color.parseColor("#EEEEEE"))
                .limit(10)
                .addInjectedFeature(home)
                .addInjectedFeature(work)
                .build(PlaceOptions.MODE_CARDS)
            val intent = PlaceAutocomplete.IntentBuilder()
                .accessToken(getString(R.string.app_token))
                .placeOptions(placeOptions)
                .build(this)
            startActivityForResult(intent, REQ_CODE)
        }
    }

    private fun reverseGeocodeFunc(point: LatLng, c: Int) {
        val reverseGeocode = MapboxGeocoding
            .builder()
            .accessToken(getString(R.string.app_token))
            .query(Point.fromLngLat(point.longitude, point.latitude))
            .geocodingTypes(GeocodingCriteria.TYPE_POI)
            .build()

        reverseGeocode.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                val results: List<CarmenFeature>? = response.body()?.features()
                if (results!!.size > 0) {
                    // log the first result point
                    var firstResultPoint = results[0].center()
                    val feature: CarmenFeature = results.get(0)
                    // if c == 0 we show source location
                    if (c == 0) {
                        val formattedName = feature.placeName()!!.split(",")
                        val placeName = formattedName.get(0) + ", " + formattedName.get(1)
                        startLocation = placeName
                        Log.d(TAG, "onResponse: Start Location : $startLocation")
//                        startLocation = startLocation.replace(", Dhaka, Bangladesh", ".")
                        sourceTV.setText("Src: $startLocation")
                    }
                    if (c == 1) {
                        val formattedName = feature.placeName()!!.split(",")
                        val placeName = formattedName.get(0) + ", " + formattedName.get(1)
                        endLocation = placeName
                        Timber.d("onResponse: End Location : $endLocation")
                        destinationTV.setText("Des: $endLocation")
                    }

                    Toast.makeText(this@MainActivity, "" + feature.placeName(), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "No result found!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun cameraPosition(mapboxMap: MapboxMap, destination: Point) {
        val position = CameraPosition
            .Builder()
            .target(LatLng(destination.latitude(), destination.longitude()))
            .zoom(14.0)
            .build()
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 4000)
    }

 
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // get an instance of the component
            val locationComponent = mapboxMap!!.locationComponent

            // activate with options
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions
                    .builder(this, loadedMapStyle)
                    .build()
            )

            //enable to make component visible
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationComponent.isLocationComponentEnabled = true

            // if the user move the icon will move
            locationComponent.cameraMode = CameraMode.TRACKING

            locationComponent.renderMode = RenderMode.COMPASS
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

    private fun initSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(ROUTE_SOURCE_ID))
        val iconGeoJsonSource: GeoJsonSource = GeoJsonSource(ICON_SOURCE_ID, FeatureCollection.fromFeatures(
            mutableListOf(
                Feature.fromGeometry(Point.fromLngLat(origin.longitude(), origin.latitude())),
                Feature.fromGeometry(Point.fromLngLat(destination.longitude(), destination.latitude()))
            )))
        loadedMapStyle.addSource(iconGeoJsonSource)
    }

    private fun initLayers(loadedMapStyle: Style) {
        val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)

        // add the LineLayer to the map. This layer will display the directions route
        routeLayer.setProperties(
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineColor(Color.parseColor("#009688"))
        )
        loadedMapStyle.addLayer(routeLayer)

        // add the red marker icon image to the map
//        loadedMapStyle.addImage(RED_PIN_ICON_ID,
//            BitmapUtils.getBitmapFromDrawable(resources.getDrawable(R.drawable.ic_marker))!!
//        )

        // add the red marker icon symbol layer to the map
        loadedMapStyle.addLayer(SymbolLayer(ICON_LAYER_ID, ICON_SOURCE_ID).withProperties(
            PropertyFactory.iconImage(RED_PIN_ICON_ID),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconOffset(arrayOf(0f, - 9f)),
        ))
    }

    private fun getRoute(origin: Point, destination: Point) {
        client = MapboxDirections
            .builder()
            .origin(origin)
            .destination(destination)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .accessToken(getString(R.string.app_token))
            .build()

        client!!.enqueueCall(this)
    }

    private fun navigationRoute() {
        NavigationRoute.builder(this)
            .accessToken(getString(R.string.app_token))
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(object : Callback<DirectionsResponse> {
                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    if (response.body() == null) {
                        Toast.makeText(this@MainActivity, "No results! Error!", Toast.LENGTH_SHORT).show()
                        return
                    } else if (response.body()!!.routes().size < 1) {
                        Toast.makeText(this@MainActivity, "No results!", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // getting route for navigation between source and destination
                    val currentRoute: DirectionsRoute = response.body()!!.routes().get(0)

                    // launching navigation for the route
                    val options = NavigationLauncherOptions
                        .builder()
                        .directionsRoute(currentRoute)
                        .shouldSimulateRoute(true)
                        .build()
                    NavigationLauncher.startNavigation(this@MainActivity, options)

                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error: $t", Toast.LENGTH_SHORT).show()
                }

            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQ_CODE) {
            // retrieve selected location's carmenfeature
            val carmenFeature = PlaceAutocomplete.getPlace(data)
            if (mapboxMap != null) {
                val style = mapboxMap!!.style
                if (style != null) {
                    val source: GeoJsonSource? = style.getSourceAs(GEO_JSON_SOURCE_LAYER_ID)
                    if (source != null) {
                        source.setGeoJson(
                            FeatureCollection.fromFeatures(
                                arrayOf(Feature.fromJson(carmenFeature.toJson()))
                            )
                        )
                    }
                    if (x > 1) {
                        x = 0
                        mapboxMap!!.clear()
                    }
                    // move camera to selected location
                    cameraPosition(mapboxMap!!, carmenFeature.geometry() as Point)
                    addToMap(carmenFeature)
//                    Log.d(TAG, "onActivityResult: ${carmenFeature.placeName()}")
//                    Log.d(TAG, "onActivityResult: lng ${(carmenFeature.geometry() as Point).longitude()}")
//                    Log.d(TAG, "onActivityResult: lat: ${(carmenFeature.geometry() as Point).latitude()}")
                }
            }
        }
        x++
    }

    private fun addToMap(carmenFeature: CarmenFeature) {
        var placeName = carmenFeature.placeName()!!
        if (placeName.contains(",")){
            val formattedName = carmenFeature.placeName()!!.split(",")
            placeName = formattedName[0] + ", " + formattedName[1]
        }
        val point = carmenFeature.geometry() as Point
        val latLng = LatLng(point.latitude(), point.longitude())
        val markerOptions = MarkerOptions()
        if (x == 0) {
            markerOptions.apply {
                position(latLng)
                title("Source")
            }
//            mapboxMap!!.clear()
            mapboxMap!!.addMarker(markerOptions)
            sourceTV.setText("Src: " + placeName)
        }
        if (x == 1) {
            markerOptions.position(latLng)
            markerOptions.title("Destination")
//            mapboxMap!!.clear()
            mapboxMap!!.addMarker(markerOptions)
            destinationTV.setText("Des: ${placeName}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap!!.getStyle { style ->
                enableLocationComponent(style)
            }
        } else {
            finish()
        }
    }

    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
        Log.d(TAG,"Response: ${response.code()}")
        if (response.body() == null) {
            Toast.makeText(this@MainActivity, "No results! Error!", Toast.LENGTH_SHORT).show()
            return
        } else if (response.body()!!.routes().size < 1) {
            Toast.makeText(this@MainActivity, "No results!", Toast.LENGTH_SHORT).show()
            return
        }

        // getting route for direction between source and destination
        val currentRoute: DirectionsRoute = response.body()!!.routes()[0]

        distance = currentRoute.distance()!! / 1000 // route distance in KM
        formatDistance = String.format("%.2f K.M", distance)
        distanceTv.setText("Distance: " + formatDistance)

        if (mapboxMap != null) {
            mapboxMap!!.getStyle { style ->

                // retrieve and update the source designated for showing the directions route
                val source: GeoJsonSource? = style.getSourceAs(ROUTE_SOURCE_ID)
                // create a line string with directions route's geometry
                // and reset the GeoJson source for the route line layer source
                if (source != null) {
                    source.setGeoJson(LineString.fromPolyline(currentRoute.geometry()!!, Constants.PRECISION_6))
                }
            }
        }
    }

    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
        Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
    }


    override fun onDestroy() {
        super.onDestroy()
        navigation.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
        const val ROUTE_LAYER_ID = "router-layer-id"
        const val ROUTE_SOURCE_ID = "route-source-id"
        const val ICON_LAYER_ID = "icon-layer-id"
        const val ICON_SOURCE_ID = "icon-source-id"
        const val RED_PIN_ICON_ID = "red-pin-icon-id"
        const val SYMBOL_ICON_ID = "symbol-icon-id"
        const val SYMBOL_LAYER_ID = "symbol-layer-id"
        const val GEO_JSON_SOURCE_LAYER_ID = "geo-json-source-layer-id"
        const val REQ_CODE = 123
    }



}