package com.example.myapplication2.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication2.MainActivity
import com.example.myapplication2.R
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val CALENDAR_CHANNEL_ID = "calendar_updates"

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val context: Context,
) : NotificationRepository {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CALENDAR_CHANNEL_ID,
                "Календар подій",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Критичні події та дедлайни по ваших нішах"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override suspend fun notifyCalendarUpdates(cards: List<DashboardCard>) {
        val toNotify = cards
            .filter { it.priority == Priority.CRITICAL || it.priority == Priority.HIGH }
            .take(5)
        if (toNotify.isEmpty()) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        toNotify.forEachIndexed { index, card ->
            val priorityLabel = when (card.priority) {
                Priority.CRITICAL -> "Критично"
                Priority.HIGH -> "Важливо"
                else -> ""
            }
            val title = if (priorityLabel.isNotEmpty()) "[$priorityLabel] ${card.title}" else card.title
            val notification = NotificationCompat.Builder(context, CALENDAR_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(card.subtitle.ifBlank { card.body.take(100) })
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(card.id.hashCode() and 0x7FFFFFFF, notification)
        }
    }
}
