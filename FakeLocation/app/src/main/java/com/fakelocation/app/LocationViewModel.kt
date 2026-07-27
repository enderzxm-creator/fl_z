package com.fakelocation.app

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 更新间隔选项。
 */
enum class UpdateInterval(val ms: Long, val labelRes: Int) {
    FAST(1000L, R.string.interval_1s),
    NORMAL(2000L, R.string.interval_2s),
    SLOW(5000L, R.string.interval_5s)
}

/**
 * UI 状态。
 */
data class FakeLocationUiState(
    val isRunning: Boolean = false,
    val latitude: String = "39.9087",
    val longitude: String = "116.3975",
    val altitude: String = "50",
    val accuracy: String = "5",
    val jitterEnabled: Boolean = true,
    val jitterMeters: String = "10",
    val updateInterval: UpdateInterval = UpdateInterval.NORMAL,
    val hasMockPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val currentLat: Double = 0.0,
    val currentLng: Double = 0.0,
    val errorMessage: String? = null
)

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FakeLocationUiState())
    val uiState: StateFlow<FakeLocationUiState> = _uiState.asStateFlow()

    private val mockManager = MockLocationManager(application)

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        val hasMock = mockManager.isMockLocationEnabled()
        val hasNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val pm = getApplication<Application>().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            pm == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _uiState.update {
            it.copy(
                hasMockPermission = hasMock,
                hasNotificationPermission = hasNotify
            )
        }
    }

    fun updateLatitude(value: String) {
        _uiState.update { it.copy(latitude = value) }
    }

    fun updateLongitude(value: String) {
        _uiState.update { it.copy(longitude = value) }
    }

    fun updateAltitude(value: String) {
        _uiState.update { it.copy(altitude = value) }
    }

    fun updateAccuracy(value: String) {
        _uiState.update { it.copy(accuracy = value) }
    }

    fun toggleJitter(enabled: Boolean) {
        _uiState.update { it.copy(jitterEnabled = enabled) }
    }

    fun updateJitterMeters(value: String) {
        _uiState.update { it.copy(jitterMeters = value) }
    }

    fun setUpdateInterval(interval: UpdateInterval) {
        _uiState.update { it.copy(updateInterval = interval) }
    }

    fun selectPreset(preset: PresetLocation) {
        _uiState.update {
            it.copy(
                latitude = "%.6f".format(preset.lat),
                longitude = "%.6f".format(preset.lng)
            )
        }
    }

    /**
     * 启动模拟定位。
     */
    fun startMockLocation() {
        val state = _uiState.value

        if (!state.hasMockPermission) {
            _uiState.update {
                it.copy(errorMessage = getApplication<Application>().getString(R.string.warning_no_mock_permission))
            }
            return
        }

        val lat = state.latitude.toDoubleOrNull()
        val lng = state.longitude.toDoubleOrNull()
        if (lat == null || lng == null) {
            _uiState.update { it.copy(errorMessage = "经纬度格式错误") }
            return
        }

        val altitude = state.altitude.toDoubleOrNull() ?: 0.0
        val accuracy = state.accuracy.toFloatOrNull() ?: 5f
        val jitter = if (state.jitterEnabled) state.jitterMeters.toFloatOrNull() ?: 0f else 0f
        val interval = state.updateInterval.ms

        LocationService.start(
            getApplication(),
            lat,
            lng,
            altitude,
            accuracy,
            jitter,
            interval
        )

        _uiState.update {
            it.copy(
                isRunning = true,
                currentLat = lat,
                currentLng = lng,
                errorMessage = null
            )
        }
    }

    /**
     * 停止模拟定位。
     */
    fun stopMockLocation() {
        LocationService.stop(getApplication())
        _uiState.update {
            it.copy(isRunning = false)
        }
    }

    /**
     * 打开「开发者选项」中的模拟位置设置。
     */
    fun openMockLocationSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        getApplication<Application>().startActivity(intent)
    }

    /**
     * 打开应用通知设置。
     */
    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Settings.EXTRA_APP_PACKAGE, getApplication<Application>().packageName)
        }
        getApplication<Application>().startActivity(intent)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
