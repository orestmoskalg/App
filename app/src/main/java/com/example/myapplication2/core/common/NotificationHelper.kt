package com.example.myapplication2.core.common

import android.annotation.SuppressLint
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myapplication2.MainActivity
import com.example.myapplication2.core.model.Priority

object NotificationHelper {

    private fun Context.mayPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun Context.notifyCompat(id: Int, notification: Notification) {
        if (!mayPostNotifications()) return
        runCatching {
            NotificationManagerCompat.from(this).notify(id, notification)
        }
    }

    const val CHANNEL_DEADLINES = "regulatory_deadlines"
    const val CHANNEL_NEW_EVENTS = "regulatory_new_events"
    const val CHANNEL_RADAR = "regulatory_radar"

    // ── Channel setup (call from Application.onCreate) ────────────────────────

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_DEADLINES,
            "Regulatory deadlines",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders for upcoming MDR/IVDR deadlines"
            enableVibration(true)
        })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_NEW_EVENTS,
            "New regulatory events",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Alerts for new MDCG documents and regulatory changes"
        })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_RADAR,
            "Regulatory Radar",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Daily monitoring: new events and important changes"
            enableVibration(true)
        })
    }

    // ── Deadline reminder ──────────────────────────────────────────────────────

    fun sendDeadlineNotification(
        context: Context,
        title: String,
        body: String,
        daysLeft: Int,
        cardId: String,
        priority: Priority,
    ) {
        val notifId = cardId.hashCode()
        val importance = when {
            daysLeft <= 3 || priority == Priority.CRITICAL -> NotificationCompat.PRIORITY_MAX
            daysLeft <= 7 || priority == Priority.HIGH     -> NotificationCompat.PRIORITY_HIGH
            else                                           -> NotificationCompat.PRIORITY_DEFAULT
        }

        val daysText = when (daysLeft) {
            0    -> "TODAY"
            1    -> "Tomorrow"
            else -> "In $daysLeft days"
        }

        val emoji = when (priority) {
            Priority.CRITICAL -> "🚨"
            Priority.HIGH     -> "⚠️"
            Priority.MEDIUM   -> "📅"
            Priority.LOW      -> "ℹ️"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DEADLINES)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("$emoji $daysText: $title")
            .setContentText(body.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(300)))
            .setPriority(importance)
            .setAutoCancel(true)
            .setContentIntent(openCardIntent(context, cardId, notifId))
            .build()

        context.notifyCompat(notifId, notification)
    }

    // ── New event radar notification ───────────────────────────────────────────

    fun sendNewEventNotification(
        context: Context,
        eventTitle: String,
        eventBody: String,
        cardId: String,
    ) {
        val notifId = "new_${cardId}".hashCode()
        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_EVENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📡 New regulatory event")
            .setContentText(eventTitle)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(eventBody.take(250))
                .setBigContentTitle("📡 $eventTitle"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openCardIntent(context, cardId, notifId))
            .build()

        context.notifyCompat(notifId, notification)
    }

    // ── Radar summary notification ────────────────────────────────────────────

    fun sendRadarSummary(
        context: Context,
        criticalCount: Int,
        upcomingCount: Int,
        newEventsCount: Int,
    ) {
        if (criticalCount == 0 && upcomingCount == 0 && newEventsCount == 0) return

        val parts = buildList {
            if (criticalCount > 0) add("$criticalCount critical deadlines")
            if (upcomingCount > 0) add("$upcomingCount events this week")
            if (newEventsCount > 0) add("$newEventsCount new events")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_RADAR)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("📡 Regulatory Radar")
            .setContentText(parts.joinToString(" • "))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Today's update:\n${parts.joinToString("\n• ", prefix = "• ")}"))
            .setPriority(
                if (criticalCount > 0) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(openCalendarIntent(context))
            .build()

        context.notifyCompat(99001, notification)
    }

    // ── Intents ────────────────────────────────────────────────────────────────

    private fun openCardIntent(context: Context, cardId: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_card_id", cardId)
        }
        return PendingIntent.getActivity(
            context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openCalendarIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_tab", "calendar")
        }
        return PendingIntent.getActivity(
            context, 99000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
