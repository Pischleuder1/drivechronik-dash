package de.drivechronik.dash

import android.content.Context
import java.io.File

internal object OfflinePendingResolver {
    fun resolve(
        context: Context,
        id: String
    ): Pair<RemoteMapPackage, File>? {
        require(OfflinePackageSupport.isSupported(id))

        val sha = OfflineMapPending.pending(context, id)
            ?: return null
        val version = OfflineMapPending.pendingVersion(
            context, id
        ) ?: error("Vorgemerkte Version fehlt")

        val pkg = GitHubMapCatalog.readCached(context)
            .getOrThrow()
            .singleOrNull {
                it.id == id &&
                it.sha256.equals(sha, true) &&
                it.version == version
            } ?: error("Vormerkung passt nicht zum Katalog")

        check(pkg.sha256.matches(Regex("[a-fA-F0-9]{64}")))
        OfflinePackageSupport.validate(pkg)

        val base = context.getExternalFilesDir(null)
            ?: error("Kartenspeicher fehlt")
        val file = File(
            base,
            "map-downloads/$id/${sha.lowercase()}/" +
                "${pkg.fileName}.verified"
        )

        check(file.isFile && file.length() == pkg.sizeBytes) {
            "Vorgemerktes Paket fehlt oder ist unvollständig"
        }

        return pkg to file
    }
}
