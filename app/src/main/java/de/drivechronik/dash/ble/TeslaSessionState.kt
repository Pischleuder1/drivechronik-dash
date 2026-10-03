package de.drivechronik.dash.ble

import com.tesla.generated.signatures.Signatures
import com.tesla.generated.universalmessage.UniversalMessage
import java.security.MessageDigest

class TeslaSessionState private constructor(

    val domain: UniversalMessage.Domain,

    initialEpoch: ByteArray,

    initialCounter: Long,

    initialVehicleClockSeconds: Long,

    initialSampleNanos: Long,

    initialSharedKey: ByteArray

) {

    companion object {

        private const val UINT32_MAX =
            0xffff_ffffL

        fun fromAuthenticatedSessionInfo(
            domain: UniversalMessage.Domain,
            info: Signatures.SessionInfo,
            sharedKey: ByteArray,
            receivedAtNanos: Long =
                System.nanoTime()
        ): TeslaSessionState {

            val epoch =
                info.epoch.toByteArray()

            require(
                epoch.size == 16
            ) {
                "Tesla epoch must contain 16 bytes"
            }

            require(
                sharedKey.size == 16
            ) {
                "Tesla shared key must contain 16 bytes"
            }

            return TeslaSessionState(
                domain = domain,
                initialEpoch = epoch,
                initialCounter =
                    Integer.toUnsignedLong(
                        info.counter
                    ),
                initialVehicleClockSeconds =
                    Integer.toUnsignedLong(
                        info.clockTime
                    ),
                initialSampleNanos =
                    receivedAtNanos,
                initialSharedKey =
                    sharedKey.copyOf()
            )
        }
    }


    private var epoch =
        initialEpoch.copyOf()

    private var counter =
        initialCounter

    private var vehicleClockSeconds =
        initialVehicleClockSeconds

    private var sampleNanos =
        initialSampleNanos

    private var sharedKey =
        initialSharedKey.copyOf()


    @Synchronized
    fun epoch(): ByteArray {

        return epoch.copyOf()
    }


    @Synchronized
    fun sharedKey(): ByteArray {

        return sharedKey.copyOf()
    }


    @Synchronized
    fun currentCounter(): Long {

        return counter
    }


    /*
     * Ein reservierter Counter wird niemals
     * wieder zurückgegeben.
     *
     * Auch wenn anschließend keine Antwort
     * empfangen wird, könnte das Fahrzeug
     * den Request bereits verarbeitet haben.
     */
    @Synchronized
    fun reserveCounter(): Long {

        require(
            counter < UINT32_MAX
        ) {
            "Tesla session counter exhausted"
        }

        counter += 1

        return counter
    }


    @Synchronized
    fun vehicleTimeSeconds(
        nowNanos: Long =
            System.nanoTime()
    ): Long {

        require(
            nowNanos >= sampleNanos
        ) {
            "Monotonic clock moved backwards"
        }

        val elapsedSeconds =
            (
                nowNanos -
                    sampleNanos
                ) / 1_000_000_000L

        return vehicleClockSeconds +
            elapsedSeconds
    }


    /*
     * Rückgabe:
     *
     * true  = authentifizierte SessionInfo
     *         wurde übernommen.
     *
     * false = alte Fahrzeugzeit innerhalb
     *         derselben Epoch -> verwerfen.
     */
    @Synchronized
    fun updateAuthenticatedSessionInfo(
        info: Signatures.SessionInfo,
        newSharedKey: ByteArray,
        receivedAtNanos: Long =
            System.nanoTime()
    ): Boolean {

        require(
            newSharedKey.size == 16
        ) {
            "Tesla shared key must contain 16 bytes"
        }

        val newEpoch =
            info.epoch.toByteArray()

        require(
            newEpoch.size == 16
        ) {
            "Tesla epoch must contain 16 bytes"
        }

        val newCounter =
            Integer.toUnsignedLong(
                info.counter
            )

        val newVehicleClock =
            Integer.toUnsignedLong(
                info.clockTime
            )

        val sameEpoch =
            MessageDigest.isEqual(
                epoch,
                newEpoch
            )

        if (sameEpoch) {

            /*
             * Tesla verlangt:
             * ältere clock_time derselben
             * Epoch nicht akzeptieren.
             */
            if (
                newVehicleClock <
                    vehicleClockSeconds
            ) {
                return false
            }

            /*
             * Counter innerhalb einer Epoch
             * niemals zurückrollen.
             */
            counter =
                maxOf(
                    counter,
                    newCounter
                )
        }
        else {

            /*
             * Neue Epoch = Fahrzeugdomain
             * wurde neu gestartet.
             *
             * Jetzt darf der Counter wieder
             * vom Fahrzeug übernommen werden.
             */
            epoch =
                newEpoch.copyOf()

            counter =
                newCounter
        }

        vehicleClockSeconds =
            newVehicleClock

        sampleNanos =
            receivedAtNanos

        sharedKey =
            newSharedKey.copyOf()

        return true
    }
}
