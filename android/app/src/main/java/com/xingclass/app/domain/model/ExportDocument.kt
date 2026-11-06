package com.xingclass.app.domain.model

/** A ready-to-share export produced on explicit user action. */
data class ExportDocument(
    val fileName: String,
    val mimeType: String,
    val content: String,
)
