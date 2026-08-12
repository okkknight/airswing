# 空气挥拍MVP 产品与技术设计文档

> 版本：v2.0  
> 日期：2026-08-11  
> 目标读者：Codex / Android 开发者 / 产品实现者  
> 平台：Android  
> 技术栈：Kotlin + Jetpack Compose + Android Sensor Framework  
> 文档目标：一份文档同时说明“做什么、怎么玩、怎么实现、如何验收”。

---

## 1. 产品定义

“空气挥拍”是一款玩票性质的安卓体感应用。用户把手机当作羽毛球拍握在手中，在空气中做羽毛球挥拍动作；系统通过手机内置 IMU（Accelerometer + Gyroscope，必要时辅助 Rotation Vector）识别挥拍过程，并在最接近真实“击球瞬间”的位置立即播放真实羽毛球击球音效，同时触发短促震动。

它不是羽毛球教学 App，也不是专业动作分析工具。第一阶段最重要的产品价值只有一句话：

> **让用户挥动手机时，产生“我刚刚真的用球拍击中了一个羽毛球”的错觉。**

产品体验优先级从高到低：

1. **击球反馈跟手**：声音和震动落在用户主观认为应该击球的瞬间；
2. **挥拍识别可靠**：真正挥拍能触发，普通拿手机、翻转、走动尽量不触发；
3. **连续可玩**：连续挥十几次仍然稳定，一拍只响一次；
4. **反馈有质感**：击球声音真实，震动短、硬、干净；
5. **动作识别**：核心体验稳定后，再识别正手、反手、杀球等动作；
6. **个性化**：只有通用方案确实不足时再加入轻量校准或个人模型。

### 1.1 产品不是在做什么

第一版不以以下能力作为卖点：

- 判断动作是否标准；
- 给用户羽毛球技术评分；
- 预测真实球速和落点；
- 模拟完整羽毛球比赛；
- AI 教练；
- 社区、排行、账号体系。

核心产品不是“识别技术动作”，而是一个**低延迟体感反馈玩具**。动作识别是这个核心上的第二层玩法。

---

## 2. MVP 产品目标与推进顺序

MVP 不缩成一次性的传感器 Demo，而按照最初确定的完整路线推进。

### 阶段 A：核心击球体验

实现：

```text
手机挥拍
  ↓
识别完整挥拍事件
  ↓
定位 Virtual Impact（虚拟击球点）
  ↓
立即播放真实击球音效 + 短促震动
  ↓
记录一次有效挥拍
```

这一阶段允许完全不知道用户挥的是正手还是反手，但必须把“啪”的时机做对。

### 阶段 B：基础动作识别

优先支持现有公开研究覆盖较好的动作：

- 正手头顶高远球（Forehand Overhead Clear）
- 反手头顶高远球（Backhand Overhead Clear）
- 正手杀球（Forehand Overhead Smash，可作为第三类）

低手挑高球不作为 MVP 必需项；不是因为产品不需要，而是现有公开研究覆盖弱，没有必要为了它提前建立自己的数据集。

### 阶段 C：个性化与稳健性

仅在实机证明确实需要时增加：

- 惯用手参与坐标/分类；
- 3～5 次快速个人动作校准；
- 阈值、尺度、坐标自适应；
- 用户自己的动作模板；
- 轻量 personalized classifier。

> **原则：先吃透公开研究，再做自己的研究；先使用现成方法，再补自己的数据。**

---

## 3. 目标用户与使用场景

这是一个“拿起来就能玩”的轻产品，不要求用户懂传感器、训练模型或羽毛球动作学。

典型场景：

- 在家或空旷位置，突然想挥两下；
- 羽毛球爱好者拿手机体验“空气击球”的反馈；
- 给朋友展示一个有趣的体感小玩具；
- 后续动作识别成熟后，用不同挥拍动作触发不同真实击球声音。

### 3.1 产品设计约束

用户挥拍时基本不会持续看屏幕，因此：

- **声音 + 震动是主交互；**
- 屏幕信息必须大、少、瞬间可读；
- 不设计需要边挥边点的小按钮；
- 核心状态不能依赖文字说明才能理解；
- 练习中不要弹 Toast、Dialog 或打断性 UI。

---

## 4. 核心用户体验

### 4.1 第一次打开

目标：用户第一次安装后，**10 秒左右就能挥出第一声有效“啪”**。

建议流程：

