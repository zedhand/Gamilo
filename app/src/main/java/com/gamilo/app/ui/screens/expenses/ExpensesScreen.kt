package com.gamilo.app.ui.screens.expenses

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.data.entity.ExpenseEntity
import com.gamilo.app.ui.components.BorderedTextField
import com.gamilo.app.ui.components.FilterBarHost
import com.gamilo.app.ui.components.GamiloButton
import com.gamilo.app.ui.components.JobPickerSection
import com.gamilo.app.ui.components.ListRowCard
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens
import com.gamilo.app.ui.theme.MonospaceNumeric
import java.math.BigDecimal

@Composable
fun ExpensesScreen(viewModel: ExpensesViewModel, filter: GlobalFilter, onFilterChange: (GlobalFilter) -> Unit) {
    val settings by viewModel.settings.collectAsState(initial = null)
    val jobs by viewModel.jobs.collectAsState(initial = emptyList())
    val jobsById = remember(jobs) { jobs.associateBy { it.id } }
    val entries = viewModel.entries.collectAsLazyPagingItems()
    val context = LocalContext.current

    var description by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var selectedJobId by remember { mutableStateOf<Long?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            photoUri = uri.toString()
        }
    }

    LaunchedEffect(filter) { viewModel.setFilter(filter) }

    // Wrapped in an explicit Box/Column (not left as bare siblings): FilterBarHost's
    // conditional full-screen picker overlay needs a real parent layout to overlay
    // correctly alongside the list below it — see StickyFilterBar.kt.
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            FilterBarHost(filter = filter, jobs = jobs, onFilterChange = onFilterChange)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GamiloColors.Background)
                    .padding(GamiloDimens.ScreenPadding)
                    .testTag("expenses_form_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    BorderedTextField(
                        label = "Description",
                        value = description,
                        onValueChange = { description = it },
                        testTag = "expense_description_input",
                    )
                }
                item {
                    BorderedTextField(
                        label = "Cost (${settings?.baseCurrencyCode ?: "CAD"})",
                        value = costText,
                        onValueChange = { costText = it },
                        isNumeric = true,
                        testTag = "expense_cost_input",
                    )
                }
                item { SectionLabel("Job") }
                item { JobPickerSection(jobs = jobs, selectedJobId = selectedJobId, onSelect = { selectedJobId = it }) }
                item {
                    GamiloButton(
                        label = if (photoUri == null) "Attach Receipt Photo" else "Photo Attached",
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    )
                }
                item {
                    GamiloButton(
                        label = "Add Expense",
                        onClick = {
                            val cost = costText.toBigDecimalOrNull()
                            if (description.isNotBlank() && cost != null) {
                                viewModel.addExpense(description, cost, photoUri, selectedJobId)
                                description = ""
                                costText = ""
                                photoUri = null
                                selectedJobId = null
                            }
                        },
                    )
                }

                if (entries.itemCount == 0) {
                    item { Text(text = "No expenses logged yet.", color = GamiloColors.TextSecondary, fontSize = 13.sp) }
                } else {
                    items(count = entries.itemCount, key = entries.itemKey { it.id }) { index ->
                        entries[index]?.let { expense ->
                            ExpenseRow(expense, jobTitle = expense.jobId?.let { jobsById[it]?.title }, onDelete = { viewModel.deleteExpense(expense.id) })
                        }
                    }
                }
            }
        }
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

@Composable
private fun SectionLabel(text: String) {
    Text(text = text.uppercase(), color = GamiloColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}

@Composable
private fun ExpenseRow(expense: ExpenseEntity, jobTitle: String?, onDelete: () -> Unit) {
    ListRowCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = expense.description, color = GamiloColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = "${expense.cost} ${expense.currencyCode}" +
                        (if (expense.photoUri != null) " · PHOTO" else "") +
                        (jobTitle?.let { " · $it" } ?: ""),
                    color = GamiloColors.TextSecondary,
                    fontFamily = MonospaceNumeric,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = "DELETE",
                color = GamiloColors.TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDelete),
            )
        }
    }
}
