package com.gamilo.app.ui.screens.shipping

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.data.entity.ShippingEntity
import com.gamilo.app.data.model.CoverageParty
import com.gamilo.app.data.model.ShippingCarrier
import com.gamilo.app.shipping.ShippingLinks
import com.gamilo.app.shipping.createScanImageUri
import com.gamilo.app.ui.components.BorderedTextField
import com.gamilo.app.ui.components.FilterBarHost
import com.gamilo.app.ui.components.GamiloButton
import com.gamilo.app.ui.components.JobPickerSection
import com.gamilo.app.ui.components.ListRowCard
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens
import com.gamilo.app.ui.theme.MonospaceNumeric
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun ShippingScreen(viewModel: ShippingViewModel, filter: GlobalFilter, onFilterChange: (GlobalFilter) -> Unit) {
    val entries by viewModel.entries.collectAsState(initial = emptyList())
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())
    val jobsById = remember(jobs) { jobs.associateBy { it.id } }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    var carrier by remember { mutableStateOf(ShippingCarrier.CANADA_POST) }
    var trackingNumber by remember { mutableStateOf("") }
    var shippingCostText by remember { mutableStateOf("") }
    var insuranceCostText by remember { mutableStateOf("") }
    var declaredValueText by remember { mutableStateOf("") }
    var lengthText by remember { mutableStateOf("") }
    var widthText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var coverage by remember { mutableStateOf(CoverageParty.SELLER) }
    var selectedJobId by remember { mutableStateOf<Long?>(null) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingUri
        if (success && uri != null) {
            scope.launch {
                val parsed = runCatching { InputImage.fromFilePath(context, uri) }
                    .mapCatching { viewModel.scanLabel(it) }
                    .getOrNull()
                if (parsed?.carrier != null || parsed?.trackingNumber != null) {
                    parsed.carrier?.let { carrier = it }
                    parsed.trackingNumber?.let { trackingNumber = it }
                    scanMessage = "Scanned — review before saving."
                } else {
                    scanMessage = "Could not read a tracking number — enter manually."
                }
            }
        }
    }

    fun launchCamera() {
        val uri = createScanImageUri(context)
        pendingUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    LaunchedEffect(filter) { viewModel.setFilter(filter) }

    // Wrapped in an explicit Box/Column (not left as bare siblings): FilterBarHost's
    // conditional full-screen picker overlay needs a real parent layout to overlay
    // correctly alongside the list below it — see StickyFilterBar.kt.
    Box(modifier = Modifier.fillMaxSize()) { Column(modifier = Modifier.fillMaxSize()) {
    FilterBarHost(filter = filter, jobs = jobs, onFilterChange = onFilterChange)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GamiloColors.Background)
            .padding(GamiloDimens.ScreenPadding)
            .testTag("shipping_form_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GamiloButton(
                label = "Scan Label",
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
            )
        }
        scanMessage?.let { message ->
            item { Text(text = message, color = GamiloColors.TextSecondary, fontSize = 12.sp) }
        }

        item { SectionLabel("Carrier") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShippingCarrier.entries.forEach { c ->
                    CarrierRow(c, selected = carrier == c, onClick = { carrier = c })
                }
            }
        }

        item {
            BorderedTextField(label = "Tracking number", value = trackingNumber, onValueChange = { trackingNumber = it }, testTag = "tracking_number_input")
        }
        item {
            BorderedTextField(label = "Shipping cost", value = shippingCostText, onValueChange = { shippingCostText = it }, isNumeric = true, testTag = "shipping_cost_input")
        }
        item {
            BorderedTextField(label = "Insurance cost", value = insuranceCostText, onValueChange = { insuranceCostText = it }, isNumeric = true)
        }
        item {
            BorderedTextField(label = "Declared value", value = declaredValueText, onValueChange = { declaredValueText = it }, isNumeric = true)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BorderedTextField("Length (cm)", lengthText, { lengthText = it }, isNumeric = true, modifier = Modifier.weight(1f))
                BorderedTextField("Width (cm)", widthText, { widthText = it }, isNumeric = true, modifier = Modifier.weight(1f))
                BorderedTextField("Height (cm)", heightText, { heightText = it }, isNumeric = true, modifier = Modifier.weight(1f))
            }
        }

        item { SectionLabel("Coverage") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CoverageRow("Seller pays", selected = coverage == CoverageParty.SELLER) { coverage = CoverageParty.SELLER }
                CoverageRow("Client pays", selected = coverage == CoverageParty.CLIENT) { coverage = CoverageParty.CLIENT }
            }
        }

        item { SectionLabel("Job") }
        item { JobPickerSection(jobs = jobs, selectedJobId = selectedJobId, onSelect = { selectedJobId = it }) }

        item {
            GamiloButton(
                label = "Add Shipment",
                onClick = {
                    val cost = shippingCostText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val insurance = insuranceCostText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val declared = declaredValueText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val length = lengthText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val width = widthText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val height = heightText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    if (trackingNumber.isNotBlank()) {
                        viewModel.addShipment(carrier, trackingNumber, cost, insurance, declared, length, width, height, coverage, selectedJobId)
                        trackingNumber = ""
                        shippingCostText = ""
                        insuranceCostText = ""
                        declaredValueText = ""
                        lengthText = ""
                        widthText = ""
                        heightText = ""
                        selectedJobId = null
                        scanMessage = null
                    }
                },
            )
        }

        if (entries.isEmpty()) {
            item { Text(text = "No shipments logged yet.", color = GamiloColors.TextSecondary, fontSize = 13.sp) }
        } else {
            items(entries, key = { it.id }) { shipment ->
                ShipmentRow(
                    shipment = shipment,
                    jobTitle = shipment.jobId?.let { jobsById[it]?.title },
                    onTrack = {
                        ShippingLinks.trackingUrl(shipment.carrier, shipment.trackingNumber)?.let { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                    onCopySnippet = {
                        val snippet = ShippingLinks.clientSnippet(shipment.carrier, shipment.trackingNumber)
                        scope.launch {
                            clipboard.setClipEntry(androidx.compose.ui.platform.ClipEntry(ClipData.newPlainText("Tracking snippet", snippet)))
                        }
                    },
                    onDelete = { viewModel.deleteShipment(shipment.id) },
                )
            }
        }
    }
    } }
}

