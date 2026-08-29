package com.gamilo.app.shipping

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.LargeTest
import com.gamilo.app.data.model.ShippingCarrier
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Feeds bundled sample label images (androidTest/assets/shipping_labels/, generated — real
 * photographed labels weren't available — but rendered as real raster text, not synthetic
 * strings) through the actual on-device ML Kit recognizer end-to-end, bypassing the camera
 * intent entirely per the master plan's Test Automation Limits. [ShippingLabelParserTest]
 * (JVM unit test) covers the carrier/regex heuristics themselves in isolation.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ShippingLabelParserImageTest {

    private fun loadAsset(name: String): InputImage {
        // Test-only assets (androidTest/assets/) are packaged into the separate instrumentation
        // (test) APK, not the app-under-test APK — ApplicationProvider.getApplicationContext()
        // returns the latter's Context, whose AssetManager can't see them. The instrumentation
        // Context is the one whose AssetManager actually has this file.
        val context = InstrumentationRegistry.getInstrumentation().context
        val bitmap = context.assets.open("shipping_labels/$name").use { BitmapFactory.decodeStream(it) }
        return InputImage.fromBitmap(bitmap, 0)
    }

    @Test
    fun canadaPostLabel_isRecognizedEndToEnd() = runTest {
        val result = ShippingLabelParser.parse(loadAsset("canada_post.png"))
        assertEquals(ShippingCarrier.CANADA_POST, result.carrier)
        assertEquals("1234567890123456", result.trackingNumber)
    }

    @Test
    fun fedExLabel_isRecognizedEndToEnd() = runTest {
        val result = ShippingLabelParser.parse(loadAsset("fedex.png"))
        assertEquals(ShippingCarrier.FEDEX, result.carrier)
        assertEquals("123456789012", result.trackingNumber)
    }

    @Test
    fun upsLabel_isRecognizedEndToEnd() = runTest {
        val result = ShippingLabelParser.parse(loadAsset("ups.png"))
        assertEquals(ShippingCarrier.UPS, result.carrier)
        assertEquals("1Z999AA10123456784", result.trackingNumber)
    }

    @Test
    fun dhlLabel_isRecognizedEndToEnd() = runTest {
        val result = ShippingLabelParser.parse(loadAsset("dhl.png"))
        assertEquals(ShippingCarrier.DHL, result.carrier)
        assertEquals("1234567890", result.trackingNumber)
    }
}
