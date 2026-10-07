package de.drivechronik.dash

import android.content.Context
import java.security.MessageDigest

internal object OfflineDownloadSelfTest {

    private val bytes =
        "DRIVECHRONIK_OFFLINE_DOWNLOAD_SELFTEST_2026".toByteArray()

    private fun sha256(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(data)
            .joinToString("") {
                "%02x".format(it.toInt() and 255)
            }

    fun packageFor(url: String): RemoteMapPackage {
        val sha = sha256(bytes)

        val part = RemoteMapPart(
            name = "selftest.part00",
            sizeBytes = bytes.size.toLong(),
            sha256 = sha,
            url = url
        )

        return RemoteMapPackage(
            id = "selftest-map",
            country = "xx",
            countryName = "Selftest",
            kind = "map",
            version = "selftest",
            sizeBytes = bytes.size.toLong(),
            sha256 = sha,
            fileName = "selftest.bin",
            parts = listOf(part)
        )
    }

    fun clear(context: Context, pkg: RemoteMapPackage) {
        context.getExternalFilesDir(null)
            ?.resolve(
                "map-downloads/${pkg.id}/${pkg.sha256.lowercase()}"
            )
            ?.deleteRecursively()
    }

    suspend fun run(context: Context): String {
        val url =
            "https://github.com/Pischleuder1/drivechronik-maps/" +
            "releases/download/selftest-2026-10/selftest.part00"

        val pkg = packageFor(url)

        return try {
            val part = pkg.parts.single()

            OfflineMapDownloader.downloadPart(
                context, pkg, part
            ) { _, _ -> }

            val verified = OfflineMapAssembler.assemble(
                context, pkg
            )

            check(verified.isFile)
            check(verified.length() == 43L)
            check(verified.readBytes().contentEquals(bytes)) {
                "Selftest-Inhalt stimmt nicht"
            }

            "Download-Selbsttest erfolgreich"
        } finally {
            clear(context, pkg)
        }
    }

}
