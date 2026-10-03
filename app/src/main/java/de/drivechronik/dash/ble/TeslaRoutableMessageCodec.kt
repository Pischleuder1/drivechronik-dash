package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage

object TeslaRoutableMessageCodec {

    fun encode(
        message: UniversalMessage.RoutableMessage
    ): ByteArray {

        return message.toByteArray()
    }


    fun decode(
        bytes: ByteArray
    ): UniversalMessage.RoutableMessage {

        return UniversalMessage
            .RoutableMessage
            .parseFrom(bytes)
    }
}
