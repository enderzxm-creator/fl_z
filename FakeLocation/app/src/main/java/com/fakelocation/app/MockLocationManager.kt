package com.fakelocation.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlin.math.cos

/**
 * 模拟定位核心管理器�?
 *
 * 通过 Android �? Mock Location Provider API 向系统注入伪造的 GPS 坐标�?
 * 使用前需在「开发者选项 �? 模拟位置信息应用」中选择本应用�?
 */
class MockLocationManager(private val context: Context) {

    companion object {
        private const val TAG = "MockLocationManager"
        private const val PROVIDER = LocationManager.GPS_PROVIDER
        // 地球半径（米），用于经纬度↔米换�?
        private const val EARTH_RADIUS = 6371000.0
    }

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * 检查当前应用是否被设为「模拟位置信息应用」�?
     */
    fun isMockLocationEnabled(): Boolean {
        return try {
            checkMockPermission()
        } catch (e: Exception) {
            Log.w(TAG, "isMockLocationEnabled check failed", e)
            false
        }
    }

    @SuppressLint("NewApi")
    private fun checkMockPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val opsManager = context.getSystemService(Context.APP_OPS_SERVICE)
                    as android.app.AppOpsManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                opsManager.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_MOCK_LOCATION,
                    android.os.Process.myUid(),
                    context.packageName
                ) == android.app.AppOpsManager.MODE_ALLOWED
            } else {
                @Suppress("DEPRECATION")
                opsManager.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_MOCK_LOCATION,
                    android.os.Process.myUid(),
                    context.packageName
                ) == android.app.AppOpsManager.MODE_ALLOWED
            }
        } else {
            // Android 5.x����� ACCESS_MOCK_LOCATION Ȩ��
            context.checkCallingOrSelfPermission("android.permission.ACCESS_MOCK_LOCATION") ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 注册测试位置提供者�?
     */
    @SuppressLint("MissingPermission")
    fun startProvider() {
        try {
            // 如果已存在则先移�?
            if (locationManager.getProvider(PROVIDER) != null) {
                locationManager.removeTestProvider(PROVIDER)
            }
            locationManager.addTestProvider(
                PROVIDER,
                false,       // requiresNetwork
                false,       // requiresSatellite
                false,       // requiresCell
                false,       // hasMonetaryCost
                true,        // supportsAltitude
                true,        // supportsSpeed
                false,       // supportsBearing
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(PROVIDER, true)
            Log.i(TAG, "Test provider registered: $PROVIDER")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to add test provider - missing mock location permission", e)
            throw e
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to add test provider", e)
            throw e
        }
    }

    /**
     * 向系统推送一个伪造的位置�?
     *
     * @param lat 纬度
     * @param lng 经度
     * @param altitude 海拔（米�?
     * @param accuracy 精度（米�?
     * @param jitterMeters 抖动幅度（米），>0 时在高斯分布下做小幅偏移
     */
    @SuppressLint("MissingPermission")
    fun pushLocation(
        lat: Double,
        lng: Double,
        altitude: Double = 0.0,
        accuracy: Float = 5.0f,
        jitterMeters: Float = 0f
    ) {
        val (finalLat, finalLng) = if (jitterMeters > 0f) {
            applyJitter(lat, lng, jitterMeters)
        } else {
            lat to lng
        }

        val location = Location(PROVIDER).apply {
            latitude = finalLat
            longitude = finalLng
            this.altitude = altitude
            this.accuracy = accuracy
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            // Android O+ 需设置完整属�?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                verticalAccuracyMeters = accuracy
                speed = 0f
                bearing = 0f
            }
        }

        try {
            locationManager.setTestProviderLocation(PROVIDER, location)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set test provider location", e)
        }
    }

    /**
     * 移除测试位置提供者�?
     */
    fun stopProvider() {
        try {
            locationManager.setTestProviderEnabled(PROVIDER, false)
            locationManager.removeTestProvider(PROVIDER)
            Log.i(TAG, "Test provider removed")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear test provider", e)
        }
    }

    /**
     * 对坐标施加高斯抖动�?
     *
     * 将抖动幅度（米）转换为纬�?/经度的偏移量，方向随机�?
     * 1 度纬�? �? 111km�?1 度经�? �? 111km × cos(lat)
     */
    private fun applyJitter(lat: Double, lng: Double, jitterMeters: Float): Pair<Double, Double> {
        val rng = java.util.Random()
        val dNorth = rng.nextGaussian() * jitterMeters / 2
        val dEast = rng.nextGaussian() * jitterMeters / 2

        val dLat = dNorth / EARTH_RADIUS * (180.0 / Math.PI)
        val dLng = dEast / (EARTH_RADIUS * cos(Math.toRadians(lat))) * (180.0 / Math.PI)

        return (lat + dLat) to (lng + dLng)
    }
}
