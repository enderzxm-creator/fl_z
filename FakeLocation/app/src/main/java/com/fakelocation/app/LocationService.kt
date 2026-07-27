package com.fakelocation.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 模拟定位前台服务。
 *
 * 启动后注册 test provider，然后按设定频率循环推送伪造坐标。
 * 停止时清理 provider。
 */
class LocationService : Service() {

    companion object {
        const val TAG = "LocationService"

        const val ACTION_START = "com.fakelocation.app.action.START"
        const val ACTION_STOP = "com.fakelocation.app.action.STOP"

        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_ALTITUDE = "altitude"
        const val EXTRA_ACCURACY = "accuracy"
        const val EXTRA_JITTER = "jitter"
        const val EXTRA_INTERVAL_MS = "interval_ms"

        private const val CHANNEL_ID = "fake_location_service"
        private const val NOTIFICATION_ID = 1

        /**
         * 启动服务的便捷方法。
         */
        fun start(
            context: Context,
            lat: Double,
            lng: Double,
            altitude: Double,
            accuracy: Float,
            jitter: Float,
            intervalMs: Long
        ) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_LAT, lat)
                putExtra(EXTRA_LNG, lng)
                putExtra(EXTRA_ALTITUDE, altitude)
                putExtra(EXTRA_ACCURACY, accuracy)
                putExtra(EXTRA_JITTER, jitter)
                putExtra(EXTRA_INTERVAL_MS, intervalMs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var pushJob: Job? = null
    private var mockManager: MockLocationManager? = null
    private var currentLat = 0.0
    private var currentLng = 0.0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
                val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
                val altitude = intent.getDoubleExtra(EXTRA_ALTITUDE, 0.0)
                val accuracy = intent.getFloatExtra(EXTRA_ACCURACY, 5f)
                val jitter = intent.getFloatExtra(EXTRA_JITTER, 0f)
                val intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 1000L)

                startForegroundCompat(lat, lng)
                startMocking(lat, lng, altitude, accuracy, jitter, intervalMs)
            }
            ACTION_STOP -> {
                stopMocking()
                stopForegroundCompat()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMocking(
        lat: Double,
        lng: Double,
        altitude: Double,
        accuracy: Float,
        jitter: Float,
        intervalMs: Long
    ) {
        currentLat = lat
        currentLng = lng

        // 停掉旧的
        pushJob?.cancel()

        mockManager = MockLocationManager(this)

        try {
            mockManager?.startProvider()
        } catch (e: SecurityException) {
            Log.e(TAG, "No mock location permission", e)
            stopSelf()
            return
        }

        pushJob = serviceScope.launch {
            while (isActive) {
                mockManager?.pushLocation(lat, lng, altitude, accuracy, jitter)

                // 更新通知显示的坐标
                val (dispLat, dispLng) = if (jitter > 0f) {
                    // 推送时已经抖动过，这里只用于通知显示
                    lat to lng
                } else {
                    lat to lng
                }
                withContext(Dispatchers.Main) {
                    updateNotification(dispLat, dispLng)
                }

                delay(intervalMs)
            }
        }

        Log.i(TAG, "Mocking started: $lat, $lng (interval=${intervalMs}ms, jitter=${jitter}m)")
    }

    private fun stopMocking() {
        pushJob?.cancel()
        pushJob = null
        mockManager?.stopProvider()
        mockManager = null
        Log.i(TAG, "Mocking stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundCompat(lat: Double, lng: Double) {
        val notification = buildNotification(lat, lng)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 需指定 foregroundServiceType
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun buildNotification(lat: Double, lng: Double): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, LocationService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(
                getString(
                    R.string.service_notification_text,
                    lat,
                    lng
                )
            )
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.btn_stop),
                stopIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(lat: Double, lng: Double) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(lat, lng))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMocking()
    }
}