```text
启动 App
  ↓
检查 Gyroscope / Accelerometer
  ↓
一句话介绍：把手机当成羽毛球拍
  ↓
选择惯用手：右手 / 左手
  ↓
显示推荐握法
  ↓
[开始挥拍]
  ↓
音效预加载完成
  ↓
READY
  ↓
第一次挥拍 → 啪 + 震
```

首次体验不要要求用户：

- 注册；
- 登录；
- 填资料；
- 录几十次训练动作；
- 看长教程；
- 先做论文式校准。

### 4.2 非首次打开

直接进入首页：

```text
空气挥拍

[ 开始挥拍 ]

上次：46 拍
```

点击开始后进入 Play Screen。惯用手和握法不变时不重复教学。

---

## 5. 页面设计

MVP 正式用户界面只需要 4 个页面/状态面板。

### 5.1 Home Screen

```text
空气挥拍

      [ 开始挥拍 ]

上次挥拍  46
累计挥拍  312

              设置
```

设计原则：

- “开始挥拍”是绝对主按钮；
- 不堆训练课程、排行、资讯；
- 统计只是辅助，不抢核心入口；
- 第一次使用时点击开始进入 Setup，之后直接进入 Play。

### 5.2 Setup / Grip Screen

首次使用或用户主动重新设置时显示。

```text
像握球拍一样握住手机

[简洁握持示意]

惯用手
● 右手   ○ 左手

[ 开始 ]
```

产品上只要求一种推荐握法，减少坐标系混乱。不要给用户多种“自由握法模式”。

MVP 可先规定：

- 手机长轴大体对应球拍杆方向；
- 手握手机下半部；
- 屏幕朝向保持统一；
- 右手/左手记录下来供后续动作分类使用。

### 5.3 Play Screen

这是核心页面。

待机：

```text
READY

  12

挥一下
```

成功击球瞬间：

```text
   ●

  13

啪！
```

视觉只做短暂反馈，真正反馈来自声音与震动。

阶段 B 加入动作识别后：

```text
READY

  13

正手高远
```

分类结果可以晚 100～300ms 出现，但声音和震动绝不能等它。

Play Screen 推荐交互：

- 中央：本次 Session 挥拍数；
- 顶部：当前状态 READY / PAUSED；
- 底部：最近一次动作类型（阶段 B）；
- 一个明显的结束按钮；
- 可选静音/震动开关放到设置，不要占主界面。

### 5.4 Result Screen

结束一次 Session 后：

```text
本次挥拍

46 拍

正手高远  18
反手高远  11
杀球       7
未识别    10

[ 再来一局 ]
[ 返回首页 ]
```

阶段 A 尚未动作分类时只显示：

```text
本次挥拍
46 拍

[ 再来一局 ]
```

不要为了“数据丰富”展示没有可靠意义的假指标，例如虚构球速、动作评分、卡路里。

---

## 6. Play 状态与交互状态机

产品层状态与底层识别状态分开。

```text
LOADING
  ↓
READY
  ↓
SWINGING
  ↓
HIT_FEEDBACK
  ↓
READY
```

另外支持：

```text
PAUSED
UNSUPPORTED_DEVICE
SENSOR_ERROR
```

### 6.1 LOADING

进入 Play 后：

- 注册 Sensor；
- 初始化 Ring Buffer；
- preload SoundPool；
- 初始化震动能力；
- 完成后才显示 READY。

### 6.2 READY

正常监听挥拍，不需要用户再点击“准备”。

### 6.3 SWINGING

底层检测到候选挥拍后进入内部状态；UI 可以完全不变化，避免让用户追着屏幕反馈挥拍。

### 6.4 HIT_FEEDBACK

Virtual Impact 被确认时，同一事件同时执行：

```text
SoundPool.play()
VibrationEffect
strokeCount + 1
极短视觉 pulse
```

动作分类异步继续。

### 6.5 PAUSED

App 进入后台、页面失焦或用户主动暂停时：

- 停止有效挥拍计数；
- 停止/降级 Sensor 监听；
- 回到前台后重新 READY；
- 不要把拿起手机返回 App 的动作误认为挥拍。

---

## 7. 击球反馈设计

这是产品的灵魂，优先级高于动作分类。

### 7.1 音效

至少准备三档真实羽毛球击球 sample：

```text
hit_soft.wav
hit_medium.wav
hit_hard.wav
```

要求：

