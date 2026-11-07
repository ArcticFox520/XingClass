package com.xingclass.app.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.xingclass.app.R
import com.xingclass.app.domain.model.ExportDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val EXPORT_RETENTION_MILLIS = 24L * 60 * 60 * 1000

/**
 * Writes an export to the app cache on an IO thread and opens the system share sheet through a
 * FileProvider. The file is not world-readable; the receiving app gets a temporary read grant.
 */
suspend fun shareExportDocument(context: Context, document: ExportDocument) {
    val uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        // Drop exports left over from earlier shares so the cache does not grow unbounded.
        val cutoff = System.currentTimeMillis() - EXPORT_RETENTION_MILLIS
        directory.listFiles()?.forEach { old ->
            if (old.isFile && old.lastModified() < cutoff) old.delete()
        }
        val file = File(directory, document.fileName)
        file.writeText(document.content)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }
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
