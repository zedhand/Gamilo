package com.gamilo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens

/**
 * Shared "which Job does this belong to" picker for Expenses/Hours/Mileage/Shipping entry
 * forms — every one of those entities has a nullable `jobId`, but until now nothing in the UI
 * ever let a user pick a job to attach one to. Only active jobs are offered: a completed or
 * cancelled job shouldn't be accepting new charges against it.
 */
@Composable
fun JobPickerSection(jobs: List<JobEntity>, selectedJobId: Long?, onSelect: (Long?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JobOptionRow(label = "No Job", selected = selectedJobId == null, onClick = { onSelect(null) })
        jobs.forEach { job ->
            JobOptionRow(
                label = "${job.title} — ${job.clientName}",
                selected = selectedJobId == job.id,
                onClick = { onSelect(job.id) },
            )
        }
        if (jobs.isEmpty()) {
            Text(
                text = "No active jobs yet — create one from the JOBS link to link entries to it.",
                color = GamiloColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun JobOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
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
        Text(text = label, color = GamiloColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (selected) Text(text = "ACTIVE", color = GamiloColors.Accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}