- 是真实羽毛球/球拍质感，不用游戏里夸张的“爆炸声”；
- 前沿清晰，不能有明显静音头；
- 文件短；
- 进入 Play 时全部 preload；
- 每档后续可以准备 2～3 个轻微不同 sample 随机轮换，减少机械重复感。

强度映射使用挥拍/impact strength，而不是随机选择。

阶段 B 后可以再探索不同动作不同音色，但**不要因为等动作分类而推迟声音**。

### 7.2 震动

目标：像线床碰球时的一下瞬时冲击。

要求：

- 短；
- 硬；
- 不拖尾；
- 不做手机通知那种长“嗡”；
- 与音效由同一个 `VirtualImpactEvent` 触发。

### 7.3 视觉反馈

只作为辅助：

- 数字瞬间放大/回弹；
- 中心短 pulse；
- 可显示一个非常短的“啪”；
- 不做持续 500ms 以上的大动画，否则用户连续挥拍时会显得迟钝。

### 7.4 反馈时序原则

产品验收不是“算法 timestamp 看起来正确”，而是：

> **闭眼挥拍时，用户觉得声音就是从那次动作里长出来的。**

因此 `impactOffsetMs` 属于产品手感参数，不是论文常数。

---

## 8. 误触、漏触和异常体验

### 8.1 普通移动误触

以下动作不应该频繁触发：

- 从桌上拿起手机；
- 慢慢翻转手机；
- 走路；
- 坐下；
- 手里随意轻晃。

如果候选信号不够强，产品行为应该是：**什么也不发生。**

不要显示“动作错误”“识别失败”。这不是教学产品。

### 8.2 挥拍漏触

如果明显挥拍偶尔没有触发，也不要弹错误提示。Debug 构建记录内部指标即可。

### 8.3 重复触发

一次动作只能产生一次主要击球反馈。一次挥拍出现多个 sensor peak 时，后续 peak 应由 cooldown / re-arm 机制吸收。

### 8.4 连续挥拍

不能因为 cooldown 太保守，让第二拍明显漏掉。产品应支持用户自然连续挥：

```text
啪    啪    啪    啪
```

而不是：

```text
啪          啪
```

### 8.5 不支持陀螺仪

若设备没有 Gyroscope：

```text
这台设备缺少陀螺仪，无法获得完整的空气挥拍体验。
```

MVP 不必为少数无陀螺仪设备设计一套低质量兼容算法。

---

## 9. 产品数据与隐私

MVP 完全离线，不需要服务器。

正式保存：

```text
Session
- startedAt
- endedAt
- strokeCount

StrokeRecord
- timestamp
- strength
- type（阶段 B）
- confidence（阶段 B）
```

不默认长期保存高频原始 IMU 流。

开发版 Debug Recorder 可以临时记录 Sensor Stream，并允许手动导出 CSV / JSON；它是研发工具，不是产品使用前置流程。

---

## 10. 产品级 MVP 验收标准

### P0：第一次体验

- [ ] 新用户无需注册；
- [ ] 简短握法说明后即可开始；
- [ ] 不要求先录训练数据；
- [ ] 第一次使用很快就能产生有效击球反馈。

### P0：核心手感

- [ ] 连续真实挥十几次，大部分挥拍都只触发一次；
- [ ] “啪”主观上落在合理击球时刻；
- [ ] 音频无明显启动迟滞；
- [ ] 震动与声音基本同步；
- [ ] 连续挥拍可自然重新触发；
- [ ] 普通拿起/翻转手机不会频繁误触。

### P1：动作反馈

- [ ] 支持 FOREHAND_CLEAR / BACKHAND_CLEAR / SMASH / UNKNOWN；
- [ ] 分类置信度不足时输出 UNKNOWN；
- [ ] 分类结果不能阻塞击球反馈；
- [ ] Result Screen 能显示本次动作统计。

### P2：产品完整性

- [ ] 首页 → 开始 → Play → 结束 → Result 流程完整；
- [ ] App 进入后台不会乱计数；
- [ ] 设备不支持时有明确状态；
- [ ] 无后端也能完成全部核心体验。

---

## 11. 明确不做与后续空间

### MVP 不做

- 专业动作纠正；
- 击球质量评分；
- 真实球速；
- 落点预测；
- 低手挑高球专门识别；
- 登录/账号；
- 云端 AI；
- 社区排行榜；
- 大规模自建训练数据；
- 论文完整复现实验。

### MVP 成立后的自然扩展

