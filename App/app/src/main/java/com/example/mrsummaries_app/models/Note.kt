package com.example.mrsummaries_app.models

import java.io.Serializable
import java.util.Date
import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var content: String = "",
    var folderId: String? = null,
    var lastModified: Date = Date(),
    var drawingData: ByteArray? = null
) : Serializable {
    // Override equals and hashCode because of the ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Note

        if (id != other.id) return false
        if (title != other.title) return false
        if (content != other.content) return false
        if (folderId != other.folderId) return false
        if (lastModified != other.lastModified) return false
        if (drawingData != null) {
            if (other.drawingData == null) return false
            if (!drawingData.contentEquals(other.drawingData)) return false
        } else if (other.drawingData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + (folderId?.hashCode() ?: 0)
        result = 31 * result + lastModified.hashCode()
        result = 31 * result + (drawingData?.contentHashCode() ?: 0)
        return result
    }
}