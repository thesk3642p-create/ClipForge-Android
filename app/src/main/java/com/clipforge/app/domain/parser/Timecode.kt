package com.clipforge.app.domain.parser

import java.util.Locale

object Timecode {
    fun parse(value: String): Long? {
        val parts = value.trim().split(':')
        if (parts.size !in 2..3 || parts.any { part -> part.isEmpty() || part.any { !it.isDigit() } }) return null
        val numbers = parts.map { it.toLongOrNull() ?: return null }
        return runCatching {
            val seconds = if (numbers.size == 2) {
                val (minutes, secondPart) = numbers
                if (secondPart !in 0..59) return null
                Math.addExact(Math.multiplyExact(minutes, 60L), secondPart)
            } else {
                val (hours, minutes, secondPart) = numbers
                if (minutes !in 0..59 || secondPart !in 0..59) return null
                Math.addExact(Math.addExact(Math.multiplyExact(hours, 3600L), Math.multiplyExact(minutes, 60L)), secondPart)
            }
            seconds.takeIf { it >= 0L }
        }.getOrNull()
    }

    fun format(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
