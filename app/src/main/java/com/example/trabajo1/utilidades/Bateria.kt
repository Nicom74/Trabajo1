package com.example.trabajo1.utilidades

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class Bateria {
    var porcentaje by mutableStateOf(0)
    var horasRestantes by mutableStateOf<Double?>(null)
    var horaAgotamiento by mutableStateOf<String?>(null)

    val textoEstado: String
        get() = if (horasRestantes != null && horaAgotamiento != null) {
            val horas = horasRestantes!!.toInt()
            val minutos = ((horasRestantes!! - horas) * 60).toInt()
            "$porcentaje% · ${horas}h ${minutos}m restantes (se agota ~$horaAgotamiento)"
        } else {
            "$porcentaje% · Cargando"
        }
}

@Composable
fun rememberBatteryEstado(): Bateria {
    val context = LocalContext.current
    val estado = remember { Bateria() }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val nivel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val escala = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (nivel >= 0 && escala > 0) {
                    estado.porcentaje = (nivel * 100 / escala)
                }

                val batteryManager = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val corrienteMicroA = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

                if (corrienteMicroA < 0) {
                    val corrienteMa = abs(corrienteMicroA / 1000.0)
                    val capacidadTotalMah = obtenerCapacidadBateriaMah(ctx)
                    val cargaRestanteMah = capacidadTotalMah * (estado.porcentaje / 100.0)
                    val horas = cargaRestanteMah / corrienteMa

                    estado.horasRestantes = horas

                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.MINUTE, (horas * 60).toInt())
                    estado.horaAgotamiento = String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE)
                    )
                } else {
                    estado.horasRestantes = null
                    estado.horaAgotamiento = null
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose { context.unregisterReceiver(receiver) }
    }

    return estado
}

private fun obtenerCapacidadBateriaMah(context: Context): Double {
    return try {
        val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
        val powerProfile = powerProfileClass
            .getConstructor(Context::class.java)
            .newInstance(context)
        val method = powerProfileClass.getMethod("getBatteryCapacity")
        method.invoke(powerProfile) as Double
    } catch (e: Exception) {
        4000.0 // valor por defecto si falla (ej: 4000 mAh)
    }
}