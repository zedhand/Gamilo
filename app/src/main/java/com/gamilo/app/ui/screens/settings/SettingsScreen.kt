package com.gamilo.app.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamilo.app.data.model.Region
import com.gamilo.app.settings.GamiloSettings
import com.gamilo.app.ui.components.BorderedTextField
import com.gamilo.app.ui.components.GamiloButton
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens
import com.gamilo.app.ui.theme.GamiloThemeVariant
import com.gamilo.app.ui.theme.accentPreviewColor

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onRequestFactoryResetAuth: (onAuthenticated: () -> Unit) -> Unit) {
    val settings by viewModel.settings.collectAsState(initial = GamiloSettings.DEFAULT)

    var hourlyRateText by remember { mutableStateOf("") }
    var mileageRateText by remember { mutableStateOf("") }
    var fxRateText by remember { mutableStateOf("") }
    var gstRateText by remember { mutableStateOf("") }
    var pstRateText by remember { mutableStateOf("") }
    var ratesInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        if (!ratesInitialized) {
            hourlyRateText = settings.defaultHourlyRate.toPlainString()
            mileageRateText = settings.defaultMileageRatePerKm.toPlainString()
            fxRateText = settings.manualFxRateToCad.toPlainString()
            gstRateText = settings.defaultGstRate.toPlainString()
            pstRateText = settings.defaultPstRate.toPlainString()
            ratesInitialized = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GamiloColors.Background)
            .padding(GamiloDimens.ScreenPadding)
            .testTag("settings_list"),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { SectionLabel("Theme") }
        items(GamiloThemeVariant.entries) { variant ->
            ThemeRow(
                variant = variant,
                selected = variant == settings.themeVariant,
                onClick = { viewModel.setTheme(variant) },
            )
        }

        item { Spacer(Modifier.height(0.dp)) }
        item { SectionLabel("Region") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RegionRow("Canada (CAD)", settings.region == Region.CANADA) { viewModel.setRegion(Region.CANADA) }
                RegionRow("United States (USD)", settings.region == Region.UNITED_STATES) { viewModel.setRegion(Region.UNITED_STATES) }
            }
        }

        item { SectionLabel("Default Rates") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BorderedTextField("Hourly rate (${settings.baseCurrencyCode})", hourlyRateText, { hourlyRateText = it }, isNumeric = true)
                BorderedTextField("Mileage rate per km (${settings.baseCurrencyCode})", mileageRateText, { mileageRateText = it }, isNumeric = true)
                BorderedTextField("Manual FX rate to CAD", fxRateText, { fxRateText = it }, isNumeric = true)
                BorderedTextField("GST rate (e.g. 0.05)", gstRateText, { gstRateText = it }, isNumeric = true)
                BorderedTextField("PST rate (e.g. 0.07)", pstRateText, { pstRateText = it }, isNumeric = true)
                Text(
                    text = "SAVE RATES",
                    color = GamiloColors.Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                hourlyRateText.toBigDecimalOrNull()?.let { viewModel.setDefaultHourlyRate(it) }
                                mileageRateText.toBigDecimalOrNull()?.let { viewModel.setDefaultMileageRatePerKm(it) }
                                fxRateText.toBigDecimalOrNull()?.let { viewModel.setManualFxRateToCad(it) }
                                val gst = gstRateText.toBigDecimalOrNull()
                                val pst = pstRateText.toBigDecimalOrNull()
                                if (gst != null && pst != null) viewModel.setTaxRates(gst, pst)
                            },
                        ),
                )
            }
        }

        item { SectionLabel("Danger Zone") }
        item { DangerZoneSection(viewModel, onRequestFactoryResetAuth) }

        item { SectionLabel("Data Export") }
        item {
            val context = LocalContext.current
            var csvMessage by remember { mutableStateOf<String?>(null) }
            val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
                val stream = uri?.let { context.contentResolver.openOutputStream(it) }
                if (stream != null) {
                    viewModel.exportCsv(stream) { success ->
                        csvMessage = if (success) "CSV exported." else "Export failed."
                    }
                } else if (uri != null) {
                    csvMessage = "Export failed."
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GamiloButton(label = "Export CSV", onClick = { csvLauncher.launch("gamilo_export_${System.currentTimeMillis()}.csv") })
                csvMessage?.let { Text(text = it, color = GamiloColors.TextSecondary, fontSize = 12.sp) }
                Text(
                    text = "One combined CSV covering jobs, tasks, hours, expenses, mileage, shipping, and attachments — including soft-deleted rows, so a past record still reconciles against a historical tax filing even after it's been deleted from the app.",
                    color = GamiloColors.TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }

        item { SectionLabel("Backup") }
        item {
            val context = LocalContext.current
            var backupMessage by remember { mutableStateOf<String?>(null) }

            val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
                val stream = uri?.let { context.contentResolver.openOutputStream(it) }
                if (stream != null) {
                    viewModel.exportBackup(stream) { success ->
                        backupMessage = if (success) "Backup exported." else "Export failed."
                    }
                } else if (uri != null) {
                    backupMessage = "Export failed."
                }
            }
            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                val stream = uri?.let { context.contentResolver.openInputStream(it) }
                if (stream != null) {
                    viewModel.importBackup(stream) { success ->
                        if (success) restartApp(context) else backupMessage = "Import failed."
                    }
                } else if (uri != null) {
                    backupMessage = "Import failed."
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GamiloButton(label = "Export Backup", onClick = { exportLauncher.launch("gamilo_backup_${System.currentTimeMillis()}.db") })
                GamiloButton(label = "Import Backup", onClick = { importLauncher.launch(arrayOf("*/*")) })
                backupMessage?.let { Text(text = it, color = GamiloColors.TextSecondary, fontSize = 12.sp) }
                Text(
                    text = "Import replaces all current data and restarts the app. A backup only restores on the same device it was exported from — the file is encrypted with a key tied to this device's hardware.",
                    color = GamiloColors.TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/**
 * Factory reset is gated twice: a fresh biometric re-auth (distinct from the app's own
 * cold-start lock — [onRequestFactoryResetAuth]) to even reach the typed-confirmation step,
 * then typing "DELETE" exactly to enable the actual destructive button. Neither gate is
 * skippable, and cancelling either one resets back to the initial button with the typed text
 * cleared.
 */
@Composable
private fun DangerZoneSection(viewModel: SettingsViewModel, onRequestFactoryResetAuth: (onAuthenticated: () -> Unit) -> Unit) {
    val context = LocalContext.current
    var confirming by remember { mutableStateOf(false) }
    var typedConfirmation by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    if (!confirming) {
        GamiloButton(
            label = "Factory Reset / Wipe All Data",
            danger = true,
            onClick = { onRequestFactoryResetAuth { confirming = true } },
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "This permanently deletes ALL jobs, tasks, hours, expenses, mileage, and shipments on this device. This cannot be undone.",
                color = GamiloColors.Danger,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            BorderedTextField("Type DELETE to confirm", typedConfirmation, { typedConfirmation = it }, testTag = "danger_zone_confirm_input")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GamiloButton(
                    label = "Cancel",
                    modifier = Modifier.weight(1f),
                    onClick = { confirming = false; typedConfirmation = ""; resetMessage = null },
                )
                GamiloButton(
                    label = "Confirm Wipe",
                    danger = true,
                    enabled = typedConfirmation == "DELETE",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.factoryReset { success ->
                            if (success) restartApp(context) else resetMessage = "Factory reset failed."
                        }
                    },
                )
            }
            resetMessage?.let { Text(text = it, color = GamiloColors.Danger, fontSize = 12.sp) }
        }
    }
}

private fun restartApp(context: android.content.Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Runtime.getRuntime().exit(0)
}

private fun String.toBigDecimalOrNull(): java.math.BigDecimal? = runCatching { java.math.BigDecimal(this) }.getOrNull()

@Composable
private fun SectionLabel(text: String) {
    Text(text = text.uppercase(), color = GamiloColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}

@Composable
private fun ThemeRow(variant: GamiloThemeVariant, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(GamiloColors.Surface)
            .border(if (selected) 2.dp else GamiloDimens.BorderWidth, if (selected) GamiloColors.Accent else GamiloColors.Border)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(12.dp),
    ) {
        SwatchDot(variant)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = variant.label, color = GamiloColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = if (variant.isDark) "DARK" else "LIGHT", color = GamiloColors.TextSecondary, fontSize = 11.sp)
        }
        if (selected) Text(text = "ACTIVE", color = GamiloColors.Accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun SwatchDot(variant: GamiloThemeVariant) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(20.dp)
            .background(accentPreviewColor(variant))
            .border(GamiloDimens.BorderWidth, GamiloColors.Border),
    )
}

@Composable
private fun RegionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(GamiloDimens.TapTargetHeight)
            .background(GamiloColors.Surface)
            .border(if (selected) 2.dp else GamiloDimens.BorderWidth, if (selected) GamiloColors.Accent else GamiloColors.Border)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(text = label, color = GamiloColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (selected) Text(text = "ACTIVE", color = GamiloColors.Accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}
