package com.reva2473.f1widget.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.reva2473.f1widget.data.repository.F1Repository
import com.reva2473.f1widget.widget.ConstructorStandingsWidget
import com.reva2473.f1widget.widget.DriverStandingsWidget
import com.reva2473.f1widget.widget.FavoriteDriverWidget
import com.reva2473.f1widget.widget.FavoriteTeamWidget
import com.reva2473.f1widget.widget.LastRacePodiumWidget
import com.reva2473.f1widget.widget.LiveTimingBannerWidget
import com.reva2473.f1widget.widget.NextRaceCountdownWidget
import com.reva2473.f1widget.widget.WeekendScheduleWidget
import java.time.Year
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/**
 * Background WorkManager worker that refreshes all ApexGrid Glance widgets
 * and warms active live/race-weekend repository data when a session window is active.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            warmActiveRaceWindow()

            val widgets: List<GlanceAppWidget> = listOf(
                NextRaceCountdownWidget(),
                WeekendScheduleWidget(),
                DriverStandingsWidget(),
                ConstructorStandingsWidget(),
                FavoriteDriverWidget(),
                FavoriteTeamWidget(),
                LastRacePodiumWidget(),
                LiveTimingBannerWidget()
            )

            var failureCount = 0
            for (widget in widgets) {
                try {
                    widget.updateAll(applicationContext)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    failureCount++
                }
            }

            if (failureCount == widgets.size && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun warmActiveRaceWindow() {
        try {
            val repository = F1Repository(applicationContext)
            val currentSeason = Year.now().value.coerceAtLeast(2026)
            val schedule = repository.getSeasonSchedule(currentSeason)
            val nowMillis = System.currentTimeMillis()
            val windowStartOffsetMillis = 2L * 60L * 60L * 1000L
            val windowEndOffsetMillis = 3L * 60L * 60L * 1000L

            val hasActiveSession = schedule.any { weekend ->
                weekend.sessions.any { session ->
                    val startMillis = session.utcEpochMillis ?: return@any false
                    nowMillis in (startMillis - windowStartOffsetMillis)..(startMillis + windowEndOffsetMillis)
                }
            }

            if (hasActiveSession) {
                repository.getLiveSessionState("latest")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Proceed with widget refresh using cached/seed data when offline
        }
    }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 3
    }
}

/**
 * Scheduler for periodic and on-demand ApexGrid Glance widget updates.
 */
object WidgetRefreshScheduler {

    private const val PERIODIC_WORK_NAME = "apexgrid_periodic_widget_refresh"
    private const val IMMEDIATE_WORK_NAME = "apexgrid_immediate_widget_refresh"
    private const val REFRESH_INTERVAL_MINUTES = 30L

    /**
     * Enqueues a 30-minute unconstrained periodic widget refresh so countdowns
     * and offline-cached widgets continue ticking around midnight and session starts.
     */
    fun schedulePeriodicRefresh(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            REFRESH_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Enqueues an immediate one-time widget refresh across all Glance widgets.
     */
    fun triggerImmediateRefresh(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