在核心“挥拍 → 啪”的手感成立后，再考虑：

```text
更多动作
↓
不同动作不同击球声
↓
个人校准
↓
挥拍历史
↓
小游戏 / 连击玩法
↓
更深入的羽毛球动作分析
```

扩展顺序必须服从核心体验，不能为了功能数量破坏低延迟和简单性。

---

## 12. 研发原则

### 3.1 不从零发明已有算法

第一版必须优先吸收现有羽毛球 / 球拍 IMU 研究成果，尤其是：

1. **BadminSense (CHI 2026)**：借鉴 Stroke Segmentation、完整挥拍时间窗、Peak Detection、Stroke Classification 特征工程与 SVM 路线。
2. **Wearable Audio and IMU Based Shot Detection in Racquet Sports (2018)**：借鉴 IMU Peak Function（IPF）思想，将加速度与角速度的局部峰值共同用于击球候选点检测。

### 3.2 不把学术复现作为前置任务

不要求：

```text
下载完整公开数据集
→ 重跑论文所有实验
→ 复现 91% / 99% 指标
→ 才开始写 Android
```

正确做法：

```text
阅读并理解论文方法
→ 按论文方法实现
→ 直接在“手持手机”场景做实机工程验证
→ 只对手机场景产生的问题调参或改造
```

公开数据集保留为后备诊断工具，而不是必经步骤。

### 3.3 优先工程验证，避免过早训练大模型

第一版挥拍检测和 Virtual Impact 不使用神经网络。

动作分类优先采用：

```text
6-axis IMU
→ 特征提取
→ SVM
```

如果传统算法已经足够稳定，不引入深度学习。

---

## 13. 现有研究如何直接为本项目所用

### 13.1 BadminSense：挥拍切分与动作分类主参考

BadminSense 使用普通智能手表采集 100Hz IMU 数据，针对 12 名有经验的羽毛球爱好者采集了 848 个有效击球样本，并实现了挥拍切分与动作分类。

其 Stroke Segmentation 的核心观察：

- 一个完整挥拍通常可以分成：
  - backward swing
  - forward swing
  - impact
  - follow-through
  - retraction
- impact 附近通常出现最显著的陀螺仪峰值；
- 论文采用滑动窗口 + gyro peak detection 先找候选挥拍；
- 实验中使用 2000ms 窗口；
- 对原始值平方后做 local maximum threshold；
- 观察到 gyro 峰通常略早于实际 impact，因此使用了约 100ms offset。

论文自己的阈值（例如 21）是**手表数据上的经验值，不允许直接当作手机固定阈值使用**。

本项目应继承的是：

> “明显旋转峰值 → 候选挥拍 → 峰值附近对应 impact 区域”这一结构，而不是机械照搬绝对数值。

BadminSense 在自己的 IMU + Audio 双阶段分割条件下报告了 99.41% segmentation accuracy。空气挥拍不存在真实击球声音，因此本项目不使用麦克风验证：我们恰恰需要把 IMU 找到的“最像击球的位置”直接定义成 Virtual Impact。

动作分类方面，BadminSense 的处理链为：

```text
6-axis IMU
(acc x/y/z + gyro x/y/z)
        ↓
去掉窗口头尾各 200ms
        ↓
二阶 20Hz Low-pass Filter
        ↓
每个轴提取时域 + 频域特征
        ↓
6 × 23 features
        ↓
SVM
```

特征包括：

- sum / mean
- variance / standard deviation
- skewness / kurtosis
- percentiles
- peak count
- 一阶导数相关特征
- FFT spectral energy
- Welch PSD max / mean

BadminSense 报告：

- 普通 5-fold：SVM accuracy 95.05%
- leave-users-out：SVM accuracy 91.43%

本项目阶段 B 直接借鉴这一路线，而不是自己重新猜应该提哪些特征。

---

### 13.2 2018 IMU Peak Function：Virtual Impact 主参考

2018 年三星研究人员提出了一种轻量实时 IMU shot detector。

关键思想：挥拍可以近似看成变化半径、变化速度的圆周运动。研究中将：

```text
radial acceleration ≈ 某主轴 acceleration

tangential angular velocity = sqrt(另外两个 gyro 轴平方和)
```

在其腕部坐标系里定义为：

```text
a_rad = a_x
ω_tan = sqrt(ω_y² + ω_z²)
```

然后：

