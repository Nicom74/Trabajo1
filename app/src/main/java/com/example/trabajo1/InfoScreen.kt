package com.example.trabajo1

import android.net.Uri
import android.view.LayoutInflater
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun InfoScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val rootView = remember {
        LayoutInflater.from(context).inflate(R.layout.screen_info, null)
    }

    AndroidView(
        factory = {
            val videoTornado = rootView.findViewById<VideoView>(R.id.videoTornado)
            val videoTerremoto = rootView.findViewById<VideoView>(R.id.videoTerremoto)
            val videoIncendio = rootView.findViewById<VideoView>(R.id.videoIncendio)

            val uriTornado = Uri.parse("android.resource://${context.packageName}/${R.raw.tornado}")
            val uriTerremoto = Uri.parse("android.resource://${context.packageName}/${R.raw.terremoto}")
            val uriIncendio = Uri.parse("android.resource://${context.packageName}/${R.raw.incendio}")

            videoTornado.setVideoURI(uriTornado)
            videoTornado.setMediaController(MediaController(context).apply { setAnchorView(videoTornado) })

            videoTerremoto.setVideoURI(uriTerremoto)
            videoTerremoto.setMediaController(MediaController(context).apply { setAnchorView(videoTerremoto) })

            videoIncendio.setVideoURI(uriIncendio)
            videoIncendio.setMediaController(MediaController(context).apply { setAnchorView(videoIncendio) })

            rootView
        },
        modifier = modifier
    )
}