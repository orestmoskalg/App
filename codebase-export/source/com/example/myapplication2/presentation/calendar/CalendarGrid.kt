package com.example.myapplication2.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun Long.toCalendarDayKey(): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this
    return (cal.get(Calendar.YEAR) * 10000L + cal.get(Calendar.MONTH) * 100L + cal.get(Calendar.DAY_OF_MONTH))
}

@Composable
fun CalendarGrid(
    selectedMonthYear: Pair<Int, Int>,
    events: List<DashboardCard>,
    onMonthChange: (Int, Int) -> Unit,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (month, year) = selectedMonthYear
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startOffset = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

    val eventsByDay = events.groupBy { it.dateMillis.toCalendarDayKey() }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = {
                    val newCal = Calendar.getInstance().apply { set(year, month, 1); add(Calendar.MONTH, -1) }
                    onMonthChange(newCal.get(Calendar.MONTH), newCal.get(Calendar.YEAR))
                }) {
                    Icon(Icons.Default.ChevronLeft, "Previous month")
                }
                Text(
                    text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = {
                    val newCal = Calendar.getInstance().apply { set(year, month, 1); add(Calendar.MONTH, 1) }
                    onMonthChange(newCal.get(Calendar.MONTH), newCal.get(Calendar.YEAR))
                }) {
                    Icon(Icons.Default.ChevronRight, "Next month")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            val todayKey = System.currentTimeMillis().toCalendarDayKey()

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - startOffset + 1

                        if (day < 1 || day > daysInMonth) {
                            Box(modifier = Modifier.weight(1f))
                        } else {
                            val dayCal = Calendar.getInstance().apply { set(year, month, day) }
                            val dayKey = year * 10000L + month * 100L + day
                            val dayEvents = eventsByDay[dayKey] ?: emptyList()
                            val dayStartMillis = dayCal.timeInMillis

                            DayCell(
                                day = day,
                                isToday = dayKey == todayKey,
                                events = dayEvents,
                                onClick = { if (dayEvents.isNotEmpty()) onDayClick(dayStartMillis) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    events: List<DashboardCard>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dotColor: Color? = when {
        events.any { it.priority == Priority.CRITICAL } -> Color(0xFFE53935)
        events.any { it.priority == Priority.HIGH } -> Color(0xFFFB8C00)
        events.isNotEmpty() -> Color(0xFF43A047)
        else -> null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = events.isNotEmpty(), onClick = onClick)
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .then(
                    when {
                        isToday -> Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        else -> Modifier
                    }
                ),
        ) {
            Text(
                text = "$day",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.height(6.dp),
        ) {
            if (dotColor != null) {
                repeat(minOf(events.size, 3)) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(dotColor, CircleShape),
                    )
                }
            }
        }
    }
}
