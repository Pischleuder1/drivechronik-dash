package de.drivechronik.dash.data

import de.drivechronik.dash.ble.TeslaVehicleDataUpdate
import de.drivechronik.dash.ble.TeslaVehicleSnapshot
import de.drivechronik.dash.ble.TeslaVehicleSnapshotStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


object TeslaDashboardRepository {

    private val store =
        TeslaVehicleSnapshotStore()


    private val mutableSnapshot =
        MutableStateFlow(
            store.current()
        )


    val snapshot:
        StateFlow<TeslaVehicleSnapshot> =
            mutableSnapshot.asStateFlow()


    @Synchronized
    fun apply(
        update: TeslaVehicleDataUpdate,
        receivedAtNanos: Long =
            System.nanoTime()
    ): TeslaVehicleSnapshot {

        var result =
            store.current()


        update.drive?.let {

            result =
                store.updateDrive(
                    data = it,
                    receivedAtNanos =
                        receivedAtNanos
                )
        }


        update.charge?.let {

            result =
                store.updateCharge(
                    data = it,
                    receivedAtNanos =
                        receivedAtNanos
                )
        }


        update.location?.let {

            result =
                store.updateLocation(
                    data = it,
                    receivedAtNanos =
                        receivedAtNanos
                )
        }


        mutableSnapshot.value =
            result


        return result
    }


    @Synchronized
    fun clear():
        TeslaVehicleSnapshot {

        val result =
            store.clear()

        mutableSnapshot.value =
            result

        return result
    }
}
