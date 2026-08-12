# 空气挥拍

离线 Android 羽毛球空气挥拍 MVP。传感器到反馈链路为：`Accelerometer + Gyroscope → SwingDetector → ImpactDetector → SoundPool + Haptic`。

## 构建

```bash
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/jdk-17.0.20+8/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew testDebugUnitTest lintDebug assembleDebug
```

安装到已开启 USB/Wireless debugging 的真机：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb logcat AirSwing:D '*:S'
```

APK 位置：`app/build/outputs/apk/debug/app-debug.apk`。应用完全离线，只需要设备具备加速度计和陀螺仪；不需要账号或网络权限。

## 真机检查

首次选择惯用手后进入 Play。确认 `LOADING` 只在 Sensor 注册和三档音效预加载期间出现，之后为 `READY`；后台切换时显示 `PAUSED`。依次测试正常挥拍、慢拿起、翻转、走动、轻晃与连续十拍，并逐项记录：

- 正常挥拍是否大多数都响且一拍一响；
- 连续挥拍是否漏第二拍；
- 普通移动是否误触；
- 声音偏早、正好还是偏晚；
- 声音与短震动是否同步；
- soft / medium / hard 是否有可辨认且合理的差异。

Debug APK 的 Play 页面每 250ms 显示实际采样间隔、帧数、音频状态、detector state、swing/impact score 与最近 impact 时间。需要回放时点击“记录本局传感器”，完成后点击“导出 CSV”；录制默认关闭，只在内存保留当前手动录制且有固定上限。`adb logcat` 会输出 candidate、impact、反馈调用时间戳。调参只修改 `RecognitionConfig`。

若显示“设备不支持”，设备缺少加速度计或陀螺仪；“传感器启动失败”表示 listener 注册失败；“击球音效加载失败”表示 SoundPool preload 未完成，以上状态均不会计数或播放。

模拟器和单元测试不证明击球点、延迟或震动质感；这些必须在真机确认。
