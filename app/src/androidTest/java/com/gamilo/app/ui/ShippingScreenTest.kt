package com.gamilo.app.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.core.SystemClock
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.ShippingRepository
import com.gamilo.app.ui.screens.shipping.ShippingScreen
import com.gamilo.app.ui.screens.shipping.ShippingViewModel
import com.gamilo.app.ui.theme.GamiloTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ShippingScreenTest {

    @get:Rule
    val screenRule = ScreenTestRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addingAShipment_showsInListWithTrackAndCopyLinks() {
        val viewModel = ShippingViewModel(
            ShippingRepository(screenRule.database.shippingDao(), SystemClock),
            screenRule.settingsStore,
            JobRepository(screenRule.database.jobDao(), SystemClock),
        )
        composeRule.setContent { GamiloTheme { ShippingScreen(viewModel, GlobalFilter(), onFilterChange = {}) } }

        // Default carrier selection (Canada Post) is left as-is — only manual entry is
        // exercised here; the camera/OCR path can't be automated (see ShippingLabelParser*Test).
        // The form has many fields, so the LazyColumn only composes what's near the viewport —
        // performScrollToNode (not the plain performScrollTo used on the shorter tab screens)
        // scrolls the list step by step until each target is actually composed.
        val list = composeRule.onNodeWithTag("shipping_form_list")
        list.performScrollToNode(hasTestTag("tracking_number_input"))
        composeRule.onNodeWithTag("tracking_number_input").performTextInput("1234567890123456")
        composeRule.onNodeWithTag("shipping_cost_input").performTextInput("22.50")
        list.performScrollToNode(hasText("ADD SHIPMENT"))
        composeRule.onNodeWithText("ADD SHIPMENT").performClick()
        composeRule.waitForIdle()

        list.performScrollToNode(hasText("CANADA POST · 1234567890123456"))
        composeRule.onNodeWithText("CANADA POST · 1234567890123456").assertExists()
        composeRule.onNodeWithText("TRACK").assertExists()
        composeRule.onNodeWithText("COPY SNIPPET").assertExists()

        list.performScrollToNode(hasText("DELETE"))
        composeRule.onNodeWithText("DELETE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("CANADA POST · 1234567890123456").assertDoesNotExist()
    }
}
