package com.example.phoneapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun SensorDataDisplay(
    basicNotification: BasicNotification
): Float {
    val context = LocalContext.current
    var sensorData by remember { mutableFloatStateOf(0f) }

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    sensorData = x + y + z
                }
            }
        }
    }

    if (sensorData > 15.0) {
        basicNotification.showBasicNotification()
    }

    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

    return sensorData
}