- 对两条信号使用 10Hz IIR low-pass；
- 使用约 100ms 的局部窗口；
- 计算当前值相对于邻域均值的突出程度；
- 将 acceleration peak 与 angular-velocity peak 结合形成 IMU Peak Function（IPF）；
- IPF 在击球附近形成明显峰值。

**本项目不应硬编码 a_x / gyro_yz。** 手机握法和手表坐标不同，因此要把这个思路改造成手机版：

```text
优先使用归一化后的主运动轴 / 模长 / 动态主轴
+
局部峰值 prominence
+
局部角速度峰值
+
局部加速度峰值
```

将二者组合成 `impactScore`。

第一版建议：保留 IPF 的“相对局部窗口突出度”思想，而不是照抄原坐标轴。

---

### 13.3 BADS_CLL 公开数据集的角色

BadminSense 开源 BADS_CLL：

- 848 个有效 stroke；
- 约 14GB；
- 包含 accelerometer、gyroscope、uncalibrated gyro、magnetic、audio、视频；
- 四类动作：
  - Backhand Overhead Clear
  - Forehand Overhead Clear
  - Forehand Overhead Smash
  - Forehand Overhead Drop

本项目对该数据集的态度：

### 默认不下载、不复现

MVP 不依赖它才能开始。

### 以下情况再使用

- 手机端某个算法表现异常，需要判断算法实现本身是否有问题；
- 后续要研究通用动作分类模型；
- 需要分析真实羽毛球 IMU 曲线的相位结构；
- 需要对照正手 / 反手 / 杀球的特征差异。

许可为 **CC BY-NC-ND 4.0**。当前玩票、非商业研究阶段可作为参考；未来若商业化，不默认把该数据集直接作为产品训练资产。

---

## 14. Android 技术架构

建议包结构：

```text
app/
├── ui/
│   ├── HomeScreen.kt
│   ├── SetupScreen.kt
│   ├── PlayScreen.kt
│   ├── ResultScreen.kt
│   ├── CalibrationScreen.kt      # 阶段 C 按需
│   └── DebugScreen.kt            # 开发构建
│
├── sensor/
│   ├── SensorEngine.kt
│   ├── SensorFrame.kt
│   ├── SensorRingBuffer.kt
│   ├── SensorNormalizer.kt
│   └── MotionFilter.kt
│
├── recognition/
│   ├── SwingDetector.kt
│   ├── ImpactDetector.kt
│   ├── StrokeSegment.kt
│   ├── StrokeClassifier.kt
│   ├── FeatureExtractor.kt
│   └── RecognitionConfig.kt
│
├── feedback/
│   ├── AudioEngine.kt
│   └── HapticEngine.kt
│
├── domain/
│   ├── StrokeEvent.kt
│   ├── StrokeType.kt
│   └── SessionStats.kt
│
└── debug/
    ├── SensorRecorder.kt
    └── DebugMetrics.kt
```

整体实时链路：

```text
SensorManager
     ↓
SensorEngine
     ↓
SensorNormalizer
     ↓
MotionFilter
     ↓
SensorRingBuffer
     ↓
SwingDetector
     ↓
ImpactDetector
     ├────────────→ AudioEngine
     ├────────────→ HapticEngine
     ↓
StrokeSegment
     ↓
StrokeClassifier（阶段 B）
     ↓
UI / SessionStats
```

---

## 15. Sensor 层设计

### 15.1 必需传感器

第一版必须：

```text
TYPE_ACCELEROMETER
TYPE_GYROSCOPE
```

可选辅助：

```text
TYPE_ROTATION_VECTOR
TYPE_GRAVITY / TYPE_LINEAR_ACCELERATION
```

Android 官方说明 accelerometer 与 gyroscope 属于硬件 motion sensors，可直接通过 `SensorManager` 获取；应用启动时必须检测设备能力，若无 gyroscope，应明确提示当前设备不支持最佳体验。

### 15.2 采样率

目标：

```text
100Hz ～ 200Hz
```

建议：

- 初始尝试 200Hz；
- 若设备实际回调频率较低，以 timestamp 计算真实 delta；
- 所有算法按真实时间而不是“第 N 帧”处理；
- 不依赖固定回调间隔。

数据结构：

```kotlin
data class SensorFrame(
    val timestampNs: Long,
    val ax: Float,
    val ay: Float,
    val az: Float,
    val gx: Float,
    val gy: Float,
    val gz: Float,
    val rotation: Quaternion? = null
)
```

