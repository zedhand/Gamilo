package com.gamilo.app.shipping

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Creates a fresh cache-dir file for a full-resolution label photo and returns its content:// URI. */
fun createScanImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "scans").apply { mkdirs() }
    val file = File(dir, "label_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
