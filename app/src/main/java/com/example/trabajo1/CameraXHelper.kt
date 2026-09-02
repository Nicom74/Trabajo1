package com.example.trabajo1

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.Date
import java.util.Locale

class CameraXHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    // Arranca la cámara frontal, sin efectos, y deja todo listo para grabar
    fun startFrontCamera(previewUseCase: androidx.camera.core.Preview) {
        bindCamera(
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
            previewUseCase = previewUseCase,
            useExtensions = false
        )
    }

    // Intenta modo retrato (BOKEH) en frontal; si el device no lo soporta, cae a frontal normal
    fun startSelfiePortraitMode(previewUseCase: androidx.camera.core.Preview) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val extensionsFuture = ExtensionsManager.getInstanceAsync(context, provider)
            extensionsFuture.addListener({
                val extensionsManager = extensionsFuture.get()
                val baseSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val isBokehSupported = extensionsManager.isExtensionAvailable(
                    baseSelector, ExtensionMode.BOKEH
                )

                val finalSelector = if (isBokehSupported) {
                    extensionsManager.getExtensionEnabledCameraSelector(
                        baseSelector, ExtensionMode.BOKEH
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Este dispositivo no soporta modo retrato, grabando en frontal normal",
                        Toast.LENGTH_SHORT
                    ).show()
                    baseSelector
                }

                bindCamera(finalSelector, previewUseCase, useExtensions = isBokehSupported, providerAlreadyFetched = provider)
            }, ContextCompat.getMainExecutor(context))
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera(
        cameraSelector: CameraSelector,
        previewUseCase: androidx.camera.core.Preview,
        useExtensions: Boolean,
        providerAlreadyFetched: ProcessCameraProvider? = null
    ) {
        val bind = { provider: ProcessCameraProvider ->
            cameraProvider = provider
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewUseCase,
                    videoCapture
                )
            } catch (e: Exception) {
                Log.e("CameraXHelper", "Fallo al bindear cámara", e)
                Toast.makeText(context, "Error al iniciar cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        if (providerAlreadyFetched != null) {
            bind(providerAlreadyFetched)
        } else {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({ bind(providerFuture.get()) }, ContextCompat.getMainExecutor(context))
        }
    }

    fun startRecording(onFinished: (Boolean, String) -> Unit) {
        val capture = videoCapture ?: return

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            onFinished(false, "Falta permiso de audio")
            return
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "VIDEO_$timeStamp.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        activeRecording = capture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            onFinished(true, "Video guardado correctamente")
                        } else {
                            onFinished(false, "Error al guardar: ${event.error}")
                        }
                    }
                    else -> {}
                }
            }
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun releaseCamera() {
        cameraProvider?.unbindAll()
    }
}