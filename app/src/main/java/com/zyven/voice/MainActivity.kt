package com.zyven.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

private val Purple = Color(0xFF7856D8)
private val Pale = Color(0xFFF5F1FF)
private val Ink = Color(0xFF252238)

class MainActivity : ComponentActivity() {
    private var engine: AudioMonitor? = null
    private val permission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startEngine() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoiceHome(
                onStart = { gain, bass ->
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                        startEngine(gain, bass)
                    } else {
                        permission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStop = { engine?.stop(); engine = null },
                onSettingsChanged = { gain, bass ->
                    engine?.gain = gain
                    engine?.bass = bass
                }
            )
        }
    }

    private fun startEngine(gain: Float = 1.5f, bass: Float = 0.35f) {
        engine?.stop()
        engine = AudioMonitor(gain, bass).also { it.start() }
    }

    override fun onDestroy() {
        engine?.stop()
        super.onDestroy()
    }
}

@Composable
private fun VoiceHome(
    onStart: (Float, Float) -> Unit,
    onStop: () -> Unit,
    onSettingsChanged: (Float, Float) -> Unit
) {
    var running by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("Deep Voice") }
    var gain by remember { mutableFloatStateOf(1.5f) }
    var bass by remember { mutableFloatStateOf(0.35f) }

    fun updateSettings(newGain: Float = gain, newBass: Float = bass) {
        gain = newGain
        bass = newBass
        onSettingsChanged(newGain, newBass)
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Purple,
            background = Color(0xFFFAF9FD),
            surface = Color.White
        )
    ) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFFFAF9FD))
                .verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Pale, shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Default.Mic, null, tint = Purple,
                        modifier = Modifier.padding(12.dp).size(27.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("ZYVEN VOICE", color = Ink, fontSize = 21.sp)
                    Text("MICROPHONE AUDIO LAB", color = Purple, fontSize = 10.sp)
                }
            }

            Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Mikrofon kontrolü", color = Ink, fontSize = 19.sp)
                    Text(
                        if (running) "Mikrofon işleme aktif" else "Mikrofon henüz başlatılmadı",
                        color = Color.Gray
                    )
                    Button(
                        onClick = {
                            if (running) { onStop(); running = false }
                            else { onStart(gain, bass); running = true }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Icon(Icons.Default.Mic, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (running) "Durdur" else "Mikrofonu başlat")
                    }
                    Text("Testi kulaklıkla yap. Hoparlör geri besleme oluşturabilir.",
                        color = Color.Gray, fontSize = 12.sp)
                }
            }

            Text("SES PROFİLİ", color = Purple, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileCard("Deep Voice", "Daha dolgun / baslı ton",
                    mode == "Deep Voice", Modifier.weight(1f)) {
                    mode = "Deep Voice"
                    updateSettings(1.7f, 0.8f)
                }
                ProfileCard("Custom Voice", "Elle ayarlanabilir",
                    mode == "Custom Voice", Modifier.weight(1f)) {
                    mode = "Custom Voice"
                    updateSettings(gain, bass)
                }
            }

            Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = Purple)
                        Spacer(Modifier.width(8.dp))
                        Text("Ses ayarları", color = Ink, fontSize = 18.sp)
                    }
                    Text("Gain  ·  ${String.format(Locale.US, "%.1f", gain)}x")
                    Slider(value = gain, onValueChange = { updateSettings(newGain = it) },
                        valueRange = 0.5f..4f, enabled = mode == "Custom Voice")
                    Text("Bass  ·  ${String.format(Locale.US, "%.0f", bass * 100)}%")
                    Slider(value = bass, onValueChange = { updateSettings(newBass = it) },
                        valueRange = 0f..1f, enabled = mode == "Custom Voice")
                    Text("Bu ayarlar uygulama içindeki canlı ses monitörüne uygulanır.",
                        color = Color.Gray, fontSize = 12.sp)
                }
            }

            Surface(color = Pale, shape = RoundedCornerShape(18.dp)) {
                Text(
                    "ÖNEMLİ  Bu sürüm sesi başka uygulamalara sanal mikrofon olarak göndermez. " +
                    "Android, normal uygulamaların diğer uygulaların mikrofon girişini değiştirmesine izin vermez.",
                    Modifier.padding(16.dp), color = Ink, fontSize = 12.sp
                )
            }
            Text("ZYVEN VOICE  •  v0.1.0", color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProfileCard(
    title: String, subtitle: String, selected: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick = onClick, modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Pale else Color.White
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Purple) else null
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Purple)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
    }
}
