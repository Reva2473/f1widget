package com.reva2473.f1widget.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.reva2473.f1widget.MainActivity
import com.reva2473.f1widget.R
import com.reva2473.f1widget.data.repository.F1Repository
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.worker.WidgetRefreshScheduler
import java.time.Year
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Builds the canonical session reminder identifier matching [RaceWeekendScreen].
 */
fun buildSessionReminderKey(season: Int, round: Int, session: SessionScheduleItem): String =
    "${season}_${round}_${session.shortName.ifBlank { session.fullName }}"

/**
 * Manages notification channels, permission checks, and local AlarmManager
 * reminders firing 15 minutes before Formula 1 sessions start.
 */
object SessionReminderScheduler {

    const val CHANNEL_ID = "apexgrid_session_reminders"
    const val EXTRA_SESSION_KEY = "extra_session_key"
    const val EXTRA_TITLE = "extra_reminder_title"
    const val EXTRA_BODY = "extra_reminder_body"

    private const val ACTION_SESSION_REMINDER = "com.reva2473.f1widget.action.SESSION_REMINDER"
    private const val REMINDER_LEAD_TIME_MILLIS = 15L * 60_000L

    /**
     * Ensures the high-importance notification channel for F1 session reminders exists.
     */
    fun ensureNotificationChannel(context: Context) {
        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(NotificationManager::class.java)
            ?: return
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.notification_channel_session_reminders_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = appContext.getString(R.string.notification_channel_session_reminders_desc)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Returns true when both OS notification permission (API 33+) and app notification
     * settings allow posting session reminder notifications.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        val appContext = context.applicationContext
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return runtimeGranted && NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    /**
     * Schedules an idle-allowed AlarmManager wakeup 15 minutes prior to [session] start.
     */
    fun scheduleSessionReminder(
        context: Context,
        weekend: RaceWeekend,
        session: SessionScheduleItem
    ) {
        val triggerAtMillis = (session.utcEpochMillis ?: return) - REMINDER_LEAD_TIME_MILLIS
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val appContext = context.applicationContext
        val sessionKey = buildSessionReminderKey(weekend.season, weekend.round, session)
        val sessionLabel = session.fullName.ifBlank { session.shortName }
        val title = "${weekend.countryFlagEmoji} ${weekend.grandPrixName}".trim()
        val body = "15 minutes before $sessionLabel"

        val intent = Intent(appContext, SessionReminderReceiver::class.java).apply {
            action = ACTION_SESSION_REMINDER
            putExtra(EXTRA_SESSION_KEY, sessionKey)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            sessionKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // Defensively ignore if alarm scheduling is restricted by OEM/system policy
        }
    }

    /**
     * Cancels any scheduled reminder alarm matching [sessionKey].
     */
    fun cancelSessionReminder(context: Context, sessionKey: String) {
        val appContext = context.applicationContext
        val intent = Intent(appContext, SessionReminderReceiver::class.java).apply {
            action = ACTION_SESSION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            sessionKey.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Synchronizes all upcoming and disabled session reminders against DataStore preferences
     * and the season schedule.
     */
    suspend fun syncReminders(context: Context, repository: F1Repository) {
        val appContext = context.applicationContext
        val preferences = repository.userPreferencesFlow.first()
        val explicitKeys = repository.reminderSessionKeysFlow.first()
        val nowMillis = System.currentTimeMillis()
        val currentSeason = Year.now().value.coerceAtLeast(2026)

        val seasonsToSync = (
            listOf(currentSeason) + explicitKeys.mapNotNull { key ->
                key.substringBefore('_').toIntOrNull()
            }
            ).distinct()

        val knownKeys = mutableSetOf<String>()
        for (season in seasonsToSync) {
            val schedule = repository.getSeasonSchedule(season)
            for (weekend in schedule) {
                for (session in weekend.sessions) {
                    val sessionKey = buildSessionReminderKey(weekend.season, weekend.round, session)
                    knownKeys += sessionKey

                    val triggerAtMillis = session.utcEpochMillis?.let { it - REMINDER_LEAD_TIME_MILLIS }
                    val isEnabled = preferences.sessionRemindersEnabled || sessionKey in explicitKeys
                    if (isEnabled && triggerAtMillis != null && triggerAtMillis > nowMillis) {
                        scheduleSessionReminder(appContext, weekend, session)
                    } else {
                        cancelSessionReminder(appContext, sessionKey)
                    }
                }
            }
        }

        // Cancel any orphaned explicit keys that are no longer part of a loaded schedule
        for (key in explicitKeys) {
            if (key !in knownKeys) {
                cancelSessionReminder(appContext, key)
            }
        }
    }
}

/**
 * Receives AlarmManager session reminder broadcasts and device boot completions
 * to post local notifications, resync alarms, and refresh Glance widgets.
 */
class SessionReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context?.applicationContext ?: return
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    SessionReminderScheduler.syncReminders(appContext, F1Repository(appContext))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    // Ignore transient errors during boot sync
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        SessionReminderScheduler.ensureNotificationChannel(appContext)
        if (SessionReminderScheduler.hasNotificationPermission(appContext)) {
            postSessionNotification(appContext, intent)
        }
        WidgetRefreshScheduler.triggerImmediateRefresh(appContext)
    }

    @SuppressLint("MissingPermission")
    private fun postSessionNotification(context: Context, intent: Intent?) {
        val sessionKey = intent?.getStringExtra(SessionReminderScheduler.EXTRA_SESSION_KEY).orEmpty()
        val title = intent?.getStringExtra(SessionReminderScheduler.EXTRA_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: "ApexGrid Session Reminder"
        val body = intent?.getStringExtra(SessionReminderScheduler.EXTRA_BODY)
            ?.takeIf { it.isNotBlank() }
            ?: "15 minutes before session start"

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_SCREEN, TARGET_WEEKEND_SCHEDULE)
        }

        val notificationId = sessionKey.ifBlank { "$title:$body" }.hashCode()
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SessionReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }

    private companion object {
        const val EXTRA_TARGET_SCREEN = "EXTRA_TARGET_SCREEN"
        const val TARGET_WEEKEND_SCHEDULE = "WEEKEND_SCHEDULE"
    }
}
