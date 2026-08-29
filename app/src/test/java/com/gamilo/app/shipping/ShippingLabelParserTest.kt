package com.gamilo.app.shipping

import com.gamilo.app.data.model.ShippingCarrier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-logic tests for the carrier/tracking-number heuristics, independent of ML Kit and any
 * image — runs as a fast JVM unit test. [ShippingLabelParserImageTest] (androidTest) covers
 * the full OCR pipeline end-to-end against bundled sample label images.
 */
class ShippingLabelParserTest {

    @Test
    fun detectsCanadaPostAndExtractsSixteenDigitTracking() {
        val result = ShippingLabelParser.parseText("CANADA POST\nEXPEDIPARCEL\n1234 5678 9012 3456")
        assertEquals(ShippingCarrier.CANADA_POST, result.carrier)
        assertEquals("1234567890123456", result.trackingNumber)
    }

    @Test
    fun detectsFedExAndExtractsTwelveDigitTracking() {
        val result = ShippingLabelParser.parseText("FedEx Express\nTRK NBR\n1234 5678 9012")
        assertEquals(ShippingCarrier.FEDEX, result.carrier)
        assertEquals("123456789012", result.trackingNumber)
    }

    @Test
    fun detectsUpsFromOneZPrefix_evenWithoutTheWordUps() {
        val result = ShippingLabelParser.parseText("Tracking Number\n1Z999AA10123456784")
        assertEquals(ShippingCarrier.UPS, result.carrier)
        assertEquals("1Z999AA10123456784", result.trackingNumber)
    }

    @Test
    fun detectsDhlAndExtractsTenDigitTracking() {
        val result = ShippingLabelParser.parseText("DHL EXPRESS\nWaybill\n1234567890")
        assertEquals(ShippingCarrier.DHL, result.carrier)
        assertEquals("1234567890", result.trackingNumber)
    }

    @Test
    fun fedExWinsOverUps_whenBothWordsAppearOnTheSameLabel() {
        // "FedEx" text mentioning a "Group" partner or similar containing "ups" as a substring
        // must not misfire UPS — FedEx/DHL are checked first specifically for this reason.
        val result = ShippingLabelParser.parseText("FEDEX GROUPS SHIPPING\n123456789012")
        assertEquals(ShippingCarrier.FEDEX, result.carrier)
    }

    @Test
    fun unknownCarrier_fallsBackToGenericDigitRun() {
        val result = ShippingLabelParser.parseText("SomeRegionalCourier\nRef: 9876543210")
        assertNull(result.carrier)
        assertEquals("9876543210", result.trackingNumber)
    }

    @Test
    fun noDigitsAnywhere_returnsNullTrackingNumber() {
        val result = ShippingLabelParser.parseText("Handle with care\nFragile")
        assertNull(result.carrier)
        assertNull(result.trackingNumber)
    }
}
