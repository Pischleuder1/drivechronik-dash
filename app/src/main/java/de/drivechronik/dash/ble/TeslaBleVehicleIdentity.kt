package de.drivechronik.dash.ble

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale


object TeslaBleVehicleIdentity {

    fun normalizeVin(
        vin: String
    ): String {

        return vin
            .trim()
            .uppercase(
                Locale.ROOT
            )
    }


    fun advertisementLocalName(
        vin: String
    ): String {

        val normalizedVin =
            normalizeVin(
                vin
            )


        require(
            normalizedVin.length == 17
        ) {
            "Tesla VIN must contain exactly 17 characters"
        }


        val digest =
            MessageDigest
                .getInstance(
                    "SHA-1"
                )
                .digest(
                    normalizedVin
                        .toByteArray(
                            StandardCharsets.US_ASCII
                        )
                )


        val id =
            digest
                .take(8)
                .joinToString(
                    separator = ""
                ) { byte ->

                    "%02x".format(
                        byte.toInt() and 0xff
                    )
                }


        return "S${id}C"
    }


    fun matches(
        vin: String,
        localName: String?
    ): Boolean {

        if (localName == null) {
            return false
        }


        return runCatching {

            advertisementLocalName(
                vin
            ) == localName

        }.getOrDefault(
            false
        )
    }
}
