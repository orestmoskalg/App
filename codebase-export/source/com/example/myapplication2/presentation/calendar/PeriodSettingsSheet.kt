package com.example.myapplication2.presentation.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSettingsSheet(
    fromDateMillis: Long,
    toDateMillis: Long,
    lastRefreshLabel: String,
    eventCount: Int,
    onFromDateChange: (Long) -> Unit,
    onToDateChange: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Period Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )

            Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Date Range",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    DateRowButton(
                        label = "From",
                        dateMillis = fromDateMillis,
                        onClick = { showFromPicker = true },
                    )
                    DateRowButton(
                        label = "To",
                        dateMillis = toDateMillis,
                        onClick = { showToPicker = true },
                    )
                    Text(
                        "Default: –1 year to +3 years from today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Quick Presets",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val now = Calendar.getInstance()
                    val presets = listOf<Pair<String, () -> Unit>>(
                        "This year" to {
                            val start = Calendar.getInstance().apply { set(now.get(Calendar.YEAR), Calendar.JANUARY, 1) }
                            val end = Calendar.getInstance().apply { set(now.get(Calendar.YEAR) + 1, Calendar.JANUARY, 1); add(Calendar.DAY_OF_MONTH, -1) }
                            onFromDateChange(start.timeInMillis)
                            onToDateChange(end.timeInMillis)
                        },
                        "Next 2 years" to {
                            onFromDateChange(now.timeInMillis)
                            val end = Calendar.getInstance().apply { add(Calendar.YEAR, 2) }
                            onToDateChange(end.timeInMillis)
                        },
                        "Standard (–1yr / +3yr)" to {
                            val start = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }
                            val end = Calendar.getInstance().apply { add(Calendar.YEAR, 3) }
                            onFromDateChange(start.timeInMillis)
                            onToDateChange(end.timeInMillis)
                        },
                    )
                    presets.forEach { (label, action) ->
                        TextButton(
                            onClick = action,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Last refreshed",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            lastRefreshLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Events cached",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "$eventCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showFromPicker) {
        DatePickerDialog(
            initialMillis = fromDateMillis,
            onDateSelected = { onFromDateChange(it); showFromPicker = false },
            onDismiss = { showFromPicker = false },
        )
    }
    if (showToPicker) {
        DatePickerDialog(
            initialMillis = toDateMillis,
            onDateSelected = { onToDateChange(it); showToPicker = false },
            onDismiss = { showToPicker = false },
        )
    }
}

@Composable
private fun DateRowButton(
    label: String,
    dateMillis: Long,
    onClick: () -> Unit,
) {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
    ) {
        Icon(Icons.Default.EditCalendar, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ${df.format(Date(dateMillis))}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onDateSelected(it) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}
