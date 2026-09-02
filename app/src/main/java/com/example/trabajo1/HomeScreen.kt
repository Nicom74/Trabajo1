package com.example.trabajo1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember {
        cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val audioRecorder = remember { AudioRecorderHelper(context) }

    val cameraXHelper = remember { CameraXHelper(context, lifecycleOwner) }
    var isRecordingVideo by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }
    val preview = remember {
        androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
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
                if (isFlashOn && cameraId != null) {
                    try {
                        cameraManager.setTorchMode(cameraId, false)
                        isFlashOn = false
                    } catch (e: Exception) { }
                }
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
            val btnStart: Button = view.findViewById(R.id.btn_start_audio)
            val btnStop: Button = view.findViewById(R.id.btn_end_audio)
            val btnFrontal: Button = view.findViewById(R.id.btn_frontal)
            val btnSelfie: Button = view.findViewById(R.id.btn_selfie)
            val btnStartVideo: Button = view.findViewById(R.id.btn_start_video)
            val btnStopVideo: Button = view.findViewById(R.id.btn_end_video)

            val previewContainer: FrameLayout = view.findViewById(R.id.camera_preview_container)
            previewContainer.addView(previewView)

            btnFlash.setOnClickListener {
                if (cameraId != null) {
                    try {
                        val newStatus = !isFlashOn
                        cameraManager.setTorchMode(cameraId, newStatus)
                        isFlashOn = newStatus
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(ctx, "Flash no disponible", Toast.LENGTH_SHORT).show()
                }
            }

            btnStart.setOnClickListener {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    audioRecorder.startRecording()
                    isRecording = true
                } else {
                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            btnStop.setOnClickListener {
                audioRecorder.stopRecording()
                isRecording = false
            }

            btnFrontal.setOnClickListener { cameraXHelper.startFrontCamera(preview) }
            btnSelfie.setOnClickListener { cameraXHelper.startSelfiePortraitMode(preview) }
            btnStartVideo.setOnClickListener { cameraXHelper.startRecording { success, message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                isRecordingVideo = false
            }
                isRecordingVideo = true
            }
            btnStopVideo.setOnClickListener { cameraXHelper.stopRecording() }

            view
        },
        modifier = modifier,
        update = { view ->
            val btnFlash: Button = view.findViewById(R.id.btn_flash)
            val btnStart: Button = view.findViewById(R.id.btn_start_audio)
            val btnStop: Button = view.findViewById(R.id.btn_end_audio)
            val btnStartVideo: Button = view.findViewById(R.id.btn_start_video)
            val btnStopVideo: Button = view.findViewById(R.id.btn_end_video)

            btnFlash.text = if (isFlashOn) "Apagar Flash" else "Encender Flash"
            btnStart.isEnabled = !isRecording
            btnStop.isEnabled = isRecording
            btnStartVideo.isEnabled = !isRecordingVideo
            btnStopVideo.isEnabled = isRecordingVideo
        }
    )
}
