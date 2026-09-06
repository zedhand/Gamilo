package com.gamilo.app.ai

/**
 * Parses a transcribed voice note into a draft new [com.gamilo.app.data.entity.JobEntity] for
 * the user to review before saving — never auto-commits. This is a deliberately simple, fully
 * offline rule-based extractor (regex/keyword matching), not a neural SLM: Gamilo can't bundle
 * or verify a specific third-party on-device LLM/model file sight unseen, so this ships a
 * genuinely working "voice accelerator" now rather than a stub waiting on an unverifiable
 * dependency. A real on-device LLM could replace this later behind the same [extract] signature
 * without touching any caller.
 *
 * Example: "New job for Jane Smith to replace the kitchen faucet" extracts
 * clientName="Jane Smith", title="replace the kitchen faucet". Any trailing appointment-booking
 * clause (e.g. "...appointment booked for Tuesday at 11am") is deliberately excluded from both
 * fields — Gamilo has no calendar/appointment entity yet — but the full, unedited sentence is
 * always preserved in [Draft.rawText] so nothing the user said is ever lost, even what wasn't
 * (yet) structured.
 */
object JobVoiceExtractor {

    data class Draft(
        val rawText: String,
        val clientName: String?,
        val title: String?,
    )

    private val appointmentClauseStart = Regex("""\b(appointment|scheduled|booked)\b""", RegexOption.IGNORE_CASE)
    private val forToPattern = Regex("""(?:new\s+job\s+for|job\s+for)\s+(.+?)\s+to\s+(.+)""", RegexOption.IGNORE_CASE)
    private val forOnlyPattern = Regex("""(?:new\s+job\s+for|job\s+for)\s+(.+)""", RegexOption.IGNORE_CASE)

    fun extract(text: String): Draft {
        val trimmed = text.trim()
        val jobPortion = appointmentClauseStart.find(trimmed)
            ?.let { trimmed.substring(0, it.range.first) }
            ?: trimmed
        val cleanedJobPortion = jobPortion.trim().trimEnd('.', ',', ' ')

        forToPattern.find(cleanedJobPortion)?.let { match ->
            return Draft(
                rawText = trimmed,
                clientName = cleanTrailing(match.groupValues[1]),
                title = cleanTrailing(match.groupValues[2]),
            )
        }
        forOnlyPattern.find(cleanedJobPortion)?.let { match ->
            return Draft(rawText = trimmed, clientName = cleanTrailing(match.groupValues[1]), title = null)
        }

        return Draft(rawText = trimmed, clientName = null, title = cleanedJobPortion.ifBlank { null })
    }

    private fun cleanTrailing(value: String): String? = value.trim().trimEnd('.', ',', ' ').ifBlank { null }
}
