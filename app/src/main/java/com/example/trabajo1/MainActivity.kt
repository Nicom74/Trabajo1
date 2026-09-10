package com.example.trabajo1

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.content.ContextCompat
import com.example.trabajo1.ui.theme.Trabajo1Theme
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.firestore
import android.os.Vibrator
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Manejar respuesta si es necesario
        }

        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        setContent {
            Trabajo1Theme {
                ApnavClaseApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun ApnavClaseApp() {
    var catastrofeActiva by remember { mutableStateOf(false) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val context = LocalContext.current

    val speechRecognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Decí un comando...")
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.get(0) ?: ""

            currentDestination = procesarComando(spokenText, context) ?: currentDestination
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechLauncher.launch(speechRecognizerIntent)
        } else {
            Toast.makeText(context, "Se necesita el permiso de micrófono", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        escucharCatastrofes(miLat = -35.65, miLon = -63.75) { tipo, lat, lon ->
            catastrofeActiva = true
            activarAlarma(context)
            guardarEnHistorial(tipo, lat.toString(), lon.toString())
        }
    }

    LaunchedEffect(catastrofeActiva) {
        if (catastrofeActiva) {
            delay(8000) // 8 segundos, ajustá a gusto
            catastrofeActiva = false
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = {
                            Icon(painterResource(it.icon), contentDescription = it.label)
                        },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "SalvameYA",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                simularCatastrofe("Incendio", -35.651, -63.751)
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_info), // usá un ícono que ya tengas, o Icons.Default.Warning
                                    contentDescription = "Simular catástrofe"
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (currentDestination != AppDestinations.CHAT) {
                        FloatingActionButton(onClick = {
                            val permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            if (permiso == PackageManager.PERMISSION_GRANTED) {
                                speechLauncher.launch(speechRecognizerIntent)
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) {
                            Icon(painterResource(R.drawable.ic_voice), contentDescription = "Comando de voz")
                        }
                    }
                }
            ) { innerPadding ->
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen(modifier = Modifier.padding(innerPadding))
                    AppDestinations.MAPA -> MapScreen(modifier = Modifier.padding(innerPadding))
                    AppDestinations.INFO -> InfoScreen(modifier = Modifier.padding(innerPadding))
                    AppDestinations.CHAT -> ChatScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }

        EfectoAlarmaVisual(activo = catastrofeActiva)
    }
}


fun procesarComando(texto: String, context: Context): AppDestinations? {
    val comando = texto.lowercase()
    return when {
        comando.contains("mapa") -> AppDestinations.MAPA
        comando.contains("info") -> AppDestinations.INFO
        comando.contains("chat") -> AppDestinations.CHAT
        comando.contains("inicio") || comando.contains("home") -> AppDestinations.HOME
        else -> {
            Toast.makeText(context, "No reconocí: $texto", Toast.LENGTH_SHORT).show()
            null
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("", R.drawable.ic_home),
    MAPA("", R.drawable.ic_map),
    INFO("", R.drawable.ic_info),
    CHAT("", R.drawable.ic_message),
}

fun simularCatastrofe(tipo: String, lat: Double, lon: Double) {
    val evento = hashMapOf(
        "tipo" to tipo,
        "timestamp" to com.google.firebase.Timestamp.now(),
        "latitud" to lat.toString(),
        "longitud" to lon.toString()
    )
    Firebase.firestore.collection("catastrofes")
        .add(evento)
}


fun escucharCatastrofes(
    miLat: Double,
    miLon: Double,
    onCatastrofeDetectada: (tipo: String, lat: Double, lon: Double) -> Unit
) {
    Firebase.firestore.collection("catastrofes")
        .whereGreaterThan("timestamp", com.google.firebase.Timestamp.now())
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Catastrofe", "Error escuchando", error)
                return@addSnapshotListener
            }

            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.ADDED) {
                    val doc = change.document
                    val lat = doc.getString("latitud")?.toDoubleOrNull() ?: return@forEach
                    val lon = doc.getString("longitud")?.toDoubleOrNull() ?: return@forEach
                    val tipo = doc.getString("tipo") ?: "Catástrofe"

                    val distancia = calcularDistancia(miLat, miLon, lat, lon)
                    if (distancia < 5.0) {
                        onCatastrofeDetectada(tipo, lat, lon)
                    }
                }
            }
        }
}


fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val resultado = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, resultado)
    return resultado[0] / 1000.0 // metros a km
}

fun activarAlarma(context: Context) {
    // Vibración
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val patron = longArrayOf(0, 500, 200, 500, 200, 500) // pausa, vibra, pausa, vibra...
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(patron, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(patron, -1)
    }

    // Sonido (usando un raw o el sonido de alarma del sistema)
    val mediaPlayer = MediaPlayer.create(context, R.raw.alarma)
    mediaPlayer.start()
    mediaPlayer.setOnCompletionListener { it.release() } // liberar memoria al terminar
}

@Composable
fun EfectoAlarmaVisual(activo: Boolean) {
    if (!activo) return

    val colorTransition = rememberInfiniteTransition(label = "alarma")
    val color by colorTransition.animateColor(
        initialValue = Color.Red,
        targetValue = Color.Yellow,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorAlarma"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color.copy(alpha = 0.4f)) // semi-transparente para que se vea el contenido debajo
    )
}

fun guardarEnHistorial(tipo: String, lat: String, lon: String) {
    val evento = hashMapOf(
        "tipo" to tipo,
        "region" to "General Pico", // o derivalo de lat/lon si tenés lógica de zonas
        "fechaInicio" to com.google.firebase.Timestamp.now(),
        "latitud" to lat,
        "longitud" to lon,
        "descripcion" to "Detectada automáticamente por la app"
    )
    Firebase.firestore.collection("historial_catastrofes")
        .add(evento)
}

