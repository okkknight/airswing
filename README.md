# 空气挥拍

没有球，也没有球网；抬手一挥，还是应该有一记干脆的“啪”。

空气挥拍是一个 Android 体感玩具：把手机当作羽毛球拍，完成挥拍动作时，应用会在最接近你主观“击中球”的那一刻给出真实击球音效和短震动。它追求的不是分析动作标准不标准，而是把手机变成球拍的那一下错觉。

所以它把最重要的事情放在手感上：真正挥拍能稳定触发，走路、翻手机和随手晃动尽量不误响；连续挥十几拍时，每一拍都只响一次，而且声音要跟手。

## 体验流程

1. 在带加速度计和陀螺仪的 Android 手机上安装 Debug APK。
2. 选择惯用手，握住手机下半部，让手机长轴大致对准想象中的球拍杆，进入 Play。
3. 看到 `READY` 就可以挥。击球被识别后，会有声音、短震动和一次计数。
4. 停下来看看这一局的挥拍数；切到后台时应用会自动停掉传感器。

声音分为轻、中、重三档。Debug 版还能记录本局传感器数据并导出 CSV，用于排查漏拍、误触和反馈时机。

## 构建与安装

需要 Android SDK、可用的 JDK 和已开启 USB 或无线调试的 Android 手机。最低支持 Android 8.0（API 26）。请先在本机配置 `JAVA_HOME` 和 `ANDROID_HOME`，然后运行：

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。如需运行单元测试和 Android Lint，可执行：

```bash
./gradlew testDebugUnitTest lintDebug
```

## 想调手感时

先试试正常挥拍、缓慢拿起、翻转、走动、轻晃和连续挥拍。最值得盯住的是漏拍、重复计数，以及声音是不是正好落在你觉得“打到了”的那一下。Debug 页面会显示传感器采样、识别分数和最近一次击球时间；也可用以下命令查看日志：

```bash
adb logcat AirSwing:D '*:S'
```

显示“设备不支持”时，手机缺少必要传感器；显示“传感器启动失败”或“击球音效加载失败”时，应用不会继续计数或播放。识别参数集中在 `RecognitionConfig`，方便根据真机记录调整。

## 实现与许可

实时链路为 `Accelerometer + Gyroscope → SwingDetector → ImpactDetector → SoundPool + Haptic`。应用代码采用 [MIT 许可证](LICENSE)；击球音效及来源录音按 CC0 1.0 使用，见[音效许可说明](AUDIO_LICENSES.md)。
