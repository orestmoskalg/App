package com.example.myapplication2.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myapplication2.RegulationApplication
import java.util.concurrent.TimeUnit

private const val CALENDAR_REFRESH_WORK = "calendar_refresh_work"

class CalendarRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as RegulationApplication
        return runCatching {
            app.appContainer.refreshCalendarUseCase()
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}

fun scheduleCalendarRefreshWork(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<CalendarRefreshWorker>(24, TimeUnit.HOURS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        CALENDAR_REFRESH_WORK,
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest,
    )
}
