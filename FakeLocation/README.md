# FakeLocation

Android 模拟定位应用 —— 通过 Android Mock Location Provider API 向系统注入伪造的 GPS 坐标。

## 功能

- **一键启停** 模拟定位
- **手动输入** 任意经纬度坐标
- **预设地点** — 天安门、外滩、时代广场、东京塔、悉尼歌剧院、埃菲尔铁塔、大本钟、哈利法塔
- **坐标抖动模拟** — 高斯分布小幅偏移，使伪造位置更逼真，不易被 App 识别
- **可调更新频率** — 1s / 2s / 5s
- **前台 Service** — 通知栏持续推送，防止被系统杀掉
- **Material 3 + Jetpack Compose** UI

## 系统要求

- Android 7.0 (API 24) 及以上
- 需在「开发者选项」中设置本应用为「模拟位置信息应用」

## 编译

### 用 Android Studio

1. 打开 Android Studio
2. `File → Open` 选择 `FakeLocation` 目录
3. 等待 Gradle 同步完成
4. `Run → Run 'app'`

### 用命令行

```bash
# 需要安装 JDK 17 和 Android SDK
cd FakeLocation
./gradlew assembleDebug
```

生成的 APK 位于：`app/build/outputs/apk/debug/app-debug.apk`

## 使用方法

### 1. 开启开发者选项

`设置 → 关于手机` → 连续点击「版本号」7 次

### 2. 设置模拟位置应用

`设置 → 开发者选项 → 模拟位置信息应用` → 选择 **FakeLocation**

### 3. 使用

1. 打开 FakeLocation
2. 输入或选择目标坐标
3. 点击「启动模拟定位」
4. 其他应用（地图、打卡等）读取到的 GPS 位置将被替换
5. 使用完毕请点击「停止」恢复正常定位

## 技术原理

Android 提供了 [Mock Location Provider](https://developer.android.com/reference/android/location/LocationManager#addTestProvider(java.lang.String,%20boolean,%20boolean,%20boolean,%20boolean,%20boolean,%20boolean,%20boolean,%20int,%20int)) 机制，允许被指定为「模拟位置信息应用」的程序通过 `LocationManager.addTestProvider()` 注册一个测试位置提供者，然后用 `setTestProviderLocation()` 持续注入伪造坐标。系统会将这些坐标作为 GPS 定位结果分发给其他应用。

### 坐标抖动

真实 GPS 定位存在 ±几米的自然误差。完全静止的坐标可能被某些应用识别为 fake。本应用可选启用高斯抖动，在设定坐标附近做小幅随机偏移（默认 ±10 米，标准差 5 米），使伪造位置更接近真实 GPS 的行为特征。

## 项目结构

```
FakeLocation/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        └── java/com/fakelocation/app/
            ├── MainActivity.kt
            ├── LocationService.kt
            ├── LocationViewModel.kt
            ├── MockLocationManager.kt
            ├── PresetLocations.kt
            └── ui/
                ├── theme/
                │   ├── Color.kt
                │   ├── Theme.kt
                │   └── Type.kt
                ├── LocationPickerScreen.kt
                └── SettingsScreen.kt
```

## 依赖

- Kotlin 2.0
- Jetpack Compose (BOM 2024.06)
- Material 3
- Navigation Compose
- Lifecycle Compose

## 许可

仅供学习和研究用途。使用者需自行遵守当地法律法规。
