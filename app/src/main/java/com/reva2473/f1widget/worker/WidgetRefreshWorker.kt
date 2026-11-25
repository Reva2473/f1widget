package com.reva2473.f1widget.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

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
            withTimeoutOrNull(WARM_TIMEOUT_MS) {
                warmActiveRaceWindow()
            }

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

            val failureCount = coroutineScope {
                widgets.map { widget ->
                    async {
                        try {
                            widget.updateAll(applicationContext)
                            0
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Throwable) {
                            1
                        }
                    }
                }.awaitAll().sum()
            }

            if (failureCount > 0 && runAttemptCount < MAX_RETRY_ATTEMPTS) {
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
        const val WARM_TIMEOUT_MS = 10_000L
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
     * Enqueues a 30-minute network-connected periodic widget refresh so countdowns
     * and remote widget data stay updated without spinning offline.
     */
    fun schedulePeriodicRefresh(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            REFRESH_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

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
