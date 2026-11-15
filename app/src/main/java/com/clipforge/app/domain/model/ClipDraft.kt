package com.clipforge.app.domain.model

data class ClipDraft(
    val label: String,
    val url: String,
    val startSeconds: Long,
    val endSeconds: Long,
) {
    val durationSeconds: Long get() = endSeconds - startSeconds
    val isValid: Boolean get() = label.isNotBlank() && url.isNotBlank() && startSeconds >= 0 && endSeconds > startSeconds
}

data class ParseResult(
    val clips: List<ClipDraft>,
    val errors: List<String>,
)
