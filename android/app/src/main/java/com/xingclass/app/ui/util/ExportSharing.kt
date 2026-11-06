package com.xingclass.app.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.xingclass.app.R
import com.xingclass.app.domain.model.ExportDocument
import java.io.File

/**
 * Writes an export to the app cache and opens the system share sheet through a FileProvider.
 * The file is not world-readable; the receiving app gets a temporary read grant.
 */
fun shareExportDocument(context: Context, document: ExportDocument) {
    val directory = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(directory, document.fileName)
    file.writeText(document.content)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        type = document.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, document.fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(send, context.getString(R.string.settings_export_share_title)),
    )
}
