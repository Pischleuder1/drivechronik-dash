package de.drivechronik.dash.ble

import com.tesla.generated.signatures.Signatures
import com.tesla.generated.universalmessage.UniversalMessage
import java.security.KeyPair
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey


private object TeslaSessionRandom {

    val secureRandom =
        SecureRandom()
}


enum class TeslaProtocolSessionStatus {

    IDLE,

    HANDSHAKE_PENDING,

    READY

}


class TeslaProtocolSessionException(
    message: String
) : SecurityException(message)


data class TeslaOutboundProtocolMessage(

    val message:
        UniversalMessage.RoutableMessage,

    val uuid:
        ByteArray,

    val requestHash:
        ByteArray? = null

)


class TeslaBleSessionController(

    private val vin: String,

    val domain:
        UniversalMessage.Domain,

    private val keyPair:
        KeyPair,

    routingAddress:
        ByteArray? = null,

    private val nowNanos:
        () -> Long = System::nanoTime,

    private val randomBytes:
        (Int) -> ByteArray = { size ->

            ByteArray(size).also {

                TeslaSessionRandom
                    .secureRandom
                    .nextBytes(it)
            }
        },

    /*
     * Lokale Sicherheitsgrenze.
     *
     * Teslas Spezifikation spricht bei
     * SessionInfo-Antworten von einer
     * kürzlich verwendeten UUID innerhalb
     * "the last several seconds".
     *
     * Das ist keine Tesla-Protokollkonstante.
     */
    private val maxHandshakeAgeNanos:
        Long = 10_000_000_000L

) {

    companion object {

        private const val UUID_SIZE =
            16

        private const val ROUTING_ADDRESS_SIZE =
            16

        private const val DEFAULT_COMMAND_TTL_SECONDS =
            5L
    }


    private data class PendingHandshake(

        val uuid: ByteArray,

        val sentAtNanos: Long

    )


    private data class PendingCommand(

        val uuid: ByteArray,

        val requestHash: ByteArray,

        val createdAtNanos: Long

    )


    private val routingAddress =
        (
            routingAddress
                ?: randomBytes(
                    ROUTING_ADDRESS_SIZE
                )
            )
            .copyOf()


    private var pendingHandshake:
        PendingHandshake? = null


    private var sessionState:
        TeslaSessionState? = null


    private val pendingCommands =
        linkedMapOf<
            String,
            PendingCommand
        >()


    private val replayGuard =
        TeslaResponseReplayGuard()


    init {

        require(
            vin.length == 17
        ) {
            "VIN must contain exactly 17 characters"
        }


        require(
            this.routingAddress.size ==
                ROUTING_ADDRESS_SIZE
        ) {
            "Routing address must contain 16 bytes"
        }


        require(
            keyPair.public is
                ECPublicKey
        ) {
            "Tesla client public key must be EC"
        }
    }


    @Synchronized
    fun status():
        TeslaProtocolSessionStatus {

        return when {

            sessionState != null ->
                TeslaProtocolSessionStatus.READY

            pendingHandshake != null ->
                TeslaProtocolSessionStatus
                    .HANDSHAKE_PENDING

            else ->
                TeslaProtocolSessionStatus.IDLE
        }
    }


    @Synchronized
    fun routingAddress():
        ByteArray {

        return routingAddress.copyOf()
    }


    @Synchronized
    fun session():
        TeslaSessionState? {

        return sessionState
    }


    @Synchronized
    fun pendingRequestCount():
        Int {

        return pendingCommands.size
    }


    /*
     * Erstellt NUR die Handshake-Nachricht.
     *
     * Es findet hier noch keinerlei
     * BLE-Übertragung statt.
     */
    @Synchronized
    fun beginHandshake():
        TeslaOutboundProtocolMessage {

        sessionState =
            null

        pendingCommands.clear()

        replayGuard.clearAll()


        val uuid =
            newRandomBytes(
                UUID_SIZE
            )


        val publicKey =
            TeslaPublicKeyEncoding
                .encode(
                    keyPair.public
                        as ECPublicKey
                )


        val message =
            TeslaSessionInfoRequestBuilder
                .build(
                    publicKey =
                        publicKey,

                    domain =
                        domain,

                    routingAddress =
                        routingAddress,

                    uuid =
                        uuid
                )


        pendingHandshake =
            PendingHandshake(
                uuid =
                    uuid.copyOf(),

                sentAtNanos =
                    nowNanos()
            )


        return TeslaOutboundProtocolMessage(
            message =
                message,

            uuid =
                uuid.copyOf()
        )
    }


    /*
     * Prüft:
     *
     * - erwartete Handshake-UUID
     * - Alter der Handshake-Antwort
     * - Routing Address
     * - Domain
     * - SessionInfo Status
     * - ECDH
     * - SessionInfo-HMAC
     *
     * Erst DANACH wird die Session aktiv.
     */
    @Synchronized
    fun acceptHandshakeResponse(
        message:
            UniversalMessage.RoutableMessage
    ): TeslaSessionState {

        val pending =
            pendingHandshake
                ?: throw TeslaProtocolSessionException(
                    "No Tesla handshake is pending"
                )


        val receivedAt =
            nowNanos()


        val age =
            receivedAt -
                pending.sentAtNanos


        if (
            age < 0L ||
            age > maxHandshakeAgeNanos
        ) {

            throw TeslaProtocolSessionException(
                "Tesla handshake response is stale"
            )
        }


        val responseUuid =
            message
                .requestUuid
                .toByteArray()


        if (
            !responseUuid
                .contentEquals(
                    pending.uuid
                )
        ) {

            throw TeslaProtocolSessionException(
                "Tesla handshake request UUID mismatch"
            )
        }


        val responseRoutingAddress =
            message
                .toDestination
                .routingAddress
                .toByteArray()


        if (
            !responseRoutingAddress
                .contentEquals(
                    routingAddress
                )
        ) {

            throw TeslaProtocolSessionException(
                "Tesla handshake routing address mismatch"
            )
        }


        if (
            message
                .fromDestination
                .domain !=
            domain
        ) {

            throw TeslaProtocolSessionException(
                "Tesla handshake domain mismatch"
            )
        }


        val sessionInfoBytes =
            message
                .sessionInfo
                .toByteArray()


        if (
            sessionInfoBytes.isEmpty()
        ) {

            throw TeslaProtocolSessionException(
                "Tesla response contains no SessionInfo"
            )
        }


        if (
            !message.hasSignatureData() ||
            !message
                .signatureData
                .hasSessionInfoTag()
        ) {

            throw TeslaProtocolSessionException(
                "Tesla SessionInfo response has no authentication tag"
            )
        }


        val info =
            TeslaSessionInfoCodec
                .decode(
                    sessionInfoBytes
                )


        /*
         * 0 =
         * SESSION_INFO_STATUS_OK
         *
         * Durch statusValue vermeiden wir
         * Abhängigkeit vom Java-Namen des
         * generierten Proto-Enums.
         */
        if (
            info.statusValue != 0
        ) {

            throw TeslaProtocolSessionException(
                "Tesla SessionInfo status is ${info.status}"
            )
        }


        val vehiclePublicKey =
            TeslaSessionCrypto
                .decodePublicKey(
                    info
                        .publicKey
                        .toByteArray()
                )


        val sharedKey =
            TeslaSessionCrypto
                .deriveSharedKey(
                    privateKey =
                        keyPair.private,

                    vehiclePublicKey =
                        vehiclePublicKey
                )


        val receivedTag =
            message
                .signatureData
                .sessionInfoTag
                .tag
                .toByteArray()


        val valid =
            TeslaSessionInfoAuthenticator
                .verifyTag(
                    sharedKey =
                        sharedKey,

                    vin =
                        vin,

                    challenge =
                        pending.uuid,

                    sessionInfoBytes =
                        sessionInfoBytes,

                    receivedTag =
                        receivedTag
                )


        if (!valid) {

            throw TeslaProtocolSessionException(
                "Tesla SessionInfo authentication failed"
            )
        }


        val state =
            TeslaSessionState
                .fromAuthenticatedSessionInfo(
                    domain =
                        domain,

                    info =
                        info,

                    sharedKey =
                        sharedKey,

                    receivedAtNanos =
                        receivedAt
                )


        sessionState =
            state

        pendingHandshake =
            null


        return state
    }


    /*
     * Baut einen authentifizierten
     * AES-GCM Request.
     *
     * Noch KEIN BLE-Senden.
     */
    @Synchronized
    fun buildEncryptedRequest(
        plaintext: ByteArray,
        ttlSeconds: Long =
            DEFAULT_COMMAND_TTL_SECONDS
    ): TeslaOutboundProtocolMessage {

        require(
            ttlSeconds in 1L..60L
        ) {
            "Command TTL must be 1..60 seconds"
        }


        val state =
            sessionState
                ?: throw TeslaProtocolSessionException(
                    "Tesla session is not ready"
                )


        val counter =
            state.reserveCounter()


        val expiresAt =
            state.vehicleTimeSeconds(
                nowNanos()
            ) +
                ttlSeconds


        val uuid =
            newRandomBytes(
                UUID_SIZE
            )


        val nonce =
            newRandomBytes(
                TeslaCommandCrypto.NONCE_SIZE
            )


        val flags =
            1L shl
                UniversalMessage
                    .Flags
                    .FLAG_ENCRYPT_RESPONSE
                    .number


        val publicKey =
            TeslaPublicKeyEncoding
                .encode(
                    keyPair.public
                        as ECPublicKey
                )


        val encrypted =
            TeslaEncryptedCommandBuilder
                .build(
                    plaintext =
                        plaintext,

                    clientPublicKey =
                        publicKey,

                    sharedKey =
                        state.sharedKey(),

                    domain =
                        domain,

                    vin =
                        vin,

                    epoch =
                        state.epoch(),

                    routingAddress =
                        routingAddress,

                    uuid =
                        uuid,

                    expiresAt =
                        expiresAt,

                    counter =
                        counter,

                    flags =
                        flags,

                    nonce =
                        nonce
                )


        val key =
            uuid.toHexKey()


        if (
            pendingCommands
                .containsKey(
                    key
                )
        ) {

            throw TeslaProtocolSessionException(
                "Duplicate Tesla request UUID"
            )
        }


        pendingCommands[key] =
            PendingCommand(
                uuid =
                    uuid.copyOf(),

                requestHash =
                    encrypted
                        .requestHash
                        .copyOf(),

                createdAtNanos =
                    nowNanos()
            )


        return TeslaOutboundProtocolMessage(
            message =
                encrypted.message,

            uuid =
                uuid.copyOf(),

            requestHash =
                encrypted
                    .requestHash
                    .copyOf()
        )
    }


    /*
     * Verarbeitet eine Response für einen
     * vorher erzeugten Request.
     *
     * Sowohl verschlüsselte Responses als
     * auch der Plaintext-Fallback älterer
     * Firmware werden vom vorhandenen
     * ResponseProcessor behandelt.
     */
    @Synchronized
    fun processCommandResponse(
        message:
            UniversalMessage.RoutableMessage
    ): TeslaProcessedResponse {

        val state =
            sessionState
                ?: throw TeslaProtocolSessionException(
                    "Tesla session is not ready"
                )


        val pending =
            findPendingRequest(
                message
            )


        val result =
            TeslaRoutableResponseProcessor
                .process(
                    message =
                        message,

                    domain =
                        domain,

                    vin =
                        vin,

                    sharedKey =
                        state.sharedKey(),

                    requestHash =
                        pending.requestHash,

                    replayGuard =
                        replayGuard,

                    expectedRequestUuid =
                        pending.uuid
                )


        /*
         * WAIT kann weitere Antworten
         * für denselben Request bedeuten.
         *
         * OK oder ERROR schließen den
         * Request für unseren aktuellen
         * sequenziellen Client ab.
         */
        if (
            result.operationStatus !=
            UniversalMessage
                .OperationStatus_E
                .OPERATIONSTATUS_WAIT
        ) {

            pendingCommands.remove(
                pending.uuid.toHexKey()
            )
        }


        return result
    }


    @Synchronized
    fun clear() {

        pendingHandshake =
            null

        sessionState =
            null

        pendingCommands.clear()

        replayGuard.clearAll()
    }


    private fun findPendingRequest(
        message:
            UniversalMessage.RoutableMessage
    ): PendingCommand {

        val requestUuid =
            message
                .requestUuid
                .toByteArray()


        if (
            requestUuid.isNotEmpty()
        ) {

            return pendingCommands[
                requestUuid.toHexKey()
            ]
                ?: throw TeslaProtocolSessionException(
                    "Unknown Tesla response request UUID"
                )
        }


        /*
         * VCSEC kann request_uuid
         * weglassen.
         *
         * Ohne UUID ist eine Zuordnung nur
         * eindeutig, wenn genau ein Request
         * offen ist.
         */
        if (
            pendingCommands.size == 1
        ) {

            return pendingCommands
                .values
                .first()
        }


        throw TeslaProtocolSessionException(
            "Tesla response cannot be matched to a request"
        )
    }


    private fun newRandomBytes(
        size: Int
    ): ByteArray {

        val value =
            randomBytes(
                size
            )


        require(
            value.size == size
        ) {
            "Random source returned wrong byte count"
        }


        return value.copyOf()
    }


    private fun ByteArray.toHexKey():
        String {

        return joinToString("") {

            "%02x".format(
                it.toInt() and 0xff
            )
        }
    }
}
