package com.example.habittracker.activities

import android.app.*
import android.content.*
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.habittracker.HabitTrackerApplication
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.data.local.entity.ActivitySessionStatus
import com.example.habittracker.data.local.entity.Habit
import com.example.habittracker.data.local.entity.elapsedMillis
import kotlinx.coroutines.*

class AndroidActivityServiceController(private val context: Context) : ActivityServiceController {
    override fun refresh() { ContextCompat.startForegroundService(context, Intent(context, ActiveActivityService::class.java).setAction(ActiveActivityService.ACTION_REFRESH)) }
    override fun stop() { context.stopService(Intent(context, ActiveActivityService::class.java)) }
}

class ActiveActivityService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() { super.onCreate(); createChannel() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification_habit).setContentTitle("Active activity").setContentText("Restoring timer…").setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build())
        scope.launch {
            val repository = (application as HabitTrackerApplication).repository
            when (intent?.action) {
                ACTION_PAUSE -> repository.pauseSession()
                ACTION_RESUME -> repository.resumeSession()
                ACTION_FINISH -> repository.finishSession(allowShort = true)
            }
            val session = repository.activeSessionSnapshot()
            val habit = session?.let { repository.reminderData(it.habitId).first }
            if (session == null || habit == null) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return@launch }
            startForeground(NOTIFICATION_ID, notification(habit, session))
        }
        return START_STICKY
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private fun notification(habit: Habit, session: com.example.habittracker.data.local.entity.ActivitySession): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_ACTIVE_SESSION, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val open = PendingIntent.getActivity(this, 8100, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val toggleAction = if (session.status == ActivitySessionStatus.RUNNING) ACTION_PAUSE else ACTION_RESUME
        val toggleLabel = if (session.status == ActivitySessionStatus.RUNNING) "Pause" else "Resume"
        val toggle = PendingIntent.getService(this, 8101, Intent(this, ActiveActivityService::class.java).setAction(toggleAction), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val finish = PendingIntent.getService(this, 8102, Intent(this, ActiveActivityService::class.java).setAction(ACTION_FINISH), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val target = habit.target?.takeIf { it > 0 }?.let { "Target: $it ${habit.unit ?: "min"}" } ?: "Timed activity"
        val builder = NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification_habit).setContentTitle(habit.name).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setPriority(NotificationCompat.PRIORITY_LOW).addAction(0, toggleLabel, toggle).addAction(0, "Finish", finish)
        if (session.status == ActivitySessionStatus.RUNNING) builder.setWhen(System.currentTimeMillis() - session.elapsedMillis(System.currentTimeMillis())).setUsesChronometer(true).setContentText(target)
        else builder.setContentText("${formatElapsed(session.accumulatedActiveMillis)} · Paused").setSubText(target)
        return builder.build()
    }
    private fun createChannel() { if (android.os.Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, "Active activities", NotificationManager.IMPORTANCE_LOW).apply { description = "Ongoing timed routine activity" }) }
    companion object { const val CHANNEL_ID = "active_activities"; const val NOTIFICATION_ID = 8100; const val ACTION_REFRESH = "activity.refresh"; const val ACTION_PAUSE = "activity.pause"; const val ACTION_RESUME = "activity.resume"; const val ACTION_FINISH = "activity.finish" }
}

private fun formatElapsed(millis: Long): String { val seconds = millis / 1000; val h = seconds / 3600; val m = seconds % 3600 / 60; val s = seconds % 60; return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}" else "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}" }
