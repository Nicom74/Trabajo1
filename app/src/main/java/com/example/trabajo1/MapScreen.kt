package com.example.trabajo1

import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.trabajo1.utilidades.fetchWeather
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val gpsActivado = rememberGpsActivado()
    val rootView = remember {
        LayoutInflater.from(context).inflate(R.layout.screen_map, null)
    }
    val mapView = remember { rootView.findViewById<MapView>(R.id.mapView) }
    val saveButton = remember { rootView.findViewById<Button>(R.id.guardar) }
    val ubicacion = remember { rootView.findViewById<TextView>(R.id.text_ubicacion) }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    // Guardamos la última ubicación conocida para que el botón la pueda usar
    var ultimaUbicacion by remember { mutableStateOf<GeoPoint?>(null) }

    if (!gpsActivado) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "GPS desactivado.",
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, sharedPrefs)
        Configuration.getInstance().userAgentValue =
            "MiAppMapaUnica/1.0 (nicolas.martelrodrigo@gmail.com)"
        mapView.setTileSource(TileSourceFactory.MAPNIK)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            calcularCoordenadas(context, mapView) { punto -> ultimaUbicacion = punto }
        }
    }

    LaunchedEffect(Unit) {
        val tienePermiso = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) {
            calcularCoordenadas(context, mapView) { punto ->
                ultimaUbicacion = punto
                ubicacion.text = "Buscando ubicación..."
                scope.launch {
                    val nombreLugar = obtenerNombreLugar(context, punto.latitude, punto.longitude)
                    ubicacion.text = "$nombreLugar\n(${punto.latitude}, ${punto.longitude})"
                }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Listener del botón: se re-crea si cambia ultimaUbicacion, para tener el valor fresco
    LaunchedEffect(ultimaUbicacion) {
        saveButton.setOnClickListener {
            val punto = ultimaUbicacion
            if (punto == null) {
                Toast.makeText(context, "Todavía no se detectó tu ubicación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val datos = hashMapOf(
                "latitud" to punto.latitude,
                "longitud" to punto.longitude,
            )

            db.collection("ubicacion")
                .add(datos)
                .addOnSuccessListener {
                    Toast.makeText(context, "Ubicación guardada", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    //CLIMA
    val api = "f0e7fb319b91b1cad81024b3427ccd68"
    LaunchedEffect(ultimaUbicacion) {
        val punto = ultimaUbicacion
        if (punto != null) {
            fetchWeather(punto.latitude, punto.longitude, api, rootView)
        }
    }

    AndroidView(factory = { rootView }, modifier = modifier.fillMaxSize())
}

private fun calcularCoordenadas(
    context: Context,
    mapView: MapView,
    onUbicacionObtenida: (GeoPoint) -> Unit
) {
    if (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) return

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            val userPoint = GeoPoint(location.latitude, location.longitude)
            onUbicacionObtenida(userPoint)

            mapView.post {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(userPoint)

                val marker = Marker(mapView)
                marker.position = userPoint
                marker.title = "Tu ubicación: ${location.latitude}, ${location.longitude}"
                mapView.overlays.add(marker)
                mapView.invalidate()
            }
        }
    }
}
suspend fun obtenerNombreLugar(context: Context, lat: Double, lon: Double): String {
    if (!Geocoder.isPresent()) return "Ubicación no disponible"

    val geocoder = Geocoder(context, Locale.getDefault())

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: API con callback, no bloqueante
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lon, 1) { direcciones ->
                    cont.resume(formatearDireccion(direcciones.firstOrNull()))
                }
            }
        } else {
            // Android 12 y anteriores: API bloqueante, hay que sacarla del hilo principal
            @Suppress("DEPRECATION")
            val direcciones = geocoder.getFromLocation(lat, lon, 1)
            formatearDireccion(direcciones?.firstOrNull())
        }
    } catch (e: Exception) {
        "No se pudo obtener la dirección"
    }
}

private fun formatearDireccion(direccion: android.location.Address?): String {
    if (direccion == null) return "Ubicación desconocida"
    val ciudad = direccion.locality ?: direccion.subAdminArea
    val provincia = direccion.adminArea
    return listOfNotNull(ciudad, provincia).joinToString(", ")
}

@Composable
fun rememberGpsActivado(): Boolean {
    val context = LocalContext.current
    var gpsActivado by remember { mutableStateOf(gpsEstaActivado(context)) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                gpsActivado = gpsEstaActivado(ctx)
            }
        }
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    return gpsActivado
}

fun gpsEstaActivado(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}
