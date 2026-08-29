package com.gamilo.app.shipping

import com.gamilo.app.data.model.ShippingCarrier

/** Carrier tracking-page URLs and the client-facing clipboard snippet, built from a shipment. */
object ShippingLinks {

    fun trackingUrl(carrier: ShippingCarrier, trackingNumber: String): String? = when (carrier) {
        ShippingCarrier.CANADA_POST -> "https://www.canadapost-postescanada.ca/track-reperage/en#/search?searchFor=$trackingNumber"
        ShippingCarrier.FEDEX -> "https://www.fedex.com/fedextrack/?trknbr=$trackingNumber"
        ShippingCarrier.UPS -> "https://www.ups.com/track?tracknum=$trackingNumber"
        ShippingCarrier.DHL -> "https://www.dhl.com/en/express/tracking.html?AWB=$trackingNumber"
        ShippingCarrier.OTHER -> null
    }

    /** A ready-to-paste message for the client — 1-tap "Copy Client Snippet" in the Shipping tab. */
    fun clientSnippet(carrier: ShippingCarrier, trackingNumber: String): String {
        val carrierLabel = carrier.name.replace('_', ' ').lowercase()
            .split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        val url = trackingUrl(carrier, trackingNumber)
        return buildString {
            append("Your order has shipped via $carrierLabel.\n")
            append("Tracking #: $trackingNumber")
            if (url != null) append("\nTrack here: $url")
        }
    }
}
