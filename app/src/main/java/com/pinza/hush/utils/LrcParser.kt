package com.pinza.hush.utils

import java.util.regex.Pattern

data class LrcLine(
    val time: Long,
    val text: String,
    val translation: String? = null
)

object LrcParser {
    private val linePattern = Pattern.compile("\\[(\\d{1,2}):(\\d{2})[.:](\\d{2,3})\\](.*)")

    fun parse(lrcContent: String): List<LrcLine> {
        if (lrcContent.isBlank()) return emptyList()
        
        val lines = lrcContent.lines()
        val result = mutableMapOf<Long, LrcLine>()
        var hasTimedLines = false

        for (line in lines) {
            val matcher = linePattern.matcher(line.trim())
            if (matcher.find()) {
                val min = matcher.group(1)?.toLong() ?: 0L
                val sec = matcher.group(2)?.toLong() ?: 0L
                val msStr = matcher.group(3) ?: "0"
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val text = matcher.group(4)?.trim() ?: ""

                val time = (min * 60 * 1000) + (sec * 1000) + ms
                
                // If the time already exists, it's likely a translation line in Spotify-style dual LRC
                if (result.containsKey(time)) {
                    val existing = result[time]!!
                    result[time] = existing.copy(translation = text)
                } else {
                    result[time] = LrcLine(time, text)
                }
                hasTimedLines = true
            }
        }

        // Si no se encontraron etiquetas de tiempo, tratar como texto plano
        if (!hasTimedLines && lrcContent.isNotBlank()) {
            val plainLines = lrcContent.lines().filter { it.isNotBlank() }
            return plainLines.mapIndexed { index, text ->
                LrcLine(index * 1000L, text.trim())
            }
        }

        return result.values.sortedBy { it.time }
    }
}
