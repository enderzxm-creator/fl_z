package com.fakelocation.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakelocation.app.LocationViewModel
import com.fakelocation.app.UpdateInterval

/**
 * 设置页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LocationViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 更新频率
            SettingsSection("更新频率") {
                val options = UpdateInterval.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, interval ->
                        SegmentedButton(
                            selected = state.updateInterval == interval,
                            onClick = { viewModel.setUpdateInterval(interval) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size)
                        ) {
                            Text(
                                when (interval) {
                                    UpdateInterval.FAST -> "1秒"
                                    UpdateInterval.NORMAL -> "2秒"
                                    UpdateInterval.SLOW -> "5秒"
                                }
                            )
                        }
                    }
                }
            }

            // 定位精度
            SettingsSection("定位精度（米）") {
                OutlinedTextField(
                    value = state.accuracy,
                    onValueChange = viewModel::updateAccuracy,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 海拔
            SettingsSection("海拔（米）") {
                OutlinedTextField(
                    value = state.altitude,
                    onValueChange = viewModel::updateAltitude,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 坐标抖动
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "坐标抖动模拟",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "模拟真实 GPS 的轻微位置抖动，使伪造位置更逼真",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.jitterEnabled,
                            onCheckedChange = viewModel::toggleJitter
                        )
                    }
                    if (state.jitterEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Text("抖动幅度（米）", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.jitterMeters,
                            onValueChange = viewModel::updateJitterMeters,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 权限状态
            SettingsSection("权限状态") {
                PermissionRow(
                    name = "模拟位置权限",
                    granted = state.hasMockPermission,
                    onClick = viewModel::openMockLocationSettings
                )
                PermissionRow(
                    name = "通知权限",
                    granted = state.hasNotificationPermission,
                    onClick = viewModel::openNotificationSettings
                )
            }

            Spacer(Modifier.height(8.dp))

            // 说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "使用说明",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. 在「开发者选项」中将本应用设为「模拟位置信息应用」\n" +
                        "2. 选择或输入目标坐标\n" +
                        "3. 点击「启动模拟定位」\n" +
                        "4. 其他应用读取到的 GPS 位置将被替换\n" +
                        "5. 使用完毕请点击「停止」恢复正常定位",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun PermissionRow(
    name: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (granted) "✓ 已授权" else "✗ 未授权",
                style = MaterialTheme.typography.labelLarge,
                color = if (granted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(8.dp))
            if (!granted) {
                Text(
                    text = "去设置",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(4.dp)
                )
                IconButton(onClick = onClick) {
                    Text("›", fontSize = 24.sp)
                }
            }
        }
    }
}
