package com.example.trabajo1

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.screen_profile, null)
        },
        modifier = modifier
    )
}