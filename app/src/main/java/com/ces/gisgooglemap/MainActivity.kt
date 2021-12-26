package com.ces.gisgooglemap

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.data.geojson.GeoJsonLayer
import android.os.Bundle
import android.util.Log
import com.ces.gisgooglemap.utility.UTM2Deg
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.ces.gisgooglemap.utility.Deg2UTM
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle
import com.google.maps.android.data.geojson.GeoJsonLayer.GeoJsonOnFeatureClickListener
import org.json.JSONException
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.Volley
import android.widget.Toast
import com.android.volley.Request
import com.google.android.gms.maps.model.*
import com.google.maps.android.data.geojson.GeoJsonPointStyle

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var googleMap: GoogleMap? = null
    var layer: GeoJsonLayer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_activity)
        initilizeMap()
//        val deg = UTM2Deg(562987.0, 3614094.0, 39)
    }

    private fun initilizeMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    public override fun onResume() {
        super.onResume()
    }

    private fun setUpMap() {
        val wmsTileProvider: TileProvider = TileProviderFactory.getWMSTileProviderByName(
            "{YOUR-LAYERNAME}"
        )
        googleMap!!.addTileOverlay(TileOverlayOptions().tileProvider(wmsTileProvider))

        // to satellite so we can see the WMS overlay.
        googleMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        if (this.googleMap != null) {
            setUpMap()
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(32.643759, 51.837594),
                    12f
                )
            )
            googleMap.setOnCameraIdleListener {
                val bounds = googleMap.projection.visibleRegion.latLngBounds
                val a = bounds.northeast.latitude
                val b = bounds.northeast.longitude
                val c = bounds.southwest.latitude
                val d = bounds.southwest.longitude
                val a1 = Deg2UTM(a, b)
                val a2 = Deg2UTM(c, d)
                val u1 = UTM(a1.Easting, a1.Northing)
                val u2 = UTM(a2.Easting, a2.Northing)
                val zoom = googleMap.cameraPosition.zoom
                val url = "{YOUR-URL?}" +
                        "service={service}&version={version}&request={request}" +
                        "&typeName={typeName}" +
                        "&outputFormat={outputFormat}&" +
                        "CQL_FILTER={CQL_FILTER}," + u1.x + "," + u1.y + "," + u2.x + "," + u2.y + "%29%29"
                if (zoom >= 21) {
                    getGeoJson(url)
                } else if (layer != null) {
                    layer!!.removeLayerFromMap()
                }
            }
        }
    }

    fun getGeoJson(url: String?) {
        val request = StringRequest(
            Request.Method.GET, url, { response ->
                try {
                    if (layer != null) {
                        layer!!.removeLayerFromMap()
                        layer = null
                    }
                    val `object` = JSONObject(response)
                    for (i in 0 until `object`.getJSONArray("{your-array}").length()) {
                        val gr = `object`.getJSONArray("{your-array}").getJSONObject(i)
                        val geo = gr.getJSONObject("{your-object}")
                        if (geo.getJSONArray("{your-array}").length() == 0) continue
                        val cordin = geo.getJSONArray("{your-array}").getJSONArray(0)
                        for (j in 0 until cordin.length()) {
                            val x = cordin.getJSONArray(j).getDouble(0)
                            val y = cordin.getJSONArray(j).getDouble(1)
                            //                            LatLng latLng = ToLatLon(x,y,"39N");
                            val utm2Deg = UTM2Deg(x, y, 39)
                            Log.i("lastt", utm2Deg.latitude.toString() + "," + utm2Deg.longitude)
                            cordin.getJSONArray(j).put(0, utm2Deg.longitude)
                            cordin.getJSONArray(j).put(1, utm2Deg.latitude)
                        }
                    }
                    layer = GeoJsonLayer(googleMap, `object`)
                    for (f in layer!!.features) {
                        val style = GeoJsonPolygonStyle()

//                        if (f.hasProperty("status"))
//                            Log.i("statusssas","1");
                        if (f.getProperty("status") != null) if (f.hasProperty("status") && f.getProperty(
                                "status"
                            ) == "3"
                        ) {
                            style.fillColor = Color.parseColor("#A52A2A") //A52A2A
                        } else {
                            style.fillColor = Color.parseColor("#FDA8AA") //FDA8AA
                        }
                        f.polygonStyle = style
                    }
                    layer!!.addLayerToMap()
                    layer!!.setOnFeatureClickListener(GeoJsonOnFeatureClickListener { })
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }) { }
        request.retryPolicy = DefaultRetryPolicy(
            18000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        Volley.newRequestQueue(applicationContext).add<String>(request)
    }

    private fun addGeoJsonLayerToMap(layer: GeoJsonLayer) {

//        addColorsToMarkers(layer);
        layer.addLayerToMap()

//        getMap().moveCamera(CameraUpdateFactory.newLatLng(new LatLng(31.4118,-103.5355)));
        // Demonstrate receiving features via GeoJsonLayer clicks.
        layer.setOnFeatureClickListener(GeoJsonOnFeatureClickListener { feature ->
            Toast.makeText(
                this@MainActivity,
                "Feature clicked: " + feature.getProperty("title"),
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    internal inner class UTM(var x: Double, var y: Double)

    private fun addColorsToMarkers(layer: GeoJsonLayer) {
        // Iterate over all the features stored in the layer
        for (feature in layer.features) {
            // Check if the magnitude property exists
            // Get the icon for the feature
            val pointIcon = BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_CYAN)
            Log.i("fiuasd", "1")
            // Create a new point style
            val pointStyle = GeoJsonPointStyle()

            // Set options for the point style
            pointStyle.icon = pointIcon
            pointStyle.title = "Magnitude of "
            pointStyle.snippet = "Earthquake occured "

            // Assign the point style to the feature
            feature.pointStyle = pointStyle
        }
    }
}