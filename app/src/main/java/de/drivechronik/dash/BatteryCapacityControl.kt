package de.drivechronik.dash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Lokale Konfiguration der nutzbaren Akkukapazität.
 * Keine Änderung an der Tesla-BLE-Konfiguration.
 */
@Composable
internal fun BatteryCapacityControl(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val settings = remember(context) {
        BatteryCapacitySettings(context)
    }

    var savedCapacity by remember(settings) {
        mutableStateOf(settings.load())
    }

    var input by remember(settings) {
        mutableStateOf(
            savedCapacity?.let {
                String.format(Locale.GERMANY, "%.1f", it)
            } ?: ""
        )
    }

    var error by remember {
        mutableStateOf(false)
    }

    val parsedCapacity = input.trim()
        .replace(',', '.')
        .toFloatOrNull()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "NUTZBARE AKKUKAPAZITÄT",
            color = Color(0xFF98AABB),
            fontSize = 11.sp
        )

        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                error = false
            },
            label = {
                Text("Kapazität in kWh")
            },
            singleLine = true,
            isError = error,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val value = parsedCapacity

                    if (
                        value != null &&
                        settings.save(value)
                    ) {
                        savedCapacity = value
                        input = String.format(
                            Locale.GERMANY,
                            "%.1f",
                            value
                        )
                        error = false
                    } else {
                        error = true
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Speichern")
            }

            OutlinedButton(
                onClick = {
                    settings.clear()
                    savedCapacity = null
                    input = ""
                    error = false
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Zurücksetzen")
            }
        }

        Text(
            text = when {
                error ->
                    "Bitte 20 bis 150 kWh eingeben."
                savedCapacity != null ->
                    "Gespeichert: " +
                        String.format(
                            Locale.GERMANY,
                            "%.1f kWh",
                            savedCapacity
                        )
                else ->
                    "Keine Kapazität hinterlegt."
            },
            color = if (error) {
                Color(0xFFFF7777)
            } else {
                Color(0xFF98AABB)
            },
            fontSize = 11.sp
        )

        Text(
            text = "Manuell hinterlegter Wert, " +
                "kein Tesla-BLE-Messwert.",
            color = Color(0xFF98AABB),
            fontSize = 10.sp
        )
    }
}
