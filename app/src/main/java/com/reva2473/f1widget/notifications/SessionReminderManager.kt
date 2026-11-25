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
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reva2473.f1widget.MainActivity
import com.reva2473.f1widget.R
import com.reva2473.f1widget.data.repository.F1Repository
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.worker.WidgetRefreshScheduler
import java.time.Year
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Outcome of attempting to schedule a session reminder alarm.
 */
enum class ReminderScheduleResult {
    SCHEDULED,
    PERMISSION_DENIED,
    SESSION_IN_PAST,
    MISSING_TIMESTAMP,
    CANCELLED
}

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

    internal const val ACTION_SESSION_REMINDER = "com.reva2473.f1widget.action.SESSION_REMINDER"
    private const val REMINDER_LEAD_TIME_MILLIS = 15L * 60_000L
    private const val REMINDER_RESYNC_WORK_NAME = "apexgrid_session_reminder_resync"

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
        scheduleSessionReminderWithResult(context, weekend, session)
    }

    /**
     * Schedules an idle-allowed AlarmManager wakeup 15 minutes prior to [session] start
     * and returns a [ReminderScheduleResult] describing the outcome.
     */
    fun scheduleSessionReminderWithResult(
        context: Context,
        weekend: RaceWeekend,
        session: SessionScheduleItem
    ): ReminderScheduleResult {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        if (!hasNotificationPermission(appContext)) {
            return ReminderScheduleResult.PERMISSION_DENIED
        }

        val sessionStartMillis = session.utcEpochMillis
            ?: return ReminderScheduleResult.MISSING_TIMESTAMP
        val triggerAtMillis = sessionStartMillis - REMINDER_LEAD_TIME_MILLIS
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return ReminderScheduleResult.SESSION_IN_PAST
        }

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

        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
            ?: return ReminderScheduleResult.CANCELLED
        return try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            ReminderScheduleResult.SCHEDULED
        } catch (_: SecurityException) {
            // Defensively ignore if alarm scheduling is restricted by OEM/system policy
            ReminderScheduleResult.PERMISSION_DENIED
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
        ensureNotificationChannel(appContext)
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

    internal fun enqueueReminderResync(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<SessionReminderResyncWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            REMINDER_RESYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}

/**
 * WorkManager worker that safely resyncs session reminder alarms in the background
 * after device boot, app updates, or system clock/timezone changes.
 */
class SessionReminderResyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            withTimeoutOrNull(RESYNC_TIMEOUT_MS) {
                SessionReminderScheduler.syncReminders(
                    applicationContext,
                    F1Repository(applicationContext)
                )
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            Result.success()
        }
    }

    private companion object {
        const val RESYNC_TIMEOUT_MS = 10_000L
    }
}

/**
 * Receives AlarmManager session reminder broadcasts and system resync broadcasts
 * to post local notifications, reschedule alarms via WorkManager, and refresh Glance widgets.
 */
class SessionReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context?.applicationContext ?: return
        val action = intent?.action ?: return

        when (action) {
            in RESYNC_ACTIONS -> {
                SessionReminderScheduler.ensureNotificationChannel(appContext)
                WidgetRefreshScheduler.schedulePeriodicRefresh(appContext)
                WidgetRefreshScheduler.triggerImmediateRefresh(appContext)
                SessionReminderScheduler.enqueueReminderResync(appContext)
            }

            SessionReminderScheduler.ACTION_SESSION_REMINDER -> {
                SessionReminderScheduler.ensureNotificationChannel(appContext)
                if (SessionReminderScheduler.hasNotificationPermission(appContext)) {
                    postSessionNotification(appContext, intent)
                }
                WidgetRefreshScheduler.triggerImmediateRefresh(appContext)
            }
        }
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
            .setSmallIcon(R.drawable.ic_notification_f1)
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
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"

        val RESYNC_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_QUICKBOOT_POWERON
        )
    }
}
