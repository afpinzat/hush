package com.pinza.hush.data.local.model

data class LrcLine(
    val timeMillis: Long,
    val text: String
)

object LrcParser {
    // Regex para buscar [mm:ss.xx]
    private val regex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)".toRegex()

    fun parse(rawLrc: String): List<LrcLine> {
        return rawLrc.lineSequence()
            .mapNotNull { line ->
                val match = regex.find(line)
                if (match != null) {
                    val (min, sec, cent, text) = match.destructured
                    val time = (min.toLong() * 60000) + (sec.toLong() * 1000) + (cent.toLong() * 10)
                    LrcLine(time, text.trim())
                } else null
            }.toList()
    }
}