private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

@Composable
private fun SectionLabel(text: String) {
    Text(text = text.uppercase(), color = GamiloColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}

@Composable
private fun CarrierRow(carrier: ShippingCarrier, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(GamiloDimens.TapTargetHeight)
            .background(GamiloColors.Surface)
            .border(if (selected) 2.dp else GamiloDimens.BorderWidth, if (selected) GamiloColors.Accent else GamiloColors.Border)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = carrier.name.replace('_', ' '), color = GamiloColors.TextPrimary, fontSize = 14.sp)
        if (selected) Text(text = "ACTIVE", color = GamiloColors.Accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun CoverageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(GamiloDimens.TapTargetHeight)
            .background(GamiloColors.Surface)
            .border(if (selected) 2.dp else GamiloDimens.BorderWidth, if (selected) GamiloColors.Accent else GamiloColors.Border)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = GamiloColors.TextPrimary, fontSize = 14.sp)
        if (selected) Text(text = "ACTIVE", color = GamiloColors.Accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun ShipmentRow(shipment: ShippingEntity, jobTitle: String?, onTrack: () -> Unit, onCopySnippet: () -> Unit, onDelete: () -> Unit) {
    ListRowCard {
        Text(text = "${shipment.carrier.name.replace('_', ' ')} · ${shipment.trackingNumber}", color = GamiloColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(
            text = "${shipment.shippingCost} ${shipment.currencyCode} · ${shipment.coverage.name} pays" + (jobTitle?.let { " · $it" } ?: ""),
            color = GamiloColors.TextSecondary,
            fontFamily = MonospaceNumeric,
            fontSize = 12.sp,
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionLink("TRACK", onTrack)
            ActionLink("COPY SNIPPET", onCopySnippet)
            ActionLink("DELETE", onDelete)
        }
    }
}

@Composable
private fun ActionLink(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = GamiloColors.Accent,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    )
}
