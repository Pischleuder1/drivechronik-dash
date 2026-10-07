package de.drivechronik.dash

import android.util.AtomicFile
import java.io.File

internal object OfflineMapJournal {

    private fun journal(target: File): AtomicFile =
        AtomicFile(
            File(target.parentFile, target.name + ".txn")
        )

    fun read(target: File): OfflineMapTransaction? {
        val file = journal(target)
        if (!file.baseFile.exists() &&
            !File(file.baseFile.path + ".bak").exists()
        ) return null
        return OfflineMapTransaction.fromJson(
            file.readFully().toString(Charsets.UTF_8)
        )
    }

    fun write(
        target: File,
        transaction: OfflineMapTransaction
    ) {
        val file = journal(target)
        val output = file.startWrite()

        try {
            output.write(
                transaction.toJson()
                    .toByteArray(Charsets.UTF_8)
            )
            file.finishWrite(output)
        } catch (e: Exception) {
            file.failWrite(output)
            throw e
        }
    }

    fun clear(target: File) {
        journal(target).delete()
    }
}
