# 项目交接变更记录

本文件只追加长期有效的项目状态变化，不重复 `PROJECT_CONTEXT.md` 的日常快照。

## 2026-08-12 — 项目基线与交接包

- 初始化本地 Git 仓库，默认分支为 `main`。
- 创建初始提交 `34a73f9 docs: add AirSwing MVP design`，纳入 MVP 设计文档与实施约束。
- 建立 `PROJECT_CONTEXT.md` 和本交接目录；项目仍处于 Milestone 1 未执行状态，尚无 Android 工程、构建或真机验证结果。

## 2026-08-12 — Android MVP M1–M4 实现基线

- 创建 Kotlin/Compose 单模块 Android App（`minSdk 26`、API 37）、首次设置、练习和结果流程及本地 session 统计。
- 以纯 Kotlin 实现 RingBuffer、挥拍状态机、自适应 baseline、局部 acc/gyro prominence ImpactDetector、cooldown/re-arm；Impact 直接驱动预加载的 SoundPool 和短促震动。
- Play 页面在三档音效预加载完成前维持 LOADING；后台暂停 sensor listener，恢复时重启；Debug build 记录 candidate/impact 时间戳与分数。
- 自动验证命令 `./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon` 已通过；没有连接真机，所有体验与真实设备结论仍待验收。

## 2026-08-12 — M4 真实羽毛球音效

- 用 PerMagnusLindborg 的 CC0 `Badminton.wav`（Freesound 324244）替换临时球棒音效，保留原始 WAV、来源、许可和 SHA-256 以便追溯。
- 从同一次真实击球制作 0.4 秒 soft / medium / hard 三档 PCM WAV，移除无用尾部静音并使用确定性增益差异。
- 将力度档位边界统一到 `RecognitionConfig` 并新增边界单元测试；三档听感和手机扬声器表现仍需真机验证。

## 2026-08-12 — M1–M4 完成度审查与核心链路修正

- 将原先未使用的低通参数和独立 RingBuffer 接入统一 `RecognitionPipeline`；挥拍候选使用滤波活动量、自适应静稳 baseline、真实时间上升速度和持续时间。
- 将 ImpactDetector 从候选帧基线乘积改为约 100ms 窗口内加速度/角速度双 prominence，并实现 cooldown 后“活动回落 + 新加速度上升沿”的 re-arm。
- Sensor 注册失败、SoundPool 任一 sample 加载失败均进入明确错误状态；只有同一 impact 成功发起音频与触觉后才计数，并增加 180ms 视觉 pulse。
- 持久化迁移到 Preferences DataStore，保存惯用手、session 起止时间、上次/累计挥拍数；Debug 页面增加低频指标和显式开启的有界 CSV 导出。
- 新增滤波、异常 delta、完整合成挥拍、慢速移动、双信号 prominence、cooldown/re-arm、反馈门控、preload 失败、DataStore 恢复和 recorder 测试。真实击球时机与手感仍只由真机验收。
- 从 clean 状态完成 22/22 单元测试、Debug Lint、Debug/Release 构建；无连接设备，API 37 AVD 镜像安装受 Android CLI DNS 失败阻断，因此未将运行时启动或任何体感项目标为通过。
