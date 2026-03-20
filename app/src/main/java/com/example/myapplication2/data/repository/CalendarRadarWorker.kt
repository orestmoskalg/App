package com.example.myapplication2.data.repository

import android.content.Context
import androidx.work.*
import com.example.myapplication2.core.common.NotificationDedup
import com.example.myapplication2.core.common.NotificationHelper
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * REGULATORY RADAR — runs once per day.
 *
 * What it does:
 * 1. Optionally merges **new** AI-generated events (by title) if last calendar refresh was >72h ago
 * 2. Sends deadline notifications for events within 30/14/7/3/1/0 days
 * 3. Radar summary notification (if enabled)
 *
 * Static master calendar seed is applied only once at app startup when the calendar is empty — not here.
 */
class CalendarRadarWorker(
    ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val container = AppContainer(applicationContext)
        runCatching {
            val settings = container.appSettingsRepository
            val cardRepo = container.cardRepository

            // ── Step 1: Load user profile ──────────────────────────────────────
            val profile = container.userProfileRepository.getUserProfile()
                ?: return@runCatching Result.success()
            val niches = profile.niches

            // Master seed is applied once at app init when the calendar is empty (see AppRootViewModel).
            // Do not merge master here — it would re-add hundreds of events after every API refresh
            // because titles rarely match exactly.

            // ── Step 2: AI-generated niche events (max once per 3 days) ────────
            val lastAiRefresh = settings.getLastCalendarRefreshMillis()
            val hoursSinceRefresh = (System.currentTimeMillis() - lastAiRefresh) / 3_600_000L
            if (hoursSinceRefresh > 72) {
                runCatching {
                    val aiEvents = container.calendarRepository.generateCalendar(niches, profile)
                    mergeNewEvents(cardRepo, aiEvents, settings)
                    settings.setLastCalendarRefreshMillis(System.currentTimeMillis())
                }
            }

            // ── Step 3: Send deadline notifications ────────────────────────────
            val notifEnabled = settings.getNotificationsEnabled()
            val deadlinesEnabled = settings.getDeadlineAlertsEnabled()
            val urgentOnly = settings.getUrgentOnlyEnabled()

            if (notifEnabled && deadlinesEnabled) {
                sendDeadlineNotifications(cardRepo, urgentOnly)
            }

            // ── Step 4: Radar summary (weekly digest toggle) ─────────────────
            if (notifEnabled && settings.getWeeklyDigestEnabled()) {
                sendRadarSummary(cardRepo, container)
            }

            Result.success()
        }.getOrElse { Result.retry() }
    }

    // ── Merge: add only new events, never delete existing ─────────────────────

    private suspend fun mergeNewEvents(
        cardRepo: com.example.myapplication2.domain.repository.CardRepository,
        newEvents: List<com.example.myapplication2.core.model.DashboardCard>,
        settings: com.example.myapplication2.domain.repository.AppSettingsRepository,
    ) {
        val existing = runCatching {
            cardRepo.observeCardsByType(CardType.REGULATORY_EVENT).first()
        }.getOrDefault(emptyList())

        val existingTitles = existing.map { it.title.trim().lowercase() }.toSet()

        val toAdd = newEvents.filter { card ->
            card.title.trim().lowercase() !in existingTitles
        }.distinctBy { it.title.trim().lowercase() }

        if (toAdd.isEmpty()) return

        cardRepo.saveCards(toAdd)

        if (!settings.getNotificationsEnabled()) return
        val urgentOnly = settings.getUrgentOnlyEnabled()
        toAdd.take(5).forEach { card ->
            if (urgentOnly && card.priority != Priority.CRITICAL && card.priority != Priority.HIGH) return@forEach
            if (!NotificationDedup.shouldNotifyNewEventToday(applicationContext, card.id)) return@forEach
            NotificationHelper.sendNewEventNotification(
                context = applicationContext,
                eventTitle = card.title,
                eventBody = card.subtitle,
                cardId = card.id,
            )
        }
    }

    // ── Deadline notifications ─────────────────────────────────────────────────

    private suspend fun sendDeadlineNotifications(
        cardRepo: com.example.myapplication2.domain.repository.CardRepository,
        urgentOnly: Boolean,
    ) {
        val allEvents = runCatching {
            cardRepo.observeCardsByType(CardType.REGULATORY_EVENT).first()
        }.getOrDefault(emptyList())

        val now = System.currentTimeMillis()
        val notifyAt = setOf(0, 1, 3, 7, 14, 30)

        allEvents.forEach { card ->
            if (urgentOnly && card.priority != Priority.CRITICAL && card.priority != Priority.HIGH) return@forEach
            val daysLeft = ((card.dateMillis - now) / 86_400_000L).toInt()
            if (daysLeft in notifyAt) {
                if (!NotificationDedup.shouldNotifyDeadlineToday(applicationContext, card.id, daysLeft)) return@forEach
                NotificationHelper.sendDeadlineNotification(
                    context  = applicationContext,
                    title    = card.title,
                    body     = card.subtitle,
                    daysLeft = daysLeft,
                    cardId   = card.id,
                    priority = card.priority,
                )
            }
        }
    }

    // ── Radar summary ──────────────────────────────────────────────────────────

    private suspend fun sendRadarSummary(
        cardRepo: com.example.myapplication2.domain.repository.CardRepository,
        container: AppContainer,
    ) {
        val allEvents = runCatching {
            cardRepo.observeCardsByType(CardType.REGULATORY_EVENT).first()
        }.getOrDefault(emptyList())

        val now   = System.currentTimeMillis()
        val weekMs = 7 * 86_400_000L
        val dayMs  = 86_400_000L

        val critical = allEvents.count {
            it.priority == Priority.CRITICAL && it.dateMillis in now..(now + weekMs)
        }
        val upcoming = allEvents.count { it.dateMillis in now..(now + weekMs) }
        val newToday = allEvents.count {
            (now - it.dateMillis) < dayMs && it.dateMillis <= now + weekMs
        }

        NotificationHelper.sendRadarSummary(applicationContext, critical, upcoming, newToday)
    }

    // ── Static schedule helper ─────────────────────────────────────────────────

    companion object {
        private const val WORK_TAG = "calendar_radar"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CalendarRadarWorker>(
                24, TimeUnit.HOURS,
                // Flex window: run any time in the last 3 hours of the 24h window
                3, TimeUnit.HOURS,
            )
                .setInitialDelay(2, TimeUnit.HOURS)  // first run: 2 hours after install
                .addTag(WORK_TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,  // don't restart if already scheduled
                request,
            )
        }

        fun scheduleImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<CalendarRadarWorker>()
                .addTag("${WORK_TAG}_immediate")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }
    }
}
