package com.jeppe.radm.ui.map

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jeppe.radm.domain.analysis.ActivityAnalysisInteractionSnapshot
import com.jeppe.radm.domain.analysis.AnalysisRouteCoordinate
import com.jeppe.radm.domain.analysis.AnalysisRouteSegment
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.Locale

object ActivityRouteMapTestTags {
    const val MAP = "analysis_route_map"
    const val STATUS = "analysis_route_map_status"
    const val SEGMENTS = "analysis_route_map_segments"
    const val RANGE = "analysis_route_map_range"
    const val SELECTION = "analysis_route_map_selection"
    const val FULL_ROUTE = "analysis_route_map_full_route"
}

object ActivityRouteMapLayers {
    const val FULL_ROUTE = "radm-route-full-layer"
    const val RANGE_ROUTE = "radm-route-range-layer"
    const val SELECTION = "radm-route-selection-layer"
}

enum class BasemapStatus {
    LOADING,
    AVAILABLE,
    UNAVAILABLE,
}

/** OpenFreeMap configuration is isolated from activity identity and measurements. */
object OpenFreeMapConfiguration {
    const val STYLE_URI = "https://tiles.openfreemap.org/styles/liberty"
}

@Composable
fun ActivityRouteMap(
    interaction: ActivityAnalysisInteractionSnapshot,
    onSelectRoute: (latitude: Double, longitude: Double, toleranceMetres: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current.density
    val networkAvailable = rememberNetworkAvailable(context)
    val currentOnSelectRoute by rememberUpdatedState(onSelectRoute)
    var basemapStatus by remember { mutableStateOf(BasemapStatus.LOADING) }
    val controller = remember {
        MapLibreRouteController(
            context = context,
            density = density,
            onSelectRoute = { coordinate, tolerance ->
                currentOnSelectRoute(coordinate.latitude, coordinate.longitude, tolerance)
            },
            onBasemapStatus = { basemapStatus = it },
        )
    }

    LaunchedEffect(networkAvailable) {
        controller.setNetworkAvailable(networkAvailable)
    }
    LaunchedEffect(interaction) {
        controller.render(interaction)
    }
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event -> controller.onLifecycleEvent(event) }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.destroy()
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Route map", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when (basemapStatus) {
                            BasemapStatus.LOADING -> "Basemap: loading OpenFreeMap"
                            BasemapStatus.AVAILABLE -> "Basemap: OpenFreeMap"
                            BasemapStatus.UNAVAILABLE ->
                                "Basemap unavailable — local recorded route remains available"
                        },
                        modifier = Modifier.testTag(ActivityRouteMapTestTags.STATUS),
                        color = if (basemapStatus == BasemapStatus.UNAVAILABLE) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                TextButton(
                    onClick = controller::showFullRoute,
                    modifier = Modifier.testTag(ActivityRouteMapTestTags.FULL_ROUTE),
                ) { Text("Full route") }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            ) {
                AndroidView(
                    factory = { controller.createView() },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(ActivityRouteMapTestTags.MAP),
                )
            }
            Text(
                "Map route: ${interaction.route.fullSegments.size} separate " +
                    if (interaction.route.fullSegments.size == 1) "segment" else "segments",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(ActivityRouteMapTestTags.SEGMENTS),
            )
            Text(
                "Highlighted range: ${interaction.route.highlightedSegments.sumOf { it.points.size }} route points",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(ActivityRouteMapTestTags.RANGE),
            )
            Text(
                "Map selection: ${formatDuration(interaction.state.selectedPosition.activeElapsedTime.value)}" +
                    if (interaction.route.selectedCoordinate == null) " · no geographical position" else "",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .testTag(ActivityRouteMapTestTags.SELECTION),
            )
        }
    }
}

@Composable
private fun rememberNetworkAvailable(context: Context): Boolean {
    val connectivityManager = remember(context) {
        context.getSystemService(ConnectivityManager::class.java)
    }
    fun isAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    var available by remember(connectivityManager) { mutableStateOf(isAvailable()) }
    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                available = isAvailable()
            }

            override fun onLost(network: Network) {
                available = isAvailable()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                available = isAvailable()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }
    return available
}

