package com.example.childtrackerapp.parent.ui.components

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.childtrackerapp.model.ChildLocation
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OSMMapView(
    modifier: Modifier = Modifier,
    locations: Map<String, ChildLocation>,
    defaultZoom: Double = 15.0,
    onMicClick: () -> Unit
) {
    Box(modifier = modifier) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                Configuration.getInstance().load(context, context.getSharedPreferences("osm_prefs", 0))
                MapView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setMultiTouchControls(true)
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                locations.forEach { (_, loc) ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(loc.lat, loc.lng)
                        title = loc.name ?: "Không tên"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        showInfoWindow()
                    }
                    mapView.overlays.add(marker)
                }

                locations.values.firstOrNull()?.let {
                    mapView.controller.setZoom(defaultZoom)
                    mapView.controller.setCenter(GeoPoint(it.lat, it.lng))
                }

                mapView.invalidate()
            }
        )

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp),
            onClick = onMicClick
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "Voice Warning")
        }
    }
}
