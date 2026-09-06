package com.gamilo.app.shipping

import com.gamilo.app.data.model.ShippingCarrier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

data class ParsedLabel(val carrier: ShippingCarrier?, val trackingNumber: String?)

/**
 * Runs entirely on-device via the bundled ML Kit model (see build.gradle.kts) — no network
 * call, no cloud OCR. [parse] takes an [InputImage] rather than a camera callback specifically
 * so instrumented tests can feed it a bundled sample label image
 * (androidTest/assets/shipping_labels/) instead of driving a real camera, which can't be
 * automated (see the master plan's Test Automation Limits).
 *
 * Carrier detection and tracking-number extraction are both best-effort heuristics over
 * common label formats, not an exhaustive spec of every carrier's numbering scheme — the
 * Shipping form always shows the result as editable fields the user can correct before
 * saving, never auto-submits.
 */
object ShippingLabelParser {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun parse(image: InputImage): ParsedLabel {
        val result = recognizer.process(image).await()
        return parseText(result.text)
    }

    /** Exposed separately so the carrier/regex heuristics are unit-testable without an image. */
    fun parseText(text: String): ParsedLabel {
        val carrier = detectCarrier(text)
        val trackingNumber = extractTrackingNumber(text, carrier)
        return ParsedLabel(carrier, trackingNumber)
    }

    private fun detectCarrier(text: String): ShippingCarrier? {
        val upper = text.uppercase()
        return when {
            upper.contains("CANADA POST") || upper.contains("POSTES CANADA") -> ShippingCarrier.CANADA_POST
            upper.contains("FEDEX") -> ShippingCarrier.FEDEX
            upper.contains("DHL") -> ShippingCarrier.DHL
            // Checked after FedEx/DHL: "UPS" is a common English-word substring elsewhere
            // ("SETUPS", "GROUPS"), so this only fires once those two are ruled out — real
            // UPS labels also print the distinctive "1Z..." tracking prefix as a backstop.
            upper.contains("UNITED PARCEL SERVICE") || Regex("\\b1Z[0-9A-Z]{16}\\b").containsMatchIn(upper) -> ShippingCarrier.UPS
            else -> null
        }
    }

    private fun extractTrackingNumber(text: String, carrier: ShippingCarrier?): String? {
        // Collapse spaces/tabs only — real labels often print a long number in spaced
        // groups ("1234 5678 9012 3456"); newlines are kept as boundaries between
        // unrelated printed lines.
        val compact = text.replace(Regex("[ \t]"), "")
        val patterns = when (carrier) {
            ShippingCarrier.CANADA_POST -> listOf(Regex("\\b\\d{16}\\b"))
            ShippingCarrier.FEDEX -> listOf(Regex("\\b\\d{12}\\b"), Regex("\\b\\d{15}\\b"))
            ShippingCarrier.UPS -> listOf(Regex("\\b1Z[0-9A-Z]{16}\\b"))
            ShippingCarrier.DHL -> listOf(Regex("\\b\\d{10,11}\\b"))
            ShippingCarrier.OTHER, null -> emptyList()
        }
        for (pattern in patterns) {
            pattern.find(compact)?.let { return it.value }
        }
        // Generic fallback for an unrecognized carrier or an unmatched format.
        return Regex("\\b\\d{10,22}\\b").find(compact)?.value
    }
}