另计算：

```text
accMag  = sqrt(ax² + ay² + az²)
gyroMag = sqrt(gx² + gy² + gz²)
```

---

## 16. 第一核心：Swing Detector

目标：从连续 IMU 流中识别：

> “用户正在进行一次明显的球拍式高速挥动。”

### 7.1 不采用单阈值方案

禁止只做：

```text
if gyroMag > X:
    HIT
```

因为会导致：

- 随手甩手机误触；
- 一次挥拍多次触发；
- 不理解完整动作时序。

### 7.2 状态机

建议：

```text
IDLE
 ↓
ARMED
 ↓
ACCELERATING
 ↓
PEAK_CANDIDATE
 ↓
IMPACT
 ↓
FOLLOW_THROUGH
 ↓
COOLDOWN
 ↓
IDLE
```

逻辑参考 BadminSense 的完整 stroke phase，而不是要求真正分割每一个生物力学阶段。

### 7.3 候选挥拍判断

维护约 2 秒 Ring Buffer。

计算：

```text
gyroMag
accMag
angular jerk / acceleration derivative
局部方差
```

使用**自适应基线**：

```text
baselineGyro = 最近静稳区间 gyroMag 的中位数 / EMA
baselineAcc  = 最近静稳区间 acc 动态部分的统计量
```

候选判断倾向使用：

```text
current / baseline
local prominence
rise speed
持续时间
```

而不是直接照抄 BadminSense 的 threshold=21。

---

## 17. 第二核心：Virtual Impact Detector

这是整个 App 最重要的模块。

目标不是判断真实羽毛球是否碰到球拍，而是：

> **预测如果当前手机是一支羽毛球拍，最合理的虚拟触球瞬间在哪里。**

### 8.1 impactScore

第一版建议组合：

```text
G(t) = filtered gyro activity
A(t) = filtered acceleration activity

localGyroProminence(t)
localAccProminence(t)
```

可以构建：

```text
impactScore(t)
    = normalized(localGyroProminence)
    × normalized(localAccProminence)
```

这直接借鉴 IPF 的思想：两个信号必须在同一局部时间区域同时显著突出。

同时可以加入：

```text
+ gyroMag peak strength
+ acceleration derivative / jerk
+ forward-swing confidence
- noise penalty
```

最终由实机体验决定权重。

### 8.2 Peak Timing

BadminSense 发现其 gyro peak 略早于实际 impact，因此用了约 100ms offset。

空气挥拍没有真实 impact ground truth，因此：

- **不要固定照搬 +100ms**；
- 将 offset 作为可配置参数 `impactOffsetMs`；
- 初值可从研究结论获得启发，比如 30～100ms 范围；
- 最终以“声音是否跟手”为唯一判据。

### 8.3 不让动作分类阻塞击球反馈

必须采用双路径：

```text
IMU stream
   ↓
Virtual Impact detected
   ├────→ 立即声音 + 震动
   │
   └────→ 等后续 follow-through 数据补齐
              ↓
          StrokeSegment
              ↓
          StrokeClassifier
```

因此：

> **即使动作分类需要 100～300ms，击球声音也不能等分类完成。**

---

## 18. 防止多次触发

一次挥拍的 IMU 曲线可能存在多个局部峰。

因此 `ImpactDetector` 成功触发后进入：

```text
COOLDOWN
```

初始：

```text
250 ～ 400ms
```

但不要简单依赖固定 cooldown。

更好的 re-arm 条件：

```text
时间超过最小 cooldown
AND
gyro activity 已明显回落
AND
新的 acceleration rising edge 出现
```

这样未来可以支持快速连续挥拍。

---

## 19. 低延迟音频

采用 Android `SoundPool`。

理由：官方明确将 SoundPool 用于预解码到内存的短音效和低延迟播放。

设计：

```text
进入 PlayScreen
  ↓
预加载全部 hit samples
  ↓
OnLoadComplete 后进入 Ready
  ↓
VirtualImpactEvent
  ↓
SoundPool.play()
```

第一版准备至少：

```text
hit_soft.wav
hit_medium.wav
hit_hard.wav
```

后续可以增加：

```text
forehand_clear.wav
backhand_clear.wav
smash.wav
```

### 力度映射

根据 peak strength / impactScore：

```text
弱 → soft
中 → medium
强 → hard
```

不要在挥拍发生后读文件、创建播放器或解码音频。

---

## 20. 震动设计

