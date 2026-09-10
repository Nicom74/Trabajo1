package com.example.trabajo1

import android.Manifest
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.trabajo1.utilidades.Audio
import com.example.trabajo1.utilidades.Camara
import com.example.trabajo1.utilidades.ContactosScreen
import com.example.trabajo1.utilidades.HistorialEmbebido
import com.example.trabajo1.utilidades.rememberBatteryEstado
import com.example.trabajo1.utilidades.rememberFlash

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val flash = rememberFlash()
    val bateria = rememberBatteryEstado()

    var isRecording by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }

    val audioRecorder = remember { Audio(context) }
    val cameraXHelper = remember { Camara(context, lifecycleOwner) }

    val previewView = remember { PreviewView(context) }
    val preview = remember {
        Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            audioRecorder.startRecording()
            isRecording = true
        } else {
            Toast.makeText(context, "Permiso de audio denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val camGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!camGranted || !audioGranted) {
            Toast.makeText(context, "Se necesitan permisos de cámara y audio", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                flash.apagarSiEstaEncendido()
                if (isRecording) {
                    audioRecorder.stopRecording()
                    isRecording = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        onDispose { cameraXHelper.releaseCamera() }
    }

    AndroidView(
        factory = { ctx ->
            val view = LayoutInflater.from(ctx).inflate(R.layout.screen_home, null)
            val btnFlash: Button = view.findViewById(R.id.btn_flash)
            val btnStartAudio: Button = view.findViewById(R.id.btn_start_audio)
            val btnStopAudio: Button = view.findViewById(R.id.btn_end_audio)
            val btnFrontal: Button = view.findViewById(R.id.btn_frontal)
            val btnSelfie: Button = view.findViewById(R.id.btn_selfie)
            val btnStartVideo: Button = view.findViewById(R.id.btn_start_video)
            val btnStopVideo: Button = view.findViewById(R.id.btn_end_video)
            val previewContainer: FrameLayout = view.findViewById(R.id.camera_preview_container)


            previewContainer.addView(previewView)
            btnFlash.setOnClickListener { flash.toggle() }

            btnStartAudio.setOnClickListener {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    audioRecorder.startRecording()
                    isRecording = true
                } else {
                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            btnStopAudio.setOnClickListener {
                audioRecorder.stopRecording()
                isRecording = false
            }

            btnFrontal.setOnClickListener { cameraXHelper.startFrontCamera(preview) }
            btnSelfie.setOnClickListener { cameraXHelper.startSelfiePortraitMode(preview) }
            btnStartVideo.setOnClickListener {
                cameraXHelper.startRecording { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    isRecordingVideo = false
                }
                isRecordingVideo = true
            }
            btnStopVideo.setOnClickListener { cameraXHelper.stopRecording() }

            val composeHistorial: ComposeView = view.findViewById(R.id.compose_historial)
            composeHistorial.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeHistorial.setContent {
                MaterialTheme {
                    HistorialEmbebido()
                }
            }

            val composeContactos: ComposeView = view.findViewById(R.id.compose_contactos)
            composeContactos.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeContactos.setContent {
                MaterialTheme {
                    ContactosScreen()
                }
            }

            view
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            val bateriaBarra: ProgressBar = view.findViewById(R.id.progressBar_bateria)
            val bateriaTexto: TextView = view.findViewById(R.id.text_bateria)
            val btnFlash: Button = view.findViewById(R.id.btn_flash)
            val btnStartAudio: Button = view.findViewById(R.id.btn_start_audio)
            val btnStopAudio: Button = view.findViewById(R.id.btn_end_audio)
            val btnStartVideo: Button = view.findViewById(R.id.btn_start_video)
            val btnStopVideo: Button = view.findViewById(R.id.btn_end_video)

            bateriaBarra.max = 100
            bateriaBarra.progress = bateria.porcentaje
            bateriaTexto.text = bateria.textoEstado

            btnFlash.text = if (flash.isFlashOn) "Apagar Flash" else "Encender Flash"
            btnStartAudio.isEnabled = !isRecording
            btnStopAudio.isEnabled = isRecording
            btnStartVideo.isEnabled = !isRecordingVideo
            btnStopVideo.isEnabled = isRecordingVideo
        }
    )
}