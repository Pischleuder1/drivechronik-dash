package de.drivechronik.dash

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

internal object OfflineMapInstaller {

    internal fun target(
        context: Context,
        pkg: RemoteMapPackage
    ): File =
        OfflinePackageSupport.target(context, pkg)


    internal fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }


    internal var activateFile: (File, File) -> Boolean =
        { source, target -> source.renameTo(target) }

    internal var freeSpace: (File) -> Long = {
        android.os.StatFs(it.path).availableBytes
    }

    // Aufruf erst nach verifizierter Aktivierung der neuen Datei.
    // Auch nach einem Absturz beliebig oft wiederholbar.
    private fun finishCommittedBackup(
        destination: File,
        backup: File,
        rollback: File,
        previousHash: String?
    ) {
        if (previousHash == null) {
            check(!rollback.exists())
            return
        }
        if (rollback.isFile) {
            check(sha256(rollback).equals(previousHash, true)) {
                "Rollback-Datei beschädigt"
            }
            if (backup.exists()) {
                check(backup.isFile && backup.delete()) {
                    "Altes Backup nicht entfernbar"
                }
            }
            check(rollback.renameTo(backup)) {
                "Neues Backup konnte nicht aktiviert werden"
            }
        } else {
            // Bereits abgeschlossen oder nach Absturz wiederholt.
            check(backup.isFile &&
                sha256(backup).equals(previousHash, true)) {
                "Vorherige Version fehlt"
            }
        }
    }

    internal fun cleanupCommittedBackup(
        destination: File
    ): Boolean {
        val parent = destination.parentFile ?: return false
        val backup = File(parent, destination.name + ".backup")
        val rollback = File(parent, destination.name + ".rollback")

        if (OfflineMapJournal.read(destination) != null ||
            rollback.exists()
        ) {
            android.util.Log.w(
                "OfflineMaps",
                "Backup bleibt erhalten: Transaktion noch nicht sicher abgeschlossen"
            )
            return false
        }

        if (!backup.exists()) return true

        val deleted = backup.isFile && backup.delete()
        if (!deleted) {
            android.util.Log.w(
                "OfflineMaps",
                "Altes Backup konnte nicht entfernt werden: " +
                    backup.absolutePath
            )
        }

        return deleted
    }

    suspend fun install(
        context: Context,
        pkg: RemoteMapPackage,
        verified: File
    ) = withContext(Dispatchers.IO) {
        val destination = target(context, pkg)

        check(verified.isFile)
        check(verified.length() == pkg.sizeBytes)
        check(sha256(verified).equals(pkg.sha256, true))

        val parent = destination.parentFile
            ?: error("Zielverzeichnis fehlt")
        check(parent.isDirectory || parent.mkdirs())


        if (destination.isFile) {
            val previousHash = OfflineMapVersions.hash(
                context, pkg.id
            ) ?: error("Installierte Version unbekannt")

            check(sha256(destination).equals(previousHash, true)) {
                "Installierte Datei stimmt nicht mit dem Versionsstand überein"
            }
        }

        val staged = File(parent, destination.name + ".new")
        val backup = File(parent, destination.name + ".backup")
        val rollback = File(parent, destination.name + ".rollback")

        check(!staged.exists() && !rollback.exists()) {
            "Unvollständige Vorbereitung vorhanden"
        }

        check(OfflineMapJournal.read(destination) == null) {
            "Offene Transaktion vorhanden"
        }

        val reserve = 3L * 1024 * 1024 * 1024
        val required = pkg.sizeBytes + reserve
        check(freeSpace(parent) >= required) {
            "Nicht genügend freier Speicher"
        }


        // C bleibt Backup, bis D vollständig aktiviert und geprüft ist.
        check(!backup.exists() || (backup.isFile && destination.isFile)) {
            "Sicherung ohne aktive Karte – zuerst wiederherstellen"
        }

        try {
            verified.inputStream().use { input ->
                java.io.FileOutputStream(staged).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }

            check(staged.length() == pkg.sizeBytes)
            check(sha256(staged).equals(pkg.sha256, true)) {
                "Neue Datei fehlerhaft"
            }
        } catch (e: Exception) {
            staged.delete()
            throw e
        }

        check(OfflineMapJournal.read(destination) == null) {
            "Unvollständiges Update zuerst wiederherstellen"
        }

        OfflineMapJournal.write(
            destination,
            OfflineMapTransaction(
                packageId = pkg.id,
                previousHash = if (destination.exists())
                    OfflineMapVersions.hash(context, pkg.id)
                else null,
                previousVersion =
                    OfflineMapVersions.version(context, pkg.id),
                newHash = pkg.sha256,
                newVersion = pkg.version
            )
        )

        var activationVerified = false
        try {
            if (destination.exists()) {
                check(destination.renameTo(rollback)) {
                    "Rollback-Sicherung fehlgeschlagen"
                }
            }

            check(activateFile(staged, destination)) {
                "Aktivierung fehlgeschlagen"
            }
            check(sha256(destination).equals(pkg.sha256, true)) {
                "Aktivierte Datei beschädigt"
            }
            // Ab hier nie wieder D durch A ersetzen; bei Problemen
            // schließt recover() diese Transaktion nach dem Neustart ab.
            activationVerified = true
            finishCommittedBackup(
                destination, backup, rollback,
                OfflineMapJournal.read(destination)?.previousHash
            )
            OfflineMapVersions.saveVerified(
                context, pkg.id, pkg.version, pkg.sha256
            )
            OfflineMapJournal.clear(destination)
            cleanupCommittedBackup(destination)
        } catch (e: Exception) {
            if (!activationVerified) {
                val txn = OfflineMapJournal.read(destination)
                    ?: error("Transaktionsprotokoll fehlt")
                if (rollback.isFile) {
                    val oldHash = txn.previousHash
                        ?: error("Alte Prüfsumme fehlt")
                    check(sha256(rollback).equals(oldHash, true)) {
                        "Rollback-Sicherung beschädigt"
                    }
                    if (destination.exists()) {
                        check(destination.delete())
                    }
                    check(rollback.renameTo(destination)) {
                        "Wiederherstellung fehlgeschlagen"
                    }
                    check(txn.previousVersion != null)
                    OfflineMapVersions.saveVerified(
                        context, pkg.id, txn.previousVersion!!, oldHash
                    )
                } else {
                    // Bei einer Erstinstallation gab es noch kein A.
                    check(txn.previousHash == null)
                    if (destination.exists()) {
                        check(destination.delete())
                    }
                }
                OfflineMapJournal.clear(destination)
            }
            throw e
        } finally {
            staged.delete()
        }
    }

    fun recover(
        context: Context,
        destination: File,
        packageId: String
    ) {
        val parent = destination.parentFile
            ?: error("Zielverzeichnis fehlt")
        val backup = File(parent, destination.name + ".backup")
        val rollback = File(parent, destination.name + ".rollback")
        val staged = File(parent, destination.name + ".new")
        val txn = OfflineMapJournal.read(destination)
        var recoveredNewVersion = false

        if (txn != null) {
            check(txn.packageId == packageId)
            val actual = if (destination.isFile)
                sha256(destination) else null

            if (actual != null && actual.equals(txn.newHash, true)) {
                finishCommittedBackup(
                    destination, backup, rollback, txn.previousHash
                )
                OfflineMapVersions.saveVerified(
                    context, packageId, txn.newVersion, txn.newHash
                )
                recoveredNewVersion = true
            } else if (actual != null &&
                txn.previousHash != null &&
                actual.equals(txn.previousHash, true)) {
                check(!rollback.exists()) {
                    "Unerwartete zusätzliche Rollback-Datei"
                }
                val oldVersion = txn.previousVersion
                    ?: error("Alte Versionsnummer fehlt")
                OfflineMapVersions.saveVerified(
                    context, packageId, oldVersion, txn.previousHash!!
                )
            } else if (!destination.exists() && rollback.isFile) {
                val oldHash = txn.previousHash
                    ?: error("Alte Prüfsumme fehlt")
                check(sha256(rollback).equals(oldHash, true)) {
                    "Rollback-Datei beschädigt"
                }
                check(rollback.renameTo(destination)) {
                    "Wiederherstellung fehlgeschlagen"
                }
                OfflineMapVersions.saveVerified(
                    context, packageId,
                    txn.previousVersion ?: error("Alte Version fehlt"),
                    oldHash
                )
            } else if (!destination.exists() && backup.isFile) {
                // Abwärtskompatibel zu älteren Update-Transaktionen.
                val oldHash = txn.previousHash
                    ?: error("Alte Prüfsumme fehlt")
                check(sha256(backup).equals(oldHash, true)) {
                    "Sicherung beschädigt oder nicht zugeordnet"
                }
                check(backup.renameTo(destination))
                OfflineMapVersions.saveVerified(
                    context, packageId,
                    txn.previousVersion ?: error("Alte Version fehlt"),
                    oldHash
                )
            } else {
                check(!destination.exists() &&
                    txn.previousHash == null && !rollback.exists()) {
                    "Unbekannter Dateizustand – manuelle Prüfung nötig"
                }
            }
            // Erst nach vollständig erfolgreicher Wiederherstellung.
            if (staged.exists()) {
                check(staged.isFile && staged.delete()) {
                    "Temporäre Installationsdatei nicht entfernbar"
                }
            }
            OfflineMapJournal.clear(destination)
            if (recoveredNewVersion) {
                cleanupCommittedBackup(destination)
            }
            return
        }

        // Ohne Journal ist eine .rollback-Datei nicht sicher zuordenbar.
        check(!rollback.exists()) {
            "Rollback ohne Transaktionsprotokoll – nicht verändern"
        }
        if (destination.exists() || !backup.isFile) return
        val expected = OfflineMapVersions.hash(context, packageId)
            ?: error("Sicherung ohne bekannte Prüfsumme")
        check(sha256(backup).equals(expected, true)) {
            "Sicherung beschädigt oder unbekannte Version"
        }
        check(backup.renameTo(destination)) {
            "Wiederherstellung fehlgeschlagen"
        }
    }
}
