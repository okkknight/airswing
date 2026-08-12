# 空气挥拍：项目上下文

最后更新：2026-08-12

## 产品摘要

空气挥拍是一个离线 Android 体感玩具：用户握住手机做羽毛球挥拍动作，应用从加速度计和陀螺仪数据中推断最自然的 Virtual Impact，并立刻播放短促、真实的击球音效和震动。

它不是羽毛球教学、动作纠错、球速/落点预测或比赛模拟产品；MVP 也不含账号、后端、云端 AI、社区或排行榜。

## 当前实现状态

- 当前阶段：Milestone 1（Android 骨架）**未执行**。
- 已有内容：MVP 产品与技术设计、面向实施的工程约束、本地 Git 初始提交。
- 尚无内容：Gradle 工程、Android 源码、测试、音频资源、可构建 APK 或真机验证结果。
- 当前最新任务：建立项目交接基线；执行状态：**验收通过**。

## 权威来源与阅读顺序

1. `空气挥拍_MVP技术设计文档.md`：产品定义、算法路线、里程碑、验收标准的事实来源。
2. `AGENTS.md`：实施纪律、实时链路约束、验证边界与完成标准。
3. 本文件：当前实现状态、导航和未决事项；不替代以上两份文档。
4. `docs/handoff/CHANGELOG.md`：已发生的长期有效变更。

## 核心架构与状态流

目标实时链路：

```text
Accelerometer + Gyroscope
→ SensorEngine / normalization / filtering / ring buffer
→ SwingDetector（完整挥拍状态机与自适应候选）
→ ImpactDetector（加速度与角速度局部峰值融合）
→ 同一 VirtualImpactEvent 立即触发 SoundPool + Haptic
→ 异步 StrokeSegment / StrokeClassifier（Milestone 5）
```

产品状态：`LOADING → READY → SWINGING → HIT_FEEDBACK → READY`；后台或失焦进入 `PAUSED`。高频传感器状态不得驱动 Compose 按采样频率重组。

## 已确认的技术决策

- 平台与基础栈：Android、Kotlin、Jetpack Compose、Android Sensor Framework。
- 必需传感器：Accelerometer 与 Gyroscope；无陀螺仪则明确告知设备不支持完整体验。
- 采样目标：100–200Hz；算法按真实 timestamp/delta 处理，不假定固定帧率。
- 挥拍检测：参考 BadminSense 的完整挥拍阶段、滑窗与峰值候选；禁止简化为单一 `gyroMag > threshold`。
- Virtual Impact：参考 2018 IMU Peak Function 的双信号局部突出度思路；手机端采用模长/动态主轴等工程适配，不硬编码手表坐标轴。
- 反馈：短音效必须预加载到 `SoundPool`；击球路径不做 I/O、网络访问或临时解码。分类可以晚到，但绝不能阻塞声音和震动。
- 所有阈值、窗口、滤波、offset 与 cooldown 集中在 `RecognitionConfig`；论文数值只作初始参考，不能直接硬编码为手机结论。

## 快速导航

| 文件 | 用途 |
| --- | --- |
| `空气挥拍_MVP技术设计文档.md` | 产品、交互、算法、架构、里程碑和验收 |
| `AGENTS.md` | 开发范围、性能、调试和验证规则 |
| `PROJECT_CONTEXT.md` | 当前项目快照（本文件） |
| `docs/handoff/CHANGELOG.md` | 追加式关键变更记录 |

设计文档中最常需要查阅的章节：第 14–20 节（M1–M4 架构、传感器、检测与反馈）、第 21 节（分类）、第 25 节（里程碑）以及第 10 节（验收）。

## 已验证的命令与运行事实

- `git status --short --branch`：已验证本地仓库在 `main` 分支；交接包生成前工作树干净。
- `git log --oneline -3`：已验证初始提交为 `34a73f9 docs: add AirSwing MVP design`。
- 未运行构建、单元测试、模拟器或真机：仓库尚无 Android 工程文件，不能将设计结论表述为已实现或已验证体验。

## 运行与验证注意事项

- 自动测试应覆盖纯识别逻辑（状态、cooldown、无效序列、窗口与参数边界）。
- 真实 Android 手机才可验证击球时机、音频跟手、震动质感、连拍与误触；模拟器/单测均不能替代。
- Debug build 需要最小可观测性：timestamp、detector state、candidate/impact、cooldown、impact 到反馈调用时序；调试采集不能进入正式 UI。

## 未决事项与风险

- 尚未确认 Android 的最低/目标 SDK、包名、应用名称、首批真实音效资源和测试机型；这些需在 Milestone 1 前由实现者或项目负责人确定。
- 手表研究迁移到直接握持手机存在坐标系、惯量、量程和采样率的 domain gap；先以固定握法、模长/相对峰值和每设备 baseline 适配，不能未经实机证据就宣称手感成立。
- BADS_CLL 为后备诊断资源，默认不下载或复现实验；若未来商业化，不能默认把其 CC BY-NC-ND 4.0 数据当作训练资产。
- 当前变更只新增交接文档，不会影响既有功能；后续 M1 的工程初始化会成为后续所有模块的基础，需要谨慎确定包结构与 SDK 边界。

## 未来 Agent 工作规则

- 每次任务开始先阅读 `AGENTS.md`，再阅读设计文档中相关章节，并检查当前 Milestone、代码和测试。
- 优先级：用户当前指令 > 设计文档 > `AGENTS.md` > 现有代码。
- 严格按 M1 到 M6 顺序推进；先形成最小闭环、自动验证、真机验证和修正，再进入下一阶段。
- 不提前引入后端、复杂 DI、多模块、云端模型或大规模自采数据；不为方便而替换已选研究路线。
- 若核心路线确需改变，更新设计文档并显式记录偏离；不要只把新设计藏在代码或 `AGENTS.md` 中。
