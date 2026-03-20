package com.example.myapplication2.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import java.util.Calendar

object RegulatoryNotificationService {
    private const val TAG = "RegNotifications"
    private const val CHANNEL_ID = "regulatory_deadlines"
    private const val CHANNEL_NAME = "Regulatory deadlines"
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Upcoming regulatory deadlines" }
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules reminder alarms for upcoming [DashboardCard] events (best-effort; exact alarms may require permission).
     */
    fun scheduleFromCards(context: Context, cards: List<DashboardCard>) {
        cancelAlarms(context)
        val future = cards.filter { it.dateMillis > System.currentTimeMillis() }.take(40)
        var scheduled = 0
        future.forEach { card ->
            val leads = leadDaysFor(card.priority)
            leads.forEach { days ->
                val trigger = Calendar.getInstance().apply {
                    timeInMillis = card.dateMillis
                    add(Calendar.DAY_OF_YEAR, -days)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                if (trigger.timeInMillis > System.currentTimeMillis()) {
                    scheduleOne(context, card, days, trigger.timeInMillis)
                    scheduled++
                }
            }
        }
        Log.i(TAG, "Scheduled $scheduled alarms for ${future.size} events")
    }

    private fun scheduleOne(context: Context, card: DashboardCard, daysBefore: Int, triggerAt: Long) {
        val requestCode = ("${card.id}_$daysBefore").hashCode()
        val intent = Intent(context, RegulatoryNotificationReceiver::class.java).apply {
            putExtra("title", daysLabel(daysBefore))
            putExtra("subtitle", card.title)
            putExtra(
                "body",
                "${card.niche} — ${card.subtitle}\n${card.priority}",
            )
            putExtra("requestCode", requestCode)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancelAlarms(context: Context) {
        // Best-effort: cancel shown notifications; pending alarms from prior sessions may remain without stored PendingIntent refs.
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }

    private fun leadDaysFor(p: Priority): List<Int> = when (p) {
        Priority.CRITICAL -> listOf(30, 7, 1)
        Priority.HIGH -> listOf(14, 7, 1)
        Priority.MEDIUM -> listOf(7, 1)
        Priority.LOW -> listOf(1)
    }

    private fun daysLabel(days: Int): String = when (days) {
        1 -> "Tomorrow"
        7 -> "1 week away"
        14 -> "2 weeks away"
        30 -> "30 days away"
        else -> "$days days away"
    }
}

class RegulatoryNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        val subtitle = intent.getStringExtra("subtitle") ?: ""
        val body = intent.getStringExtra("body") ?: ""
        val requestCode = intent.getIntExtra("requestCode", 0)
        val nid = kotlin.math.abs(requestCode % 0x7FFF_FFFF)

        val notification = NotificationCompat.Builder(context, "regulatory_deadlines")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$subtitle\n$body"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(nid, notification)
    }
}
