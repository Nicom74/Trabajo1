package com.example.trabajo1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView

data class Mensaje(
    val texto: String = "",
    val emisor: String = "",
    val timestamp: Timestamp? = null
)

@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    var mensajes by remember { mutableStateOf<List<Mensaje>>(emptyList()) }
    var textoInput by remember { mutableStateOf("") }
    val db = remember { FirebaseFirestore.getInstance() }

    DisposableEffect(Unit) {
        val listener = db.collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                mensajes = snapshot?.documents?.mapNotNull {
                    it.toObject(Mensaje::class.java)
                } ?: emptyList()
            }

        onDispose { listener.remove() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(mensajes) { msg ->
                BurbujaMensaje(msg)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textoInput,
                onValueChange = { textoInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribí un mensaje...") }
            )
            IconButton(
                onClick = {
                    if (textoInput.isNotBlank()) {
                        enviarMensaje(textoInput)
                        textoInput = ""
                    }
                }
            ) {
                Icon(painterResource(R.drawable.ic_send), contentDescription = "Enviar")
            }
        }
    }
}
fun enviarMensaje(texto: String) {
    val mensaje = hashMapOf(
        "texto" to texto,
        "emisor" to "usuario",
        "timestamp" to FieldValue.serverTimestamp()
    )
    FirebaseFirestore.getInstance()
        .collection("chat")
        .add(mensaje)
}

@Composable
fun BurbujaMensaje(mensaje: Mensaje) {
    val esUsuario = mensaje.emisor == "usuario"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (esUsuario) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (esUsuario) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 260.dp)
        ) {
            Column {
                Text(
                    text = mensaje.texto,
                    color = if (esUsuario) Color.White else Color.Black
                )
                Text(
                    text = formatearHora(mensaje.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (esUsuario) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

fun formatearHora(timestamp: Timestamp?): String {
    if (timestamp == null) return "Enviando..."
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}