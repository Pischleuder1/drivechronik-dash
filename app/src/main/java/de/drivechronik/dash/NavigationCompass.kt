package de.drivechronik.dash

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberCompassHeading(): Float? {
    val context = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(context) {
        val manager = context.getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        val sensor = manager.getDefaultSensor(
            Sensor.TYPE_ROTATION_VECTOR
        )

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val matrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(
                    matrix, event.values
                )
                val angles = FloatArray(3)
                SensorManager.getOrientation(matrix, angles)

                val degrees = Math.toDegrees(
                    angles[0].toDouble()
                ).toFloat()

                val next = (degrees + 360f) % 360f
                val previous = heading
                val difference = if (previous == null) 180f
                    else kotlin.math.abs(
                        (next - previous + 540f) % 360f - 180f
                    )

                if (difference >= 2f) heading = next
            }

            override fun onAccuracyChanged(
                sensor: Sensor?, accuracy: Int
            ) = Unit
        }

        if (sensor != null) {
            manager.registerListener(
                listener, sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        onDispose {
            manager.unregisterListener(listener)
        }
    }

    return heading
}
