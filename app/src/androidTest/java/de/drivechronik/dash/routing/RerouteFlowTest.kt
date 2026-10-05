package de.drivechronik.dash.routing

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Test

class RerouteFlowTest {
    @Test
    fun completeFlow() = runBlocking {
        val start = RoutePoint(52.0, 8.0)
        val target = RoutePoint(52.0, 8.01)
        val far = RoutePoint(52.002, 8.005)
        val near = RoutePoint(52.0, 8.005)

        val original = OfflineRoute(
            listOf(start, target), 0.68,
            60.0, emptyList(), target
        )
        val replacement = original.copy(
            points = listOf(far, target)
        )

        var fail = true
        var receivedTarget: RoutePoint? = null

        val flow = RerouteCoordinator(original) { _, end ->
            receivedTarget = end
            if (fail) Result.failure(Exception("Testfehler"))
            else Result.success(replacement)
        }

        // Einzelner Ausreisser, danach wieder auf Route.
        assertFalse(flow.onFix(far, 1000))
        assertFalse(flow.onFix(near, 2000))
        assertEquals(0, flow.attempts)

        // Dauerhafte Abweichung, Valhalla-Fehler.
        assertFalse(flow.onFix(far, 3000))
        assertFalse(flow.onFix(far, 5000))
        assertFalse(flow.onFix(far, 9000))
        assertEquals(1, flow.attempts)
        assertSame(original, flow.route)

        // Keine Wiederholung vor 60 Sekunden.
        fail = false
        assertFalse(flow.onFix(far, 10000))
        assertFalse(flow.onFix(far, 68999))
        assertEquals(1, flow.attempts)

        // Nach Ablauf: neue Route uebernehmen.
        assertTrue(flow.onFix(far, 69000))
        assertEquals(2, flow.attempts)
        assertSame(replacement, flow.route)
        assertEquals(target, receivedTarget)
    }

    @Test
    fun staleResultMustNotOverwrite() = runBlocking {
        val a = RoutePoint(52.0, 8.0)
        val b = RoutePoint(52.0, 8.01)
        val far = RoutePoint(52.002, 8.005)

        val old = OfflineRoute(listOf(a, b),
            0.7, 60.0, emptyList(), b)
        val manual = old.copy(distanceKm = 0.8)
        val stale = old.copy(distanceKm = 0.9)
        val response = CompletableDeferred<Result<OfflineRoute>>()

        val flow = RerouteCoordinator(old) { _, _ ->
            response.await()
        }

        flow.onFix(far, 1000)
        flow.onFix(far, 3000)

        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            flow.onFix(far, 7000)
        }

        flow.adoptRoute(manual)
        response.complete(Result.success(stale))

        assertFalse(pending.await())
        assertSame(manual, flow.route)
    }
}
