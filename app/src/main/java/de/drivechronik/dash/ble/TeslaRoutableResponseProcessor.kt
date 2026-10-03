package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage

class TeslaResponseReplayException(
    message: String
) : SecurityException(message)

class TeslaResponseUuidMismatchException(
    message: String
) : SecurityException(message)


data class TeslaProcessedResponse(

    val payload: ByteArray,

    val encrypted: Boolean,

    val responseCounter: Long?,

    val operationStatus:
        UniversalMessage.OperationStatus_E,

    val fault:
        UniversalMessage.MessageFault_E

)


object TeslaRoutableResponseProcessor {

    fun process(
        message:
            UniversalMessage.RoutableMessage,

        domain:
            UniversalMessage.Domain,

        vin: String,

        sharedKey: ByteArray,

        requestHash: ByteArray,

        replayGuard:
            TeslaResponseReplayGuard,

        expectedRequestUuid:
            ByteArray? = null

    ): TeslaProcessedResponse {

        /*
         * Antworten tragen normalerweise
         * request_uuid = uuid des Requests.
         *
         * VCSEC darf das Feld allerdings
         * weglassen. Deshalb prüfen wir nur,
         * wenn es tatsächlich vorhanden ist.
         */
        if (
            expectedRequestUuid != null &&
            !message.requestUuid.isEmpty
        ) {

            if (
                !message
                    .requestUuid
                    .toByteArray()
                    .contentEquals(
                        expectedRequestUuid
                    )
            ) {

                throw TeslaResponseUuidMismatchException(
                    "Tesla response request_uuid does not match request"
                )
            }
        }


        val status =
            message.signedMessageStatus


        val operationStatus =
            status.operationStatus


        val fault =
            status.signedMessageFault


        val faultValue =
            status.signedMessageFaultValue
                .toLong() and
                0xffff_ffffL


        val responseFlags =
            message.flags.toLong() and
                0xffff_ffffL


        val encrypted =
            message.hasSignatureData() &&
                message
                    .signatureData
                    .hasAESGCMResponseData()


        if (!encrypted) {

            /*
             * Ältere Fahrzeugsoftware kann
             * FLAG_ENCRYPT_RESPONSE ignorieren.
             *
             * Dann kommt protobuf_message_as_bytes
             * im Klartext zurück.
             */
            return TeslaProcessedResponse(
                payload =
                    message
                        .protobufMessageAsBytes
                        .toByteArray(),

                encrypted = false,

                responseCounter = null,

                operationStatus =
                    operationStatus,

                fault = fault
            )
        }


        val aes =
            message
                .signatureData
                .aesgcmResponseData


        val responseCounter =
            aes.counter.toLong() and
                0xffff_ffffL


        /*
         * ZUERST authentifizieren/decrypten.
         *
         * Ein ungültiges Paket darf keinen
         * Counter im Replay-Guard verbrauchen.
         */
        val decrypted =
            TeslaResponseCrypto
                .decrypt(
                    ciphertext =
                        message
                            .protobufMessageAsBytes
                            .toByteArray(),

                    sharedKey =
                        sharedKey,

                    domain =
                        domain,

                    vin =
                        vin,

                    counter =
                        responseCounter,

                    flags =
                        responseFlags,

                    requestHash =
                        requestHash,

                    fault =
                        faultValue,

                    nonce =
                        aes.nonce.toByteArray(),

                    tag =
                        aes.tag.toByteArray()
                )


        /*
         * Erst NACH erfolgreicher
         * AES-GCM-Authentifizierung markieren.
         */
        val fresh =
            replayGuard.accept(
                requestHash =
                    requestHash,
                counter =
                    responseCounter
            )


        if (!fresh) {

            throw TeslaResponseReplayException(
                "Duplicate Tesla response counter " +
                    responseCounter
            )
        }


        return TeslaProcessedResponse(
            payload =
                decrypted.plaintext,

            encrypted = true,

            responseCounter =
                responseCounter,

            operationStatus =
                operationStatus,

            fault =
                fault
        )
    }
}
