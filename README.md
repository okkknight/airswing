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
```

## 真机检查

首次选择惯用手后进入 Play。依次测试正常挥拍、慢拿起、翻转、轻晃与连续十拍；在开发界面观察状态与计数。记录声音偏早/正好/偏晚、漏拍、双响和普通移动误触，再只调 `RecognitionConfig`。

模拟器和单元测试不证明击球点、延迟或震动质感；这些必须在真机确认。
