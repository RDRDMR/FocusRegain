package com.focusgain.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusgain.app.data.FocusPreferences
import com.focusgain.app.data.TimerMode
import kotlinx.coroutines.*

/**
 * Foreground service supporting Pomodoro, Countdown, and Stopwatch modes,
 * while accumulating today's total focused study time.
 */
class StudyTimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private lateinit var prefs: FocusPreferences

    companion object {
        const val CHANNEL_ID = "focusgain_timer_channel"
        const val NOTIFICATION_ID = 101
        const val ACTION_START = "com.focusgain.ACTION_START"
        const val ACTION_PAUSE = "com.focusgain.ACTION_PAUSE"
        const val ACTION_RESET = "com.focusgain.ACTION_RESET"
        const val BROADCAST_TICK = "com.focusgain.TIMER_TICK"

        var isRunning = false
        var currentSeconds = 0L
        var currentMode = TimerMode.POMODORO
        var isBreakPhase = false
    }

    override fun onCreate() {
        super.onCreate()
        prefs = FocusPreferences(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentMode = prefs.selectedTimerMode
                if (currentSeconds == 0L && currentMode != TimerMode.STOPWATCH) {
                    currentSeconds = when (currentMode) {
                        TimerMode.POMODORO -> prefs.pomodoroFocusMinutes * 60L
                        TimerMode.COUNTDOWN -> prefs.countdownMinutes * 60L
                        TimerMode.STOPWATCH -> 0L
                    }
                }
                startForeground(NOTIFICATION_ID, buildNotification())
                startTicker()
            }
            ACTION_PAUSE -> {
                isRunning = false
                timerJob?.cancel()
                broadcastTick()
            }
            ACTION_RESET -> {
                isRunning = false
                isBreakPhase = false
                timerJob?.cancel()
                currentSeconds = 0L
                stopForeground(STOP_FOREGROUND_REMOVE)
                broadcastTick()
            }
        }
        return START_STICKY
    }

    private fun startTicker() {
        timerJob?.cancel()
        isRunning = true
        timerJob = scope.launch {
            while (isRunning) {
                delay(1000L)
                if (currentMode == TimerMode.STOPWATCH) {
                    currentSeconds += 1
                    prefs.addStudySeconds(1)
                } else {
                    if (currentSeconds > 0) {
                        currentSeconds -= 1
                        if (!isBreakPhase) {
                            prefs.addStudySeconds(1)
                        }
                    } else {
                        if (currentMode == TimerMode.POMODORO) {
                            isBreakPhase = !isBreakPhase
                            currentSeconds = if (isBreakPhase) {
                                prefs.pomodoroBreakMinutes * 60L
                            } else {
                                prefs.pomodoroFocusMinutes * 60L
                            }
                        } else {
                            isRunning = false
                            prefs.strictLockUntilMillis = 0L
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        }
                    }
                }
                updateNotification()
                broadcastTick()
            }
        }
    }

    private fun broadcastTick() {
        val intent = Intent(BROADCAST_TICK).apply {
            putExtra("seconds", currentSeconds)
            putExtra("running", isRunning)
            putExtra("today_seconds", prefs.todayStudySeconds)
            putExtra("break_phase", isBreakPhase)
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(): Notification {
        val mins = currentSeconds / 60
        val secs = currentSeconds % 60
        val label = if (isBreakPhase) "Break" else currentMode.name.lowercase().replaceFirstChar { it.uppercase() }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FocusGain — $label")
            .setContentText(String.format("%02d:%02d remaining • Today: %dm", mins, secs, prefs.todayStudySeconds / 60))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
