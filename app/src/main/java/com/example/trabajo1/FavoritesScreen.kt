package com.example.trabajo1

import android.Manifest
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.MapView

@Composable
fun FavoritesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Inflamos la vista que contiene el MapView
    val rootView = remember {
        LayoutInflater.from(context).inflate(R.layout.screen_favorites, null)
    }

    val mapView = remember {
        rootView.findViewById<MapView>(R.id.mapView)
    }

    // Gestionamos el ciclo de vida del MapView
    val lifecycleObserver = remember {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    AndroidView(
        factory = {
            // Cuando el mapa esté listo, activamos la ubicación si tenemos permisos
            mapView.getMapAsync { googleMap ->
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    @SuppressLint("MissingPermission")
                    googleMap.isMyLocationEnabled = true
                    googleMap.uiSettings.isMyLocationButtonEnabled = true
                }
            }
            rootView
        },
        modifier = modifier
    )
}