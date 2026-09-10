package com.example.trabajo1.utilidades

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

class Flash(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId = cameraManager.cameraIdList.firstOrNull {
        cameraManager.getCameraCharacteristics(it)
            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    var isFlashOn by mutableStateOf(false)
        private set

    fun toggle() {
        if (cameraId == null) {
            Toast.makeText(context, "Flash no disponible", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val newStatus = !isFlashOn
            cameraManager.setTorchMode(cameraId, newStatus)
            isFlashOn = newStatus
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun apagarSiEstaEncendido() {
        if (isFlashOn && cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, false)
                isFlashOn = false
            } catch (e: Exception) { }
        }
    }
}

@Composable
fun rememberFlash(): Flash {
    val context = LocalContext.current
    return remember { Flash(context) }
}