package com.example.trabajo1.utilidades

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale

// ---------- ViewModel ----------

data class CatastrofeHistorial(
    val id: String = "",
    val tipo: String = "",
    val timestamp: Timestamp? = null,
    val latitud: String = "",
    val longitud: String = ""
)
class HistorialViewModel : ViewModel() {

    private val _historial = MutableStateFlow<List<CatastrofeHistorial>>(emptyList())
    val historial: StateFlow<List<CatastrofeHistorial>> = _historial.asStateFlow()

    init {
        escucharHistorial()
    }

    private fun escucharHistorial() {
        Firebase.firestore.collection("catastrofes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CatastrofeHistorial::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _historial.value = lista
            }
    }
}

// ---------- Pantalla ----------

@Composable
fun HistorialScreen(
    modifier: Modifier = Modifier,
    viewModel: HistorialViewModel = viewModel()
) {
    val historial by viewModel.historial.collectAsState()

    if (historial.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp)
        ) {
            Text(
                text = "Todavía no hay catástrofes registradas en tu zona.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        items(historial) { evento ->
            HistorialItem(evento)
        }
    }
}

@Composable
fun HistorialItem(evento: CatastrofeHistorial) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = evento.tipo.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = formatearFecha(evento.timestamp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun formatearFecha(timestamp: Timestamp?): String {
    if (timestamp == null) return "Fecha desconocida"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "AR"))
    return sdf.format(timestamp.toDate())
}


@Composable
fun HistorialEmbebido(
    modifier: Modifier = Modifier,
    viewModel: HistorialViewModel = viewModel()
) {
    val historial by viewModel.historial.collectAsState()

    if (historial.isEmpty()) {
        Text(
            text = "Todavía no hay catástrofes registradas en tu zona.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        historial.take(5).forEach { evento ->
            HistorialItem(evento)
        }
    }
}