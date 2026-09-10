package com.example.trabajo1.utilidades

import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.trabajo1.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend fun fetchWeather(lat: Double, lon: Double, api: String, view: View) {
    val result = withContext(Dispatchers.IO) {
        try {
            URL("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=$api")
                .readText(Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    try {
        val jsonObj = JSONObject(result)
        val main = jsonObj.getJSONObject("main")
        val sys = jsonObj.getJSONObject("sys")
        val wind = jsonObj.getJSONObject("wind")
        val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

        val updatedAt: Long = jsonObj.getLong("dt")
        val updatedAtText = "ACTUALIZADO: " + SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
            .format(Date(updatedAt * 1000))

        val temp = main.getString("temp") + "°C"
        val tempMin = "Min Temp: " + main.getString("temp_min") + "°C"
        val tempMax = "Max Temp: " + main.getString("temp_max") + "°C"
        val pressure = main.getString("pressure")
        val humidity = main.getString("humidity")

        val windSpeed = wind.getString("speed")
        val weatherDescription = weather.getString("description")
        val address = jsonObj.getString("name") + ", " + sys.getString("country")

        view.findViewById<TextView>(R.id.address).text = address.uppercase()
        view.findViewById<TextView>(R.id.updated_at).text = updatedAtText.uppercase()
        view.findViewById<TextView>(R.id.status).text = weatherDescription.uppercase()
        view.findViewById<TextView>(R.id.temp).text = temp
        view.findViewById<TextView>(R.id.temp_min).text = tempMin.uppercase()
        view.findViewById<TextView>(R.id.temp_max).text = tempMax.uppercase()
        view.findViewById<TextView>(R.id.wind).text = windSpeed
        view.findViewById<TextView>(R.id.presion).text = pressure
        view.findViewById<TextView>(R.id.humidity).text = humidity

        view.findViewById<RelativeLayout>(R.id.main_container).visibility = View.VISIBLE
    } catch (e: Exception) {
        Log.e("WeatherDebug", "Error: ${e.message}", e)
        view.findViewById<TextView>(R.id.errortext).visibility = View.VISIBLE
    }
}