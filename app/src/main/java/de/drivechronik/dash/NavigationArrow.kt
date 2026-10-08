package de.drivechronik.dash

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.location.Location
import de.drivechronik.dash.data.TeslaDashboardRepository

internal fun navigationBearing(
    phone: Location?,
    gps: Pair<Double, Double>
): Float? {
    val s = TeslaDashboardRepository.snapshot.value
    val age = s.locationUpdatedAtNanos?.let {
        System.nanoTime() - it
    }
    val v = s.location

    if (age != null && age in 0L..120_000_000_000L &&
        v != null &&
        (v.latitude?.toDouble()?.let { kotlin.math.abs(it - gps.first) < 0.00001 } == true) &&
        (v.longitude?.toDouble()?.let { kotlin.math.abs(it - gps.second) < 0.00001 } == true)
    ) {
        return v.headingDegrees?.toFloat()
            ?.takeIf { it in 0f..360f }
    }

    return phone?.takeIf {
        it.hasBearing() && it.hasSpeed() &&
        it.speed >= 1.5f &&
        android.os.SystemClock.elapsedRealtimeNanos() -
            it.elapsedRealtimeNanos in 0L..120_000_000_000L
    }?.bearing
}

internal fun navigationArrowBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        80, 80, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    val path = Path().apply {
        moveTo(40f, 6f)
        lineTo(68f, 68f)
        lineTo(40f, 55f)
        lineTo(12f, 68f)
        close()
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.rgb(20, 38, 55)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 7f
    paint.strokeJoin = Paint.Join.ROUND
    canvas.drawPath(path, paint)
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    canvas.drawPath(path, paint)

    // Rotes, stilisiertes Tesla-T im Navigationspfeil.
    val logo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(232, 33, 39)
        style = Paint.Style.FILL
    }

    // Oberer geschwungener T-Balken.
    val top = Path().apply {
        moveTo(24f, 34f)
        quadTo(40f, 24f, 56f, 34f)
        lineTo(53f, 39f)
        quadTo(40f, 33f, 27f, 39f)
        close()
    }
    canvas.drawPath(top, logo)

    // Schmaler, nach unten auslaufender T-Schaft.
    val stem = Path().apply {
        moveTo(36f, 38f)
        quadTo(40f, 36f, 44f, 38f)
        lineTo(42f, 52f)
        quadTo(40f, 55f, 38f, 52f)
        close()
    }
    canvas.drawPath(stem, logo)

    return bitmap
}

internal fun teslaSuperchargerBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        72, 72, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    // Weißer Kreis mit dunkler Kontur als Kartenmarker.
    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(36f, 36f, 30f, background)

    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(40, 50, 60)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawCircle(36f, 36f, 30f, border)

    // Rotes stilisiertes Tesla-T.
    val logo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(232, 33, 39)
        style = Paint.Style.FILL
    }

    val top = Path().apply {
        moveTo(18f, 29f)
        quadTo(36f, 17f, 54f, 29f)
        lineTo(50f, 36f)
        quadTo(36f, 28f, 22f, 36f)
        close()
    }
    canvas.drawPath(top, logo)

    val stem = Path().apply {
        moveTo(31f, 34f)
        quadTo(36f, 31f, 41f, 34f)
        lineTo(39f, 53f)
        quadTo(36f, 57f, 33f, 53f)
        close()
    }
    canvas.drawPath(stem, logo)

    return bitmap
}

internal fun chargingStationBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        56, 56, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(58, 134, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(28f, 28f, 22f, background)

    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawCircle(28f, 28f, 22f, border)

    val bolt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    val path = Path().apply {
        moveTo(30f, 11f)
        lineTo(19f, 29f)
        lineTo(27f, 29f)
        lineTo(24f, 45f)
        lineTo(38f, 24f)
        lineTo(30f, 24f)
        close()
    }

    canvas.drawPath(path, bolt)

    return bitmap
}

internal fun fastChargingBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        60, 60, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 158, 11)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(30f, 30f, 24f, background)

    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawCircle(30f, 30f, 24f, border)

    val bolt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    val path = Path().apply {
        moveTo(32f, 12f)
        lineTo(20f, 31f)
        lineTo(29f, 31f)
        lineTo(26f, 48f)
        lineTo(41f, 26f)
        lineTo(32f, 26f)
        close()
    }

    canvas.drawPath(path, bolt)

    return bitmap
}
