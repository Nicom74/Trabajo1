package com.example.trabajo1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

data class Contacto(
    val nombre: String = "",
    val numero: String = ""
)

@Composable
fun ContactosScreen(modifier: Modifier = Modifier) {
    val db = remember { FirebaseFirestore.getInstance() }
    var contactos by remember { mutableStateOf<List<Contacto>>(emptyList()) }
    val context = LocalContext.current

    // Guardamos el numero pendiente de llamar, por si hay que pedir permiso primero
    var numeroPendiente by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && numeroPendiente != null) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$numeroPendiente")
            }
            context.startActivity(intent)
        }
        numeroPendiente = null
    }

    LaunchedEffect(Unit) {
        db.collection("contactos")
            .get()
            .addOnSuccessListener { result ->
                contactos = result.map { doc ->
                    Contacto(
                        nombre = doc.getString("nombre") ?: "",
                        numero = doc.getString("numero") ?: ""
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error trayendo contactos", e)
            }
    }

    Column(modifier = modifier.padding(16.dp)) {
        LazyColumn {
            items(contactos) { contacto ->
                FilaContacto(contacto) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:${contacto.numero}")
                        }
                        context.startActivity(intent)
                    } else {
                        numeroPendiente = contacto.numero
                        permissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                }
                Divider()
            }
        }
    }
}

@Composable
fun FilaContacto(contacto: Contacto, onLlamar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(contacto.nombre, modifier = Modifier.weight(1f))
        Text(contacto.numero, modifier = Modifier.weight(1f))
        Button(onClick = onLlamar) {
            Text("LLAMAR")
        }
    }
}