击球触觉要求：

> 短、硬、干净，不要长时间“嗡”。

Android 可使用预定义 `VibrationEffect`，例如 CLICK/TICK，设备会在支持时使用优化效果，并在不支持时回退到平台实现。

第一版：

```text
Impact Event
  ├─ SoundPool.play()
  └─ Vibrator.vibrate(...)
```

两者必须由同一个事件同步触发。

后续再根据 Android 版本和设备能力尝试自定义 waveform / composition。

---

## 21. 阶段 B：动作分类

核心击球体验稳定后接入。

### 12.1 第一批动作

```text
FOREHAND_CLEAR
BACKHAND_CLEAR
SMASH
UNKNOWN
```

UNKNOWN 必须是合法结果，不应强迫每次挥拍都分类。

### 12.2 窗口

`ImpactDetector` 提供 `impactTimestamp`。

从 Ring Buffer 截取完整 stroke，例如：

```text
impact 前 700～1000ms
+
impact 后 700～1000ms
```

具体时间最终结合 2 秒 BadminSense 窗口和手机实际动作调整。

### 12.3 预处理

第一版直接参考 BadminSense：

```text
6-axis IMU
↓
低通滤波（初始 cutoff 20Hz）
↓
去除窗口边缘噪声
↓
FeatureExtractor
```

### 12.4 FeatureExtractor

实现 BadminSense 风格 23-feature 集合：

```text
时域：
sum
mean
variance
std
skewness
kurtosis
percentiles
peak count
...

一阶导数：
统计特征

频域：
FFT energy
frequency statistics
Welch PSD max
Welch PSD mean
```

最终：

```text
6 axes × 23 = 138 dimensions
```

### 12.5 分类器

优先：

```text
SVM
```

不要求第一天训练自有手机模型。

阶段 B 可先有两种实现路径：

**路径 1：规则 / prototype classifier**  
先验证手机上的正反手是否呈现明显轴方向差异。

**路径 2：SVM**  
一旦获得可合法使用的模型训练来源或少量手机适配数据，就替换 classifier 实现。

`StrokeClassifier` 必须抽象成接口，避免 UI / SensorEngine 与模型耦合。

```kotlin
interface StrokeClassifier {
    fun classify(segment: StrokeSegment): StrokeClassification
}
```

---

## 22. “手机 vs 手表”Domain Gap 的处理策略

这是本项目真正的新问题。

已有研究通常是：

```text
手腕 → smartwatch IMU
```

本项目：

```text
手掌直接握住 → phone IMU
```

可能差异：

- 质量和转动惯量不同；
- IMU 在手掌而不是手腕；
- 三轴坐标不同；
- 不同手机传感器量程不同；
- 高速挥动时可能发生 saturation。

第一版先通过工程归一化降低差异：

1. 固定推荐握法；
2. 使用 gyro/acc magnitude 作为部分通用特征；
3. 使用局部相对峰值而非固定绝对值；
4. 使用每台设备自己的静态 baseline；
5. 将所有阈值集中在 `RecognitionConfig`，方便快速实机调参。

例如：

```kotlin
data class RecognitionConfig(
    val minSwingScore: Float,
    val minImpactScore: Float,
    val impactOffsetMs: Long,
    val cooldownMs: Long,
    val lowPassCutoffHz: Float,
    val ringBufferMs: Long
)
```

**禁止把论文参数散落在业务代码里。**

---

## 23. 什么时候才开始自采动作数据

不是 MVP 前置条件。

仅当出现明确问题，例如：

```text
Virtual Impact：稳定
正手/反手：手机实测长期只有较低准确率
```

才进入手机数据阶段。

优先级：

### Level 1：无需“采数据”式操作

用户只是正常玩 App，开发版自动保存少量本地 Debug Session。

### Level 2：轻量校准

```text
正手 × 3～5
反手 × 3～5
```

用于：

- axis mapping；
- scale normalization；
- threshold calibration；
- DTW/template personal calibration。

### Level 3：真正自建数据集

只有前两层仍解决不了动作分类泛化问题，才考虑：

```text
多人
×
多设备
×
多动作
×
负样本
```

训练真正的 Phone-IMU classifier。

---

## 24. Debug 能力必须内置，但不能喧宾夺主

开发版本应提供隐藏 `DebugScreen`：

显示：

```text
accMag
gyroMag
swingScore
impactScore
currentState
lastImpactTimestamp
classifier result
```

