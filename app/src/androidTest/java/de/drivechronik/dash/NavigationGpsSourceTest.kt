package de.drivechronik.dash

import de.drivechronik.dash.routing.RoutePoint
import org.junit.Assert.*
import org.junit.Test

class NavigationGpsSourceTest {
    @Test
    fun sourcePriorityAndFallback() {
        val tesla = NavigationGpsFix(
            RoutePoint(52.0, 8.0), 100L, "tesla", null
        )
        val phone = NavigationGpsFix(
            RoutePoint(52.1, 8.1), 200L, "phone", 90f
        )

        assertSame(
            tesla, selectNavigationGpsFix(tesla, phone)
        )
        assertSame(
            phone, selectNavigationGpsFix(null, phone)
        )
        assertSame(
            tesla, selectNavigationGpsFix(tesla, null)
        )
        assertNull(selectNavigationGpsFix(null, null))
    }
}
