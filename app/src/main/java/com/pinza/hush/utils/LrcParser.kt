package com.pinza.hush.utils

import java.util.regex.Pattern

data class LrcLine(
    val time: Long,
    val text: String
)

object LrcParser {
    private val linePattern = Pattern.compile("\\[(\\d{1,2}):(\\d{2})[.:](\\d{2,3})\\](.*)")

    fun parse(lrcContent: String): List<LrcLine> {
        if (lrcContent.isBlank()) return emptyList()
        
        val lines = lrcContent.lines()
        val result = mutableListOf<LrcLine>()
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
                result.add(LrcLine(time, text))
                hasTimedLines = true
            }
        }

        // Si no se encontraron etiquetas de tiempo, tratar como texto plano
        if (!hasTimedLines && lrcContent.isNotBlank()) {
            lrcContent.lines().filter { it.isNotBlank() }.forEachIndexed { index, text ->
                // Asignamos tiempos ficticios o simplemente 0 para que aparezcan
                result.add(LrcLine(index * 1000L, text.trim()))
            }
        }

        return result.sortedBy { it.time }
    }
}
