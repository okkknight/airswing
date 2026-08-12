package com.knightspace.airswing

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.knightspace.airswing.domain.SessionStore
import com.knightspace.airswing.domain.Handedness
import com.knightspace.airswing.domain.SessionSummary
import com.knightspace.airswing.domain.StartDestination
import com.knightspace.airswing.domain.startDestination
import com.knightspace.airswing.domain.estimatePeakLinearSpeedKmh
import com.knightspace.airswing.debug.SensorRecorder
import com.knightspace.airswing.feedback.AudioEngine
import com.knightspace.airswing.feedback.FeedbackCoordinator
import com.knightspace.airswing.feedback.FeedbackStatus
import com.knightspace.airswing.feedback.HapticEngine
import com.knightspace.airswing.feedback.feedbackDelayMs
import com.knightspace.airswing.recognition.RecognitionConfig
import com.knightspace.airswing.recognition.RecognitionPipeline
import com.knightspace.airswing.sensor.SensorEngine
import com.knightspace.airswing.sensor.SensorStartResult
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

private const val LOG_TAG = "AirSwing"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AirSwingApp() }
    }
}

private enum class Screen { HOME, SETUP, PLAY, RESULT }
private enum class PlayStatus { LOADING, READY, PAUSED, UNSUPPORTED_DEVICE, SENSOR_ERROR, FEEDBACK_ERROR }

@Composable
private fun AirSwingApp() {
    val context = LocalContext.current
    val store = remember { SessionStore(context) }
    val initialPreferences by produceState<com.knightspace.airswing.domain.AppPreferences?>(initialValue = null) {
        value = store.state.first()
    }
    val preferences by store.state.collectAsState(
        initial = initialPreferences ?: com.knightspace.airswing.domain.AppPreferences(),
    )
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(Screen.HOME) }
    var handedness by remember { mutableStateOf(Handedness.RIGHT) }
    var count by remember { mutableIntStateOf(0) }
    var peakSwingSpeed by remember { mutableFloatStateOf(0f) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.HOME -> Page(
                    title = "空气挥拍",
                    subtitle = "上次 ${preferences.lastCount} 拍 · 累计 ${preferences.totalCount} 拍",
                ) {
                    Button(
                        enabled = initialPreferences != null,
                        onClick = {
                            screen = when (startDestination(preferences.hasSetup)) {
                                StartDestination.SETUP -> Screen.SETUP
                                StartDestination.PLAY -> Screen.PLAY
                            }
                        },
                    ) {
                        Text("开始挥拍")
                    }
                }
                Screen.SETUP -> Page(
                    title = "像握球拍一样握住手机",
                    subtitle = "手机长轴对准球拍杆，握住下半部",
                ) {
                    Row {
                        listOf(Handedness.RIGHT to "右手", Handedness.LEFT to "左手").forEach { (hand, label) ->
                            Button(
                                onClick = { handedness = hand },
                                modifier = Modifier.padding(4.dp),
                            ) { Text(if (handedness == hand) "✓ $label" else label) }
                        }
                    }
                    Button(onClick = {
                        scope.launch {
                            store.saveSetup(handedness)
                            screen = Screen.PLAY
                        }
                    }) { Text("开始") }
                }
                Screen.PLAY -> PlayPage(onEnd = { session ->
                    count = session.strokeCount
                    peakSwingSpeed = session.peakSwingSpeedRadPerSecond
                    scope.launch {
                        store.saveSession(session)
                    }
                    screen = Screen.RESULT
                })
                Screen.RESULT -> Page(
                    "本次挥拍",
                    "$count 拍\n估算最高挥速 ${String.format(Locale.US, "%.0f", estimatePeakLinearSpeedKmh(peakSwingSpeed))} km/h",
                ) {
                    Button(onClick = { screen = Screen.PLAY }) { Text("再来一局") }
                    Button(onClick = { screen = Screen.HOME }) { Text("返回首页") }
                }
            }
        }
    }
}

@Composable
private fun Page(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(subtitle)
        Spacer(Modifier.height(32.dp))
        content()
    }
}

