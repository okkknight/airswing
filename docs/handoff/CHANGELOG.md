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
- 完成 25/25 单元测试、Debug Lint、Debug/Release 构建；API 37 模拟器已验证冷启动、完整页面流程、SoundPool READY、约 100Hz 双 Sensor 注册/采样/后台注销/恢复单次重注册、两次合成 sensor 注入各产生一次 impact，以及 Result/Home 统计持久化。
- 模拟器注入暴露并修复“真实双信号峰早于 swing candidate 时漏掉 impact”的窗口相位问题：ImpactDetector 现在可回看 candidate 前约 100ms 的合格 prominence，并按滤波活动量乘积选择局部峰。真实手机挥拍体验仍未标记通过。

## 2026-08-12 — 击球音震主观同步修正

- 排查确认反馈调度先调用 SoundPool、后调用震动，主观震动抢先并非代码调用顺序导致；三个 WAV 的主击球瞬态原本均位于播放后约 121ms。
- 仅裁去音效的弱前奏，将主瞬态提前至约 9ms；未移动 Virtual Impact、震动或 session 计数时机，以免改变已认可的击球点手感。
- 新增音频资源回归测试，要求三档 WAV 的最强 1ms 瞬态位于前 15ms。实际扬声器输出延迟与音震同步仍需真机验证。

## 2026-08-13 — 安全低延迟路径与方向误触过滤

- 为高置信度双信号挥拍增加提前确认路径，不再等待加速度和角速度完全回落；普通或不确定动作仍保留原峰后确认路径。
- 保留三轴低通方向，在最近 100ms 内仅否决持续由屏幕法线 Z 轴旋转主导、且整段强度低于真实挥拍保护线的动作；方向不明确或高强度动作继续放行。
- 回放验证 session5/6/8/9 分别保留 10/10、10/10、22/22、21/21 个既有 Impact；session10 的 7 次屏幕平行误触变为 7 次 `direction_reject`、0 次 Impact。
- session9 的 21 次标准挥拍全部走提前确认路径，记录的高置信度点到检测延迟约一个采样周期（约 4–5ms）；该点通常比旧算法最终选峰早约 20–30ms。真实音效是否过早/跟手和新动作角度覆盖仍需真机验证。

## 2026-08-13 — 低置信误触的因果约束与采样率适配

- 保持高置信提前确认路径及阈值不变；仅约束低置信 `FALL_CONFIRMED`：峰值必须来自最近 500ms 的同一次前挥、角速度和动态加速度相对前挥起点共同增长，并在峰后 150ms 内出现双信号回落。
- 将方向证据冻结在候选峰时刻，避免延迟确认时拿后续收拍/游走数据重算方向；2 秒窗口继续用于完整动作缓存和高置信恢复，不再作为低置信峰的长期授权。
- 固定低通 `alpha=.22` 改为按真实 timestamp 计算的 8Hz cutoff；这在现有约 200Hz 设备上保持原响应，同时避免 50/100/200Hz 设备因采样率不同得到不同截止频率。没有按传感器 `maximumRange` 缩放阈值，避免在缺少跨设备证据时放宽误触。
- 诊断回放结果：session8 由 22 降至 20、session11 由 22 降至 20、session12 由 2 降至 0、session13 由 4 降至 2；session6/9 保持 10/21 且高置信检测延迟仍约 4–5ms。session5 为 9（开头一个低能量、长延迟、方向占优事件被拒绝）。这些文件已参与诊断和调参，只作为机制回归；真实验收必须使用 session14 及之后的新数据。

## 2026-08-13 — 本局最高挥速

- Virtual Impact 携带对应峰值的滤波角速度；只在反馈成功、计数成立时更新本局最高值，避免普通移动或播放失败污染统计。
- Result 页面增加“估算最高挥速”，内部持久化手机陀螺仪实测峰值，并按用户指定的 180cm 男性、0.80m 肩到手机有效半径和 `v = ωr` 换算为 `km/h`；这是体验比较指标，不代表雷达实测球速。
