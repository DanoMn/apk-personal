package dev.panopt.autonomia.domain.scoring

import java.util.Locale

internal object ScoreSnapshotJson {
    fun layersJson(layers: List<LayerScore>): String =
        layers.joinToString(prefix = "[", postfix = "]") { layer ->
            buildString {
                append("{")
                appendStringField("layerId", layer.layerId)
                append(",")
                appendStringField("name", layer.name)
                append(",")
                appendBooleanField("configured", layer.configured)
                append(",")
                appendNumberField("score", layer.score)
                append(",")
                appendNumberField("baseScore", layer.baseScore)
                append(",")
                appendNumberField("rawScore", layer.rawScore)
                append(",")
                appendNullableNumberField("anchorScore", layer.anchorScore)
                append(",")
                appendNullableNumberField("supportScore", layer.supportScore)
                append(",")
                appendNumberField("anchorSurplusBonus", layer.anchorSurplusBonus)
                append(",")
                appendNumberField("taskMomentumBonus", layer.taskMomentumBonus)
                append(",")
                appendNullableNumberField("sleepScore", layer.sleepScore)
                append(",")
                appendNullableNumberField("sobrietyScore", layer.sobrietyScore)
                append("}")
            }
        }

    fun reasonsJson(reasons: List<String>): String =
        reasons.joinToString(prefix = "[", postfix = "]") { reason ->
            "\"${reason.escapeJson()}\""
        }

    private fun StringBuilder.appendStringField(name: String, value: String) {
        append("\"")
        append(name)
        append("\":\"")
        append(value.escapeJson())
        append("\"")
    }

    private fun StringBuilder.appendBooleanField(name: String, value: Boolean) {
        append("\"")
        append(name)
        append("\":")
        append(value)
    }

    private fun StringBuilder.appendNumberField(name: String, value: Float) {
        append("\"")
        append(name)
        append("\":")
        append(FLOAT_FORMAT.format(value))
    }

    private fun StringBuilder.appendNullableNumberField(name: String, value: Float?) {
        append("\"")
        append(name)
        append("\":")
        append(value?.let { FLOAT_FORMAT.format(it) } ?: "null")
    }

    private fun String.escapeJson(): String =
        buildString {
            this@escapeJson.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }

    private val FLOAT_FORMAT = "%.3f".let { pattern ->
        object {
            fun format(value: Float): String = String.format(Locale.US, pattern, value)
        }
    }
}
