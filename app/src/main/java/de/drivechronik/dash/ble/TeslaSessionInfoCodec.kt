package de.drivechronik.dash.ble

import com.tesla.generated.signatures.Signatures

object TeslaSessionInfoCodec {

    fun decode(
        bytes: ByteArray
    ): Signatures.SessionInfo {

        return Signatures
            .SessionInfo
            .parseFrom(bytes)
    }
}
