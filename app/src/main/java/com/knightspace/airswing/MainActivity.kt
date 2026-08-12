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
    val context = androidx.compose.ui.platform.LocalContext.current; var count by remember { mutableIntStateOf(0) }; var supported by remember { mutableStateOf(true) }; var ready by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { val audio = AudioEngine(context); val haptic = HapticEngine(context); val swing = SwingDetector(RecognitionConfig()); val impact = ImpactDetector(RecognitionConfig()); val engine = SensorEngine(context) { frame -> val update = swing.process(frame); impact.process(frame, update)?.let { event -> audio.play(event); haptic.play(); count++ ; swing.beginCooldown(frame.timestampMs) } }; supported = engine.supported; engine.start(); ready = supported; onDispose { engine.stop(); audio.release() } }
    Page(if (supported && ready) "READY" else "设备不支持", if (supported) "$count\n挥一下" else "这台设备缺少必要传感器") { Button(onClick = { onEnd(count) }) { Text("结束练习") } }
}