允许：

- 开关 Sensor Recorder；
- 导出单次 session CSV / JSON；
- 实时调整 RecognitionConfig；
- 查看声音触发 timestamp 与 impact timestamp。

这不是要求用户先采训练数据，而是让 Codex 在实机调试阶段拥有可观测性。

---

## 25. Codex 实施顺序

### Milestone 1 — Android 骨架

- Kotlin + Compose；
- Home / Setup / Play / Result；
- Sensor capability detection；
- SensorEngine；
- RingBuffer；
- SoundPool；
- HapticEngine。

验收：首页、首次握持设置、练习页、结果页流程可走通；能实时读取 accelerometer / gyroscope。

### Milestone 2 — Research-based Swing Detector

- 实现低通滤波；
- gyro/acc magnitude；
- 自适应 baseline；
- 状态机；
- BadminSense-style peak candidate。

验收：正常挥拍大多数能形成唯一候选事件；普通缓慢拿手机基本不触发。

### Milestone 3 — Virtual Impact

- 实现 IPF-inspired impactScore；
- peak prominence；
- impact offset；
- cooldown / re-arm；
- 同步声音 + 震动。

这是 MVP 最重要验收阶段。

验收标准：

> 连续真实挥十几次，绝大部分“啪”都主观落在合理击球时刻；一次挥拍通常只响一次；反馈无明显迟滞。

### Milestone 4 — 强弱击球反馈

- 依据 impact strength 分级；
- soft / medium / hard 三档音效；
- 可选震动强度差异。

### Milestone 5 — Stroke Classification

- StrokeSegment；
- 20Hz preprocessing；
- FeatureExtractor；
- 抽象 StrokeClassifier；
- 正手 / 反手 / 杀球 / UNKNOWN；
- 优先参考 BadminSense SVM 路线。

### Milestone 6 — 个性化（按需）

只有通用方案明显不足才进入：

- 轻量 calibration；
- axis mapping；
- threshold adaptation；
- DTW/template 或 personalized classifier。

---

## 26. 给 Codex 的执行原则

1. **先阅读参考论文中的方法，再写识别代码，不要凭直觉重造一套算法。**
2. 论文中的经验阈值只作为参考，不直接硬编码到手机产品。
3. 第一优先级永远是 Virtual Impact 的“跟手感”，不是动作分类准确率。
4. `ImpactDetector` 一旦确认击球点，必须立即触发音效/震动；动作分类异步完成。
5. 如果实机效果不好，先判断是：坐标系、采样率、滤波、阈值、peak timing、cooldown 中哪一层有问题，不要第一反应就上 ML。
6. 公开研究已经验证过的结论可以直接采用；本项目不承担重复证明论文结论的任务。
7. 只有明确发现 Watch → Phone domain gap 阻碍产品体验，才进入自采数据或个性化训练。

---

## 27. 参考资料

### 核心论文

1. **BadminSense: Enabling Fine-Grained Badminton Stroke Evaluation on a Single Smartwatch**  
   CHI 2026 / arXiv  
   https://arxiv.org/abs/2603.21825  
   重点阅读：5.2 Stroke Segmentation、5.3 Stroke Classification。

2. **Wearable Audio and IMU Based Shot Detection in Racquet Sports**  
   Sharma et al., 2018  
   https://arxiv.org/abs/1805.05456  
   重点阅读：5.1 Signal Preprocessing and IMU Peak Function (IPF)。

### 公开数据

3. **BadminSense BADS_CLL Dataset**  
   https://github.com/taizhouchen/BadminSense_Dataset  
   仅作为后备分析/诊断资源，MVP 不要求预先下载和复现。

### Android 官方文档

4. Motion sensors  
   https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion

5. Sensors Overview  
   https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview

6. SoundPool  
   https://developer.android.com/reference/android/media/SoundPool

7. Android Haptics APIs  
   https://developer.android.com/develop/ui/views/haptics/haptics-apis

---

## 28. 一句话技术路线

> **先用 BadminSense 的“挥拍峰值/时间窗”思想找到一次挥拍，再用 IPF 的“加速度 + 角速度局部峰值融合”思想找到最自然的 Virtual Impact，立即反馈音效和震动；核心体验稳定后，再接 BadminSense 风格的 6-axis 特征 + SVM 做正手/反手/杀球分类，只有手机场景确实暴露 domain gap 时才补少量个人数据或建立自己的数据集。**
