package com.example.trabajo1

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
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
    val lifecycleOwner = LocalLifecycleOwner.current

    // Apaga el flash cuando la app pasa a segundo plano o se destruye
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (isFlashOn && cameraId != null) {
                    try {
                        cameraManager.setTorchMode(cameraId, false)
                        isFlashOn = false
                    } catch (e: Exception) {
                        // ignorar, la app ya se está cerrando
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { ctx ->
            val view = LayoutInflater.from(ctx).inflate(R.layout.screen_home, null)
            val btnFlash: Button = view.findViewById(R.id.btn_flash)

            btnFlash.setOnClickListener {
                if (cameraId != null) {
                    try {
                        val newStatus = !isFlashOn
                        cameraManager.setTorchMode(cameraId, newStatus)
                        isFlashOn = newStatus
                        val message = if (isFlashOn) "Prendi el flash" else "Apague el flash"
                        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(ctx, "Flash no disponible", Toast.LENGTH_SHORT).show()
                }
            }
            view
        },
        modifier = modifier,
        update = { view ->
            // Si quisieras cambiar el texto del botón basado en el estado:
            val btnFlash: Button = view.findViewById(R.id.btn_flash)
            btnFlash.text = if (isFlashOn) "Apagar Flash" else "Encender Flash"
        }
    )
}
