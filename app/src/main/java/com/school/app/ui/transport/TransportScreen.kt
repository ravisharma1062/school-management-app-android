package com.school.app.ui.transport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.school.app.R
import com.school.app.ui.common.AppTopBar
import com.school.app.ui.common.CenteredLoading
import com.school.app.ui.common.EmptyState
import com.school.app.ui.common.formatDateTime
import com.school.app.ui.common.stringRes
import com.school.app.viewmodel.TransportViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun TransportScreen(
    onBack: () -> Unit,
    viewModel: TransportViewModel = hiltViewModel(),
) {
    val title = if (viewModel.studentName.isNotBlank()) {
        stringRes(R.string.transport_title_named, viewModel.studentName)
    } else {
        stringRes(R.string.transport_title)
    }

    Scaffold(topBar = { AppTopBar(title, onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                viewModel.loading -> CenteredLoading()
                viewModel.assignment == null ->
                    EmptyState(viewModel.error ?: stringRes(R.string.transport_not_assigned))
                else -> AssignedContent(viewModel)
            }
        }
    }
}

@Composable
private fun AssignedContent(viewModel: TransportViewModel) {
    val assignment = viewModel.assignment ?: return
    val location = viewModel.location

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringRes(R.string.transport_route_stop, assignment.routeName, assignment.stopName),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (location?.latitude == null || location.longitude == null) {
            Text(
                stringRes(R.string.transport_no_location),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            val justNow = stringRes(R.string.transport_just_now)
            Text(
                stringRes(R.string.transport_last_updated, location.updatedAt?.let { formatDateTime(it) } ?: justNow),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .padding(top = 12.dp),
        ) {
            BusMapView(
                stopLat = assignment.stopLatitude,
                stopLng = assignment.stopLongitude,
                busLat = location?.latitude,
                busLng = location?.longitude,
            )
        }
    }
}

@Composable
private fun BusMapView(stopLat: Double, stopLng: Double, busLat: Double?, busLng: Double?) {
    val context = LocalContext.current
    val stopLabel = stringRes(R.string.transport_marker_stop)
    val busLabel = stringRes(R.string.transport_marker_bus)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            val stopPoint = GeoPoint(stopLat, stopLng)
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = stopPoint
                    title = stopLabel
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                },
            )

            val center = if (busLat != null && busLng != null) {
                val busPoint = GeoPoint(busLat, busLng)
                mapView.overlays.add(
                    Marker(mapView).apply {
                        position = busPoint
                        title = busLabel
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    },
                )
                busPoint
            } else {
                stopPoint
            }

            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(center)
            mapView.invalidate()
        },
    )
}
