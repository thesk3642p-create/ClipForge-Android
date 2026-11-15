package com.clipforge.app.domain.parser

import android.net.Uri
import com.clipforge.app.domain.model.ClipDraft
import com.clipforge.app.domain.model.ParseResult
import org.json.JSONArray
import org.json.JSONObject

class BulkClipParser {
    private val urlRegex = Regex(
        "(?i)https?://(?:www\\.)?(?:youtube\\.com/(?:watch\\?[^\\s|<>()]+|shorts/[^\\s|<>()?]+|live/[^\\s|<>()?]+|embed/[^\\s|<>()?]+)|youtu\\.be/[^\\s|<>()?]+)"
    )
    private val timeRegex = Regex("(?<![\\d:])(?:\\d{1,3}:)?\\d{1,3}:\\d{2}(?![\\d:])")
    private val jsonTimeFields = listOf("start", "startTime", "start_time", "from")
    private val jsonEndFields = listOf("end", "endTime", "end_time", "to")

    fun parse(text: String): ParseResult {
        val input = text.trim()
        if (input.isEmpty()) return ParseResult(emptyList(), listOf("Paste one or more clip entries to begin."))

        val jsonCandidate = input.removePrefix("```json").removePrefix("```JSON")
            .removePrefix("```").removeSuffix("```").trim()
        if (jsonCandidate.startsWith('[') || jsonCandidate.startsWith('{')) {
            runCatching { parseJson(jsonCandidate) }.getOrNull()?.let { return it }
        }
        return parsePlainText(input)
    }

    fun parseSingle(label: String, url: String, start: String, end: String): ParseResult {
        val draft = makeDraft(label, url, start, end)
        return if (draft == null) {
            ParseResult(emptyList(), listOf("Check the YouTube link and enter a valid start and end time."))
        } else {
            ParseResult(listOf(draft), emptyList())
        }
    }

    private fun parseJson(source: String): ParseResult {
        val root = when {
            source.startsWith('[') -> JSONArray(source)
            else -> JSONObject(source).let { obj ->
                val clips = obj.optJSONArray("clips")
                if (clips != null) clips else JSONArray().put(obj)
            }
        }
        if (root.length() == 0) return ParseResult(emptyList(), listOf("The JSON array has no clip entries."))
        val clips = mutableListOf<ClipDraft>()
        val errors = mutableListOf<String>()
        for (index in 0 until root.length()) {
            val item = root.optJSONObject(index)
            if (item == null) {
                errors += "Entry ${index + 1}: expected an object with label, url, start, and end."
                continue
            }
            val label = firstString(item, listOf("label", "name", "title")) ?: "Clip ${index + 1}"
            val url = firstString(item, listOf("url", "link", "videoUrl", "video_url"))
            val start = firstString(item, jsonTimeFields)
            val end = firstString(item, jsonEndFields)
            val draft = if (url != null && start != null && end != null) makeDraft(label, url, start, end) else null
            if (draft == null) errors += "Entry ${index + 1}: invalid YouTube URL or timestamps."
            else clips += draft
        }
        return ParseResult(clips, errors)
    }

    private fun firstString(json: JSONObject, keys: List<String>): String? {
        for (key in keys) {
            if (!json.has(key) || json.isNull(key)) continue
            val value = json.opt(key)?.toString()?.trim().orEmpty()
            if (value.isNotEmpty() && value != "null") return value
        }
        return null
    }

    private fun parsePlainText(source: String): ParseResult {
        val clips = mutableListOf<ClipDraft>()
        val errors = mutableListOf<String>()
        source.lines().forEachIndexed { index, rawLine ->
            val line = rawLine.trim().removePrefix("- ").removePrefix("* ").trim()
            if (line.isBlank() || line.startsWith("```")) return@forEachIndexed
            val urlMatch = urlRegex.find(line)
            val times = timeRegex.findAll(line).map { it.value }.toList()
            if (urlMatch == null && times.isEmpty()) return@forEachIndexed
            if (urlMatch == null || times.size < 2) {
                errors += "Line ${index + 1}: couldn't find a YouTube URL and both timestamps."
                return@forEachIndexed
            }

            val parts = line.split('|').map(String::trim)
            val label = if (parts.size >= 3) parts.first().trim().removePrefix("**").removeSuffix("**")
                else line.substring(0, urlMatch.range.first).trim().trimEnd('-', '—', ':', '*').ifBlank { "Clip ${clips.size + 1}" }
            val draft = makeDraft(label, urlMatch.value.trimEnd('.', ',', ')', ']', '}', '"'), times[0], times[1])
            if (draft == null) errors += "Line ${index + 1}: invalid URL or time range (end must be after start)."
            else clips += draft
        }
        if (clips.isEmpty() && errors.isEmpty()) errors += "No clips found. Use `Label | YouTube URL | 00:10 - 00:25` or a JSON array."
        return ParseResult(clips, errors)
    }

    private fun makeDraft(label: String, rawUrl: String, start: String, end: String): ClipDraft? {
        val cleanedUrl = rawUrl.trim().trimEnd('.', ',', ')', ']', '}', '"')
        val normalized = normalizeYoutubeUrl(cleanedUrl) ?: return null
        val startSeconds = Timecode.parse(start) ?: return null
        val endSeconds = Timecode.parse(end) ?: return null
        if (endSeconds <= startSeconds) return null
        val cleanLabel = label.trim().replace(Regex("^\\d{1,3}[_\\s-]+"), "").ifBlank { "Clip" }
        return ClipDraft(cleanLabel, normalized, startSeconds, endSeconds)
    }

    private fun normalizeYoutubeUrl(value: String): String? {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val validHost = host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"
        if (!validHost || uri.scheme !in setOf("http", "https")) return null
        val validPath = when {
            host == "youtu.be" -> uri.lastPathSegment?.isNotBlank() == true
            uri.path == "/watch" -> uri.getQueryParameter("v")?.isNotBlank() == true
            else -> uri.pathSegments.firstOrNull() in setOf("shorts", "live", "embed") && uri.pathSegments.getOrNull(1).orEmpty().isNotBlank()
        }
        return uri.buildUpon().scheme("https").build().toString().takeIf { validPath }
    }
}
