package com.example.myapplication2.core.common

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Prevents duplicate push notifications for the same deadline / new event within one calendar day
 * (e.g. when a worker runs more than once).
 */
object NotificationDedup {

    private const val PREFS = "notification_dedup"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun shouldNotifyDeadlineToday(context: Context, cardId: String, daysLeft: Int): Boolean {
        val key = "dl_${cardId}_$daysLeft"
        val p = prefs(context)
        val day = today()
        if (p.getString(key, null) == day) return false
        p.edit().putString(key, day).apply()
        return true
    }

    fun shouldNotifyNewEventToday(context: Context, cardId: String): Boolean {
        val key = "ne_$cardId"
        val p = prefs(context)
        val day = today()
        if (p.getString(key, null) == day) return false
        p.edit().putString(key, day).apply()
        return true
    }
}