private class MapLibreRouteController(
    private val context: Context,
    private val density: Float,
    private val onSelectRoute: (AnalysisRouteCoordinate, Double) -> Unit,
    private val onBasemapStatus: (BasemapStatus) -> Unit,
) {
    private var mapView: MapView? = null
    private var map: MapLibreMap? = null
    private var interaction: ActivityAnalysisInteractionSnapshot? = null
    private var networkAvailable: Boolean? = null
    private var styleReady = false
    private var usingRemoteStyle = false
    private var cameraInitialized = false
    private var started = false
    private var resumed = false
    private var destroyed = false
    private var styleRequestId = 0L

    fun createView(): MapView {
        MapLibre.getInstance(context.applicationContext)
        return MapView(context).also { view ->
            mapView = view
            view.onCreate(Bundle())
            view.addOnDidFailLoadingMapListener {
                if (usingRemoteStyle) loadLocalRouteStyle()
            }
            view.getMapAsync { readyMap ->
                map = readyMap
                readyMap.uiSettings.apply {
                    isScrollGesturesEnabled = true
                    isZoomGesturesEnabled = true
                    isRotateGesturesEnabled = false
                    isTiltGesturesEnabled = false
                }
                readyMap.addOnMapClickListener { latLng ->
                    val tolerance = readyMap.projection
                        .getMetersPerPixelAtLatitude(latLng.latitude) * TOUCH_TOLERANCE_DP * density
                    onSelectRoute(
                        AnalysisRouteCoordinate(latLng.latitude, latLng.longitude),
                        tolerance,
                    )
                    true
                }
                loadRequestedStyle()
            }
        }
    }

    fun onLifecycleEvent(event: Lifecycle.Event) {
        val view = mapView ?: return
        when (event) {
            Lifecycle.Event.ON_START -> if (!started) {
                view.onStart()
                started = true
            }
            Lifecycle.Event.ON_RESUME -> if (!resumed) {
                view.onResume()
                resumed = true
            }
            Lifecycle.Event.ON_PAUSE -> if (resumed) {
                view.onPause()
                resumed = false
            }
            Lifecycle.Event.ON_STOP -> if (started) {
                view.onStop()
                started = false
            }
            else -> Unit
        }
    }

    fun setNetworkAvailable(available: Boolean) {
        if (networkAvailable == available) return
        networkAvailable = available
        if (map != null) loadRequestedStyle()
    }

    fun render(snapshot: ActivityAnalysisInteractionSnapshot) {
        interaction = snapshot
        if (styleReady) updateRouteSources()
    }

    fun showFullRoute() {
        val points = interaction?.route?.fullSegments.orEmpty().flatMap { it.points }
        val readyMap = map ?: return
        if (points.isEmpty()) return
        val bounds = LatLngBounds.Builder()
            .includes(points.map { LatLng(it.coordinate.latitude, it.coordinate.longitude) })
            .build()
        readyMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, CAMERA_PADDING_PX))
        cameraInitialized = true
    }

    fun destroy() {
        if (destroyed) return
        mapView?.let { view ->
            if (resumed) view.onPause()
            if (started) view.onStop()
            view.onDestroy()
        }
        resumed = false
        started = false
        destroyed = true
    }

    private fun loadRequestedStyle() {
        if (networkAvailable == true) loadRemoteStyle() else loadLocalRouteStyle()
    }

    private fun loadRemoteStyle() {
        val readyMap = map ?: return
        val requestId = ++styleRequestId
        styleReady = false
        usingRemoteStyle = true
        onBasemapStatus(BasemapStatus.LOADING)
        readyMap.setStyle(OpenFreeMapConfiguration.STYLE_URI) {
            if (destroyed || requestId != styleRequestId) return@setStyle
            usingRemoteStyle = true
            styleReady = true
            onBasemapStatus(BasemapStatus.AVAILABLE)
            installRouteLayers(it)
        }
    }

    private fun loadLocalRouteStyle() {
        val readyMap = map ?: return
        if (!usingRemoteStyle && styleReady) {
            onBasemapStatus(BasemapStatus.UNAVAILABLE)
            return
        }
        val requestId = ++styleRequestId
        styleReady = false
        usingRemoteStyle = false
        onBasemapStatus(BasemapStatus.UNAVAILABLE)
        readyMap.setStyle(Style.Builder().fromJson(LOCAL_ROUTE_STYLE_JSON)) {
            if (destroyed || requestId != styleRequestId) return@setStyle
            styleReady = true
            installRouteLayers(it)
        }
    }

    private fun installRouteLayers(style: Style) {
        val snapshot = interaction ?: return
        style.addSource(GeoJsonSource(FULL_ROUTE_SOURCE, routeFeatures(snapshot.route.fullSegments)))
        style.addLayer(
            LineLayer(ActivityRouteMapLayers.FULL_ROUTE, FULL_ROUTE_SOURCE).withProperties(
                lineColor(Color.rgb(55, 71, 79)),
                lineWidth(5f),
                lineOpacity(0.48f),
            ),
        )
        style.addSource(GeoJsonSource(RANGE_ROUTE_SOURCE, routeFeatures(snapshot.route.highlightedSegments)))
        style.addLayer(
            LineLayer(ActivityRouteMapLayers.RANGE_ROUTE, RANGE_ROUTE_SOURCE).withProperties(
                lineColor(Color.rgb(0, 105, 92)),
                lineWidth(8f),
                lineOpacity(0.95f),
            ),
        )
        style.addSource(GeoJsonSource(ENDPOINT_SOURCE, endpointFeatures(snapshot.route.fullSegments)))
        style.addLayer(
            CircleLayer(ENDPOINT_LAYER, ENDPOINT_SOURCE).withProperties(
                circleRadius(6f),
                circleColor(Color.WHITE),
                circleStrokeWidth(3f),
                circleStrokeColor(Color.rgb(0, 77, 64)),
            ),
        )
        style.addSource(GeoJsonSource(SELECTION_SOURCE, selectionFeature(snapshot)))
        style.addLayer(
            CircleLayer(ActivityRouteMapLayers.SELECTION, SELECTION_SOURCE).withProperties(
                circleRadius(9f),
                circleColor(Color.rgb(255, 193, 7)),
                circleStrokeWidth(3f),
                circleStrokeColor(Color.rgb(35, 35, 35)),
            ),
        )
        updateRouteSources()
        if (!cameraInitialized) showFullRoute()
    }

    private fun updateRouteSources() {
        val readyMap = map ?: return
        val snapshot = interaction ?: return
        val style = readyMap.style ?: return
        style.getSourceAs<GeoJsonSource>(RANGE_ROUTE_SOURCE)
            ?.setGeoJson(routeFeatures(snapshot.route.highlightedSegments))
        style.getSourceAs<GeoJsonSource>(SELECTION_SOURCE)
            ?.setGeoJson(selectionFeature(snapshot))
    }
}

