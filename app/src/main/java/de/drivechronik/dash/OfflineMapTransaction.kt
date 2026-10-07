package de.drivechronik.dash

import org.json.JSONObject

internal data class OfflineMapTransaction(
    val packageId: String,
    val previousHash: String?,
    val previousVersion: String?,
    val newHash: String,
    val newVersion: String
) {
    fun toJson(): String = JSONObject().apply {
        put("packageId", packageId)
        put("previousHash", previousHash)
        put("previousVersion", previousVersion)
        put("newHash", newHash)
        put("newVersion", newVersion)
    }.toString()

    companion object {
        fun fromJson(text: String): OfflineMapTransaction {
            val json = JSONObject(text)
            return OfflineMapTransaction(
                packageId = json.getString("packageId"),
                previousVersion = json.optString(
                    "previousVersion"
                ).ifEmpty { null },
                previousHash = json.optString(
                    "previousHash"
                ).ifEmpty { null },
                newHash = json.getString("newHash"),
                newVersion = json.getString("newVersion")
            )
        }
    }
}
