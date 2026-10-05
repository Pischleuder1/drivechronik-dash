package de.drivechronik.dash.routing

fun selectReroutedRoute(
    current: OfflineRoute,
    result: Result<OfflineRoute>
): OfflineRoute {
    val candidate = result.getOrNull() ?: return current
    val target = current.destination ?: current.points.lastOrNull()

    return candidate.takeIf {
        it.points.size >= 2 &&
            it.destination == target
    } ?: current
}
