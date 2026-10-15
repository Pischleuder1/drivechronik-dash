package de.drivechronik.dash.routing

import org.junit.Assert.assertEquals
import org.junit.Test

class OsrmHeadingTest {

    @Test
    fun headingEast() {
        assertEquals("&bearings=90,45;", osrmHeadingParameter(90f))
    }

    @Test
    fun headingNorth() {
        assertEquals("&bearings=0,45;", osrmHeadingParameter(0f))
    }

    @Test
    fun headingNormalized() {
        assertEquals("&bearings=270,45;", osrmHeadingParameter(-90f))
        assertEquals("&bearings=10,45;", osrmHeadingParameter(370f))
    }

    @Test
    fun missingHeading() {
        assertEquals("", osrmHeadingParameter(null))
        assertEquals("", osrmHeadingParameter(Float.NaN))
        assertEquals("", osrmHeadingParameter(Float.POSITIVE_INFINITY))
    }
}
