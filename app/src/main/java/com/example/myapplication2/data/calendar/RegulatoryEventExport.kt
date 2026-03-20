package com.example.myapplication2.data.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.example.myapplication2.core.model.DashboardCard
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object RegulatoryEventExport {

    fun addToCalendar(context: Context, card: DashboardCard): Boolean = runCatching {
        val desc = buildString {
            append(buildDescription(card))
            card.links.firstOrNull()?.url?.let { append("\n\n").append(it) }
        }
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "[${card.niche}] ${card.title}")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, card.dateMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, card.dateMillis + 3_600_000L)
            putExtra(CalendarContract.Events.ALL_DAY, true)
            putExtra(CalendarContract.Events.DESCRIPTION, desc)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    fun shareText(context: Context, card: DashboardCard) {
        val df = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val text = buildString {
            appendLine(card.title)
            appendLine()
            appendLine("Date: ${df.format(Date(card.dateMillis))}")
            appendLine("Niche: ${card.niche}")
            appendLine("Priority: ${card.priority}")
            appendLine()
            appendLine(card.body)
            if (card.actionChecklist.isNotEmpty()) {
                appendLine()
                appendLine("Actions:")
                card.actionChecklist.forEachIndexed { i, a -> appendLine("  ${i + 1}. $a") }
            }
            card.links.firstOrNull()?.url?.let { appendLine("\n${it}") }
            appendLine("\n— Shared from Regulatory Assistant")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, card.title)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share event").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun generateIcsFile(context: Context, cards: List<DashboardCard>, filename: String = "regulatory_events"): File? = runCatching {
        val ics = buildIcs(cards)
        val file = File(context.cacheDir, "$filename.ics")
        file.writeText(ics)
        file
    }.getOrNull()

    private fun buildDescription(card: DashboardCard): String = buildString {
        appendLine(card.subtitle)
        appendLine(card.body)
        if (card.actionChecklist.isNotEmpty()) {
            appendLine()
            card.actionChecklist.forEach { appendLine("• $it") }
        }
    }

    private fun buildIcs(cards: List<DashboardCard>): String {
        val df = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Regulatory Assistant//EN")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")
        cards.forEach { card ->
            val start = df.format(Date(card.dateMillis))
            val end = df.format(Date(card.dateMillis + 3_600_000L))
            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:${card.id}@regulatory-assistant")
            sb.appendLine("DTSTART:$start")
            sb.appendLine("DTEND:$end")
            sb.appendLine("SUMMARY:${escapeIcs("[${card.niche}] ${card.title}")}")
            sb.appendLine("DESCRIPTION:${escapeIcs(buildDescription(card))}")
            sb.appendLine("END:VEVENT")
        }
        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    private fun escapeIcs(text: String): String =
        text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
}