@Composable
private fun PlayPage(onEnd: (SessionSummary) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var count by remember { mutableIntStateOf(0) }
    var peakSwingSpeed by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf(PlayStatus.LOADING) }
    var pulse by remember { mutableStateOf(false) }
    var debugText by remember { mutableStateOf("") }
    val sessionStartedAtMs = remember { System.currentTimeMillis() }
    val config = remember { RecognitionConfig() }
    val recorder = remember {
        SensorRecorder(metadata = mapOf(
            "app_version" to BuildConfig.VERSION_NAME,
            "build_type" to BuildConfig.BUILD_TYPE,
            "recognition_config" to config.toString(),
        ))
    }
    val exportCsv = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(recorder.toCsv())
            }
        }
    }

    DisposableEffect(Unit) {
        val pipeline = RecognitionPipeline(config)
        val audio = AudioEngine(context, config)
        val feedback = FeedbackCoordinator(audio, HapticEngine(context))
        val handler = Handler(Looper.getMainLooper())
        var lastSwingState = "IDLE"
        var lastSwingScore = 0f
        var lastImpactScore = 0f
        var lastImpactTimestampMs = 0L

        val sensor = SensorEngine(context) { frame ->
            if (status != PlayStatus.READY) {
                if (BuildConfig.DEBUG) recorder.recordSensor(frame, detectorState = status.name)
                return@SensorEngine
            }
            val result = pipeline.process(frame)
            if (BuildConfig.DEBUG) {
                recorder.recordSensor(
                    frame = frame,
                    detectorState = result.swing.state.name,
                    swingScore = result.swing.swingScore,
                    impactScore = result.impactScore,
                )
                if (result.swing.state.name != lastSwingState) {
                    val rowType = when {
                        result.swing.state.name == "COOLDOWN" -> "cooldown"
                        lastSwingState == "COOLDOWN" && result.swing.state.name == "ARMED" -> "rearm"
                        else -> "state_transition"
                    }
                    recorder.recordEvent(
                        rowType = rowType,
                        timestampNs = frame.timestampNs,
                        detectorState = result.swing.state.name,
                        swingScore = result.swing.swingScore,
                        impactScore = result.impactScore,
                        detail = "$lastSwingState->${result.swing.state.name}",
                    )
                }
            }
            lastSwingState = result.swing.state.name
            lastSwingScore = result.swing.swingScore
            lastImpactScore = result.impactScore
            if (BuildConfig.DEBUG && result.swing.candidate) {
                recorder.recordEvent(
                    rowType = "candidate",
                    timestampNs = frame.timestampNs,
                    detectorState = result.swing.state.name,
                    swingScore = result.swing.swingScore,
                    impactScore = result.impactScore,
                    detail = "candidate=true",
                )
                Log.d(LOG_TAG, "candidate sensorMs=${frame.timestampMs} score=${result.swing.swingScore}")
            }
            if (BuildConfig.DEBUG && result.directionRejected) {
                recorder.recordEvent(
                    rowType = "direction_reject",
                    timestampNs = frame.timestampNs,
                    detectorState = result.swing.state.name,
                    swingScore = result.swing.swingScore,
                    impactScore = result.impactScore,
                    detail = "screen_normal_rotation",
                )
            }
            result.impact?.let { event ->
                val detectedAtNs = SystemClock.elapsedRealtimeNanos()
                if (BuildConfig.DEBUG) {
                    recorder.recordEvent(
                        rowType = "impact",
                        timestampNs = detectedAtNs,
                        detectorState = result.swing.state.name,
                        swingScore = result.swing.swingScore,
                        impactScore = event.impactScore,
                        eventTimestampNs = event.timestampNs,
                        detail = "strength=${config.mapStrength(event.strength)};confirmation=${event.confirmation}",
                    )
                }
                handler.postDelayed({
                    val dispatchNs = SystemClock.elapsedRealtimeNanos()
                    val accepted = status == PlayStatus.READY && feedback.dispatch(event)
                    if (BuildConfig.DEBUG) {
                        recorder.recordEvent(
                            rowType = "audio_haptic",
                            timestampNs = dispatchNs,
                            detectorState = lastSwingState,
                            impactScore = event.impactScore,
                            eventTimestampNs = event.timestampNs,
                            detail = "accepted=$accepted;play=$status;feedback=${audio.status}",
                        )
                    }
                    if (accepted) {
                        lastImpactTimestampMs = event.timestampNs / 1_000_000
                        count++
                        if (event.peakAngularSpeedRadPerSecond > peakSwingSpeed) {
                            peakSwingSpeed = event.peakAngularSpeedRadPerSecond
                        }
                        if (BuildConfig.DEBUG) {
                            recorder.recordEvent(
                                rowType = "count",
                                timestampNs = dispatchNs,
                                detectorState = lastSwingState,
                                impactScore = event.impactScore,
                                eventTimestampNs = event.timestampNs,
                                detail = "count=$count",
                            )
                        }
                        pulse = true
                        handler.postDelayed({ pulse = false }, 180)
                        if (BuildConfig.DEBUG) {
                            Log.d(
                                LOG_TAG,
                                "impact sensorMs=$lastImpactTimestampMs dispatchMs=${dispatchNs / 1_000_000} " +
                                    "score=${event.impactScore} audioHaptic=called",
                            )
                        }
                    } else if (BuildConfig.DEBUG) {
                        Log.w(LOG_TAG, "impact rejected play=$status feedback=${audio.status}")
                    }
                }, feedbackDelayMs(event.timestampNs, detectedAtNs))
            }
        }

        fun startSensor() {
            pipeline.reset()
            status = when (sensor.start()) {
                SensorStartResult.STARTED, SensorStartResult.ALREADY_RUNNING -> when (audio.status) {
                    FeedbackStatus.LOADING -> PlayStatus.LOADING
                    FeedbackStatus.READY -> PlayStatus.READY
                    FeedbackStatus.ERROR -> PlayStatus.FEEDBACK_ERROR
                }
                SensorStartResult.UNSUPPORTED -> PlayStatus.UNSUPPORTED_DEVICE
                SensorStartResult.REGISTRATION_FAILED -> PlayStatus.SENSOR_ERROR
            }
        }

        val statusPoll = object : Runnable {
            override fun run() {
                if (sensor.isRunning) {
                    status = when (audio.status) {
                        FeedbackStatus.LOADING -> PlayStatus.LOADING
                        FeedbackStatus.READY -> PlayStatus.READY
                        FeedbackStatus.ERROR -> PlayStatus.FEEDBACK_ERROR
                    }
                }
                if (BuildConfig.DEBUG) {
                    debugText = "sensor=${sensor.isRunning} frames=${sensor.frameCount} " +
                        "dt=${String.format(Locale.US, "%.1f", sensor.sampleIntervalMs)}ms\n" +
                        "audio=${audio.status} state=$lastSwingState " +
                        "swing=${String.format(Locale.US, "%.1f", lastSwingScore)} " +
                        "impact=${String.format(Locale.US, "%.1f", lastImpactScore)} " +
                        "last=$lastImpactTimestampMs"
                }
                handler.postDelayed(this, 250)
            }
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = startSensor()
            override fun onPause(owner: LifecycleOwner) {
                sensor.stop()
                pipeline.reset()
                status = PlayStatus.PAUSED
            }
        }

        lifecycle.addObserver(observer)
        startSensor()
        handler.post(statusPoll)
        onDispose {
            handler.removeCallbacksAndMessages(null)
            lifecycle.removeObserver(observer)
            sensor.stop()
            pipeline.reset()
            audio.release()
        }
    }

    val title = when (status) {
        PlayStatus.LOADING -> "LOADING"
        PlayStatus.READY -> if (pulse) "啪" else "READY"
        PlayStatus.PAUSED -> "PAUSED"
        PlayStatus.UNSUPPORTED_DEVICE -> "设备不支持"
        PlayStatus.SENSOR_ERROR -> "传感器启动失败"
        PlayStatus.FEEDBACK_ERROR -> "击球音效加载失败"
    }
    val subtitle = when (status) {
        PlayStatus.UNSUPPORTED_DEVICE -> "这台设备缺少加速度计或陀螺仪"
        PlayStatus.SENSOR_ERROR -> "无法注册运动传感器，请重新进入练习"
        PlayStatus.FEEDBACK_ERROR -> "无法预加载击球音效，请重新进入练习"
        PlayStatus.LOADING -> "$count 拍\n正在准备传感器和击球音效"
        PlayStatus.PAUSED -> "$count 拍\n返回前台后继续"
        PlayStatus.READY -> "$count 拍\n挥一下"
    }
    Page(title, subtitle) {
        if (BuildConfig.DEBUG && debugText.isNotEmpty()) {
            Text(debugText, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (recorder.isRecording) recorder.stop() else recorder.start()
            }) { Text(if (recorder.isRecording) "停止记录" else "记录本局传感器") }
            if (recorder.frameCount > 0 && !recorder.isRecording) {
                Button(onClick = { exportCsv.launch("airswing-session.csv") }) {
                    Text("导出 CSV（${recorder.frameCount} 帧 / ${recorder.rowCount} 行）")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Button(onClick = {
            onEnd(SessionSummary(sessionStartedAtMs, System.currentTimeMillis(), count, peakSwingSpeed))
        }) { Text("结束练习") }
    }
}
