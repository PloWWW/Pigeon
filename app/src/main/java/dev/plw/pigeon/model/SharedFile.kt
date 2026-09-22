package dev.plw.pigeon.model

import android.net.Uri

data class SharedFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long
) {
    val sizeMb: String
        get() = "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))
}