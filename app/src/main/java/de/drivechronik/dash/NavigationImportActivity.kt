package de.drivechronik.dash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class NavigationImportActivity : Activity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val payload =
            intent
                ?.data
                ?.getQueryParameter(
                    "payload"
                )

        if (payload.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Kein Navigationsplan übergeben",
                Toast.LENGTH_LONG
            ).show()

            openDashboard()
            return
        }

        DriveChronikNavigationImport
            .decodePayload(payload)
            .onSuccess { plan ->
                NavigationImportState.publish(
                    plan
                )

                Toast.makeText(
                    this,
                    "DriveChronik-Route empfangen",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .onFailure { error ->
                Toast.makeText(
                    this,
                    "Navigationsplan ungültig: " +
                        (error.message ?: "Fehler"),
                    Toast.LENGTH_LONG
                ).show()
            }

        openDashboard()
    }

    private fun openDashboard() {
        startActivity(
            Intent(
                this,
                MainActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
        )

        finish()
    }
}
