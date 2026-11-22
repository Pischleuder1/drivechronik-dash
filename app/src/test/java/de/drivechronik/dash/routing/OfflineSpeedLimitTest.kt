package de.drivechronik.dash.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineSpeedLimitTest {

    private fun response(
        type: String = "matched",
        distance: Double = 3.0,
        edgeIndex: Int = 0
    ): String = """
        {
          "edges": [
            {"speed_limit": 50},
            {"speed_limit": 100}
          ],
          "matched_points": [
            {
              "type": "matched",
              "edge_index": 0,
              "distance_from_trace_point": 2
            },
            {
              "type": "$type",
              "edge_index": $edgeIndex,
              "distance_from_trace_point": $distance
            }
          ]
        }
    """.trimIndent()

    @Test
    fun usesEdgeOfLatestGpsPoint() {
        assertEquals(
            50,
            OfflineSpeedLimit.parseLastSpeedLimit(response())
        )
    }

    @Test
    fun usesSecondEdgeWhenLatestPointMatchesIt() {
        assertEquals(
            100,
            OfflineSpeedLimit.parseLastSpeedLimit(
                response(edgeIndex = 1)
            )
        )
    }

    @Test
    fun rejectsUnmatchedGpsPoint() {
        assertNull(
            OfflineSpeedLimit.parseLastSpeedLimit(
                response(type = "unmatched")
            )
        )
    }

    @Test
    fun rejectsDistantStreet() {
        assertNull(
            OfflineSpeedLimit.parseLastSpeedLimit(
                response(distance = 60.0)
            )
        )
    }

    @Test
    fun rejectsInvalidEdgeIndex() {
        assertNull(
            OfflineSpeedLimit.parseLastSpeedLimit(
                response(edgeIndex = 5)
            )
        )
    }

    @Test
    fun rejectsMissingMapMatching() {
        assertNull(
            OfflineSpeedLimit.parseLastSpeedLimit(
                """{"edges":[{"speed_limit":50}]}"""
            )
        )
    }

    @Test
    fun rejectsUnknownLimit() {
        assertNull(
            OfflineSpeedLimit.parseLastSpeedLimit(
                """{
                  "edges":[{"speed_limit":0}],
                  "matched_points":[
                    {"type":"matched","edge_index":0,
                     "distance_from_trace_point":2},
                    {"type":"matched","edge_index":0,
                     "distance_from_trace_point":3}
                  ]
                }"""
            )
        )
    }
}
