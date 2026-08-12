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

## 真机检查

首次选择惯用手后进入 Play。确认 `LOADING` 只在音效预加载期间出现，之后为 `READY`；后台切换时显示 `PAUSED`。依次测试正常挥拍、慢拿起、翻转、轻晃与连续十拍。`adb logcat` 会输出 `candidate` 与 `impact` 时间戳/分数，用于判断漏拍、双响、声音偏早或偏晚；参数只从 `RecognitionConfig` 调整。

模拟器和单元测试不证明击球点、延迟或震动质感；这些必须在真机确认。