private fun routeFeatures(segments: List<AnalysisRouteSegment>): FeatureCollection =
    FeatureCollection.fromFeatures(
        segments.mapNotNull { segment ->
            val points = segment.points.map { point ->
                Point.fromLngLat(point.coordinate.longitude, point.coordinate.latitude)
            }
            points.takeIf { it.size >= 2 }?.let(LineString::fromLngLats)?.let(Feature::fromGeometry)
        },
    )

private fun endpointFeatures(segments: List<AnalysisRouteSegment>): FeatureCollection {
    val points = segments.flatMap { segment ->
        listOfNotNull(segment.points.firstOrNull(), segment.points.lastOrNull())
    }.distinctBy { it.coordinate }
    return FeatureCollection.fromFeatures(
        points.map { Feature.fromGeometry(Point.fromLngLat(it.coordinate.longitude, it.coordinate.latitude)) },
    )
}

private fun selectionFeature(snapshot: ActivityAnalysisInteractionSnapshot): FeatureCollection =
    FeatureCollection.fromFeatures(
        listOfNotNull(
            snapshot.route.selectedCoordinate?.let { coordinate ->
                Feature.fromGeometry(Point.fromLngLat(coordinate.longitude, coordinate.latitude))
            },
        ),
    )

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1_000L
    return String.format(
        Locale.getDefault(),
        "%d:%02d:%02d",
        seconds / 3_600L,
        (seconds % 3_600L) / 60L,
        seconds % 60L,
    )
}

private const val TOUCH_TOLERANCE_DP = 48.0
private const val CAMERA_PADDING_PX = 72
private const val FULL_ROUTE_SOURCE = "radm-route-full-source"
private const val RANGE_ROUTE_SOURCE = "radm-route-range-source"
private const val ENDPOINT_SOURCE = "radm-route-endpoints-source"
private const val ENDPOINT_LAYER = "radm-route-endpoints-layer"
private const val SELECTION_SOURCE = "radm-route-selection-source"
private const val LOCAL_ROUTE_STYLE_JSON = """
    {
      "version": 8,
      "name": "RADM local route fallback",
      "sources": {},
      "layers": [
        {
          "id": "background",
          "type": "background",
          "paint": { "background-color": "#ECEFF1" }
        }
      ]
    }
"""
