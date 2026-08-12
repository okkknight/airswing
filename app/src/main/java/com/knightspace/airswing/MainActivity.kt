package com.knightspace.airswing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.util.Log
import com.knightspace.airswing.feedback.AudioEngine
import com.knightspace.airswing.feedback.HapticEngine
import com.knightspace.airswing.recognition.*
import com.knightspace.airswing.sensor.SensorEngine
import com.knightspace.airswing.domain.SessionStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AirSwingApp() } }
}

private enum class Screen { HOME, SETUP, PLAY, RESULT }
@Composable private fun AirSwingApp() {
    val context = androidx.compose.ui.platform.LocalContext.current; val store = remember { SessionStore(context) }; var screen by remember { mutableStateOf(Screen.HOME) }; var handed by remember { mutableStateOf("右手") }; var count by remember { mutableIntStateOf(0) }
    MaterialTheme { Surface(Modifier.fillMaxSize()) { when (screen) {
        Screen.HOME -> Page("空气挥拍", "上次 ${store.lastCount} 拍 · 累计 ${store.totalCount} 拍") { Button(onClick = { screen = if (store.hasSetup) Screen.PLAY else Screen.SETUP }) { Text("开始挥拍") } }
        Screen.SETUP -> Page("像握球拍一样握住手机", "手机长轴对准球拍杆，握住下半部") { Row { listOf("右手", "左手").forEach { Button(onClick = { handed = it }, modifier = Modifier.padding(4.dp)) { Text(it) } } }; Button(onClick = { store.saveSetup(handed); screen = Screen.PLAY }) { Text("开始") } }
        Screen.PLAY -> PlayPage(onEnd = { finalCount -> count = finalCount; store.saveSession(finalCount); screen = Screen.RESULT })
        Screen.RESULT -> Page("本次挥拍", "$count 拍") { Button(onClick = { screen = Screen.PLAY }) { Text("再来一局") }; Button(onClick = { screen = Screen.HOME }) { Text("返回首页") } }
    } } }
}
@Composable private fun Page(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = { Text(title, style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(16.dp)); Text(subtitle); Spacer(Modifier.height(32.dp)); content() }) }
@Composable private fun PlayPage(onEnd: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current; val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var count by remember { mutableIntStateOf(0) }; var supported by remember { mutableStateOf(true) }; var ready by remember { mutableStateOf(false) }; var paused by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { val config = RecognitionConfig(); val audio = AudioEngine(context, config); val haptic = HapticEngine(context); val swing = SwingDetector(config); val impact = ImpactDetector(config); val engine = SensorEngine(context) { frame -> if (audio.isReady()) { val update = swing.process(frame); if (BuildConfig.DEBUG && update.candidate) Log.d("AirSwing", "candidate t=${frame.timestampMs} gyro=${update.gyroActivity}"); impact.process(frame, update)?.let { event -> Log.d("AirSwing", "impact t=${event.timestampNs / 1_000_000} score=${event.impactScore}"); audio.play(event); haptic.play(); count++; swing.beginCooldown(frame.timestampMs) } } }; val observer = object : DefaultLifecycleObserver { override fun onResume(owner: LifecycleOwner) { paused = false; if (engine.supported) engine.start() }; override fun onPause(owner: LifecycleOwner) { paused = true; engine.stop() } }; supported = engine.supported; lifecycle.addObserver(observer); engine.start(); val poll = android.os.Handler(android.os.Looper.getMainLooper()); val readyCheck = object : Runnable { override fun run() { ready = audio.isReady(); if (!ready) poll.postDelayed(this, 30) } }; poll.post(readyCheck); onDispose { poll.removeCallbacks(readyCheck); lifecycle.removeObserver(observer); engine.stop(); audio.release() } }
    val title = when { !supported -> "设备不支持"; paused -> "PAUSED"; !ready -> "LOADING"; else -> "READY" }
    Page(title, if (supported) "$count\n${if (ready) "挥一下" else "正在加载击球音效"}" else "这台设备缺少必要传感器") { Button(onClick = { onEnd(count) }) { Text("结束练习") } }
}
