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
