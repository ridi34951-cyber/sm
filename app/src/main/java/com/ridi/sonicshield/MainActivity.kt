package com.ridi.sonicshield

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

private enum class SoundMode(val label: String, val subtitle: String) {
    ANC("Software Reduce", "Filter digital dari mic HP, keluaran tetap lewat TWS."),
    TRANSPARENCY("Transparency", "Dengarkan suara sekitar melalui mic HP."),
    OFF("Off", "Pemantauan dan pemrosesan mic dihentikan.")
}

class MainActivity : ComponentActivity() {
    private var engine: LiveAudioEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SonicShieldScreen(this, ::startAudio, ::stopAudio) }
    }

    private fun startAudio(mode: SoundMode, onStatus: (String) -> Unit) {
        stopAudio()
        engine = LiveAudioEngine(this, mode) { status -> runOnUiThread { onStatus(status) } }.also { it.start() }
    }

    private fun stopAudio() {
        engine?.stop()
        engine = null
    }

    override fun onDestroy() {
        stopAudio()
        super.onDestroy()
    }
}

@Composable
private fun SonicShieldScreen(
    activity: ComponentActivity,
    startAudio: (SoundMode, (String) -> Unit) -> Unit,
    stopAudio: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(SoundMode.OFF) }
    var deviceStatus by remember { mutableStateOf("Belum diperiksa") }
    var deviceName by remember { mutableStateOf("Belum terdeteksi") }
    var micStatus by remember { mutableStateOf("Belum diuji") }
    var processingStatus by remember { mutableStateOf("Tidak aktif") }
    var permissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val micOk = grants[Manifest.permission.RECORD_AUDIO] == true || ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val btOk = Build.VERSION.SDK_INT < 31 || grants[Manifest.permission.BLUETOOTH_CONNECT] == true || ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        permissionGranted = micOk && btOk
        if (permissionGranted) {
            val result = inspectBluetooth(activity)
            deviceStatus = result.first
            deviceName = result.second
            micStatus = "Mic HP siap. Mic TWS tidak dipakai agar Bluetooth tidak masuk mode panggilan."
        } else {
            deviceStatus = if (!btOk) "Izin Bluetooth tidak diberikan" else "Bluetooth siap"
            micStatus = if (!micOk) "Izin mikrofon belum diberikan" else "Mic siap"
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF82C4FF), secondary = Color(0xFF7DE0BD), background = Color(0xFF080D16), surface = Color(0xFF121C2B))) {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF14243B), Color(0xFF080D16), Color(0xFF080D16))))
        ) {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, null, tint = Color(0xFF82C4FF), modifier = Modifier.size(27.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("SONICSHIELD", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
                Text("POWERED BY RIDI  •  SMART AUDIO", color = Color(0xFF91A5BF), fontSize = 10.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(22.dp))

                val isRunning = selectedMode != SoundMode.OFF && processingStatus.startsWith("Aktif")
                val orbScale by animateFloatAsState(if (isRunning) 1.04f else 1f, label = "orb")
                val orbColor by animateColorAsState(if (isRunning) Color(0xFF2D8BCE) else Color(0xFF20334C), label = "orbColor")
                Box(Modifier.size(142.dp).scale(orbScale).border(1.dp, Color(0xFF527BA7), CircleShape).background(orbColor.copy(alpha = 0.24f), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(112.dp).border(1.dp, Color(0x667FC4FF), CircleShape).background(Color(0x332C78B8), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Headphones, null, tint = Color(0xFFA9D7FF), modifier = Modifier.size(53.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(if (isRunning) "AUDIO ENGINE BERJALAN" else "AUDIO ENGINE SIAGA", color = if (isRunning) Color(0xFF7DE0BD) else Color(0xFF9EB4CE), fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))

                Text("Pilih mode", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Pilih mode audio. Output musik tetap melalui Bluetooth normal.", color = Color(0xFF9BAEC4), fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(13.dp))

                SoundMode.values().forEach { mode ->
                    val active = selectedMode == mode
                    val accent = when (mode) { SoundMode.ANC -> Color(0xFF7DBBFF); SoundMode.TRANSPARENCY -> Color(0xFF7DE0BD); SoundMode.OFF -> Color(0xFFB7C3D3) }
                    Card(
                        onClick = {
                            selectedMode = mode
                            if (mode == SoundMode.OFF) {
                                stopAudio()
                                processingStatus = "Tidak aktif"
                            } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && (Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)) {
                                startAudio(mode) { processingStatus = it }
                                processingStatus = "Menyiapkan mic HP dan output A2DP..."
                            } else {
                                selectedMode = SoundMode.OFF
                                permissionLauncher.launch(buildList {
                                    add(Manifest.permission.RECORD_AUDIO)
                                    if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
                                }.toTypedArray())
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(if (active) 1.5.dp else 0.7.dp, if (active) accent else Color(0x334F6580), RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(containerColor = if (active) accent.copy(alpha = 0.16f) else Color(0x99121D2C)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(when (mode) { SoundMode.ANC -> Icons.Default.GraphicEq; SoundMode.TRANSPARENCY -> Icons.Default.Mic; SoundMode.OFF -> Icons.Default.PowerSettingsNew }, null, tint = accent, modifier = Modifier.size(25.dp))
                            Spacer(Modifier.width(13.dp))
                            Column(Modifier.weight(1f)) {
                                Text(mode.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(mode.subtitle, color = Color(0xFFA2B2C5), fontSize = 10.sp, lineHeight = 14.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(25.dp).border(1.5.dp, if (active) accent else Color(0xFF66758A), CircleShape).background(if (active) accent.copy(alpha = 0.22f) else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                                if (active) Icon(Icons.Default.CheckCircle, null, tint = accent, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(13.dp))
                Button(
                    onClick = {
                        val missing = buildList {
                            if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.BLUETOOTH_CONNECT)
                            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECORD_AUDIO)
                        }
                        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
                        else {
                            permissionGranted = true
                            val result = inspectBluetooth(activity)
                            deviceStatus = result.first
                            deviceName = result.second
                            micStatus = "Input mic HP; output A2DP diperiksa saat mode dijalankan."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF245F96)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Bluetooth, null)
                    Spacer(Modifier.width(8.dp))
                    Text("PERIKSA KONEKSI TWS", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xAA111B29)), shape = RoundedCornerShape(17.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("STATUS SISTEM", color = Color(0xFF8DBEFF), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.4.sp)
                        InfoLine("Bluetooth", deviceStatus)
                        InfoLine("Perangkat", deviceName)
                        InfoLine("Mikrofon", micStatus)
                        InfoLine("Pemrosesan", processingStatus)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("CATATAN: Software Reduce memakai filter digital Android pada mic HP lalu memutar hasilnya ke TWS. Ini bukan ANC hardware dan tidak menggantikan pelindung pendengaran.", color = Color(0xFFFFD28A), fontSize = 10.sp, lineHeight = 15.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text("SonicShield 3.0.14 • Powered by Ridi", color = Color(0xFF6F8198), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = Color(0xFF91A2B7), fontSize = 11.sp)
        Spacer(Modifier.width(10.dp))
        Text(value, color = Color(0xFFE4ECF6), fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

private class LiveAudioEngine(
    private val context: Context,
    private val mode: SoundMode,
    private val onStatus: (String) -> Unit
) {
    private val running = AtomicBoolean(false)
    @Volatile private var worker: Thread? = null
    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var player: AudioTrack? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private val sampleRate = 48000

    fun start() {
        if (!running.compareAndSet(false, true)) return
        worker = Thread({ processLoop() }, "SonicShield-Audio").apply { start() }
    }

    private fun processLoop() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val oldMode = audioManager.mode
        try {
            // Never enter MODE_IN_COMMUNICATION and never request SCO/HFP.
            // This keeps the A9 Pro on its normal media/A2DP path.
            audioManager.mode = AudioManager.MODE_NORMAL

            val a2dpOutput = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
                ?: throw IllegalStateException("Output Bluetooth A2DP tidak ditemukan. Hubungkan A9 Pro dari pengaturan Bluetooth terlebih dahulu.")

            val minIn = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val minOut = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minIn <= 0 || minOut <= 0) throw IllegalStateException("Format audio 48 kHz tidak didukung perangkat.")
            val bufferBytes = maxOf(minIn, minOut, 1920) * 2
            val inputFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()
            val outputFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            // Use the phone microphone. TWS microphone access normally requires HFP/SCO,
            // which caused the A9 Pro to show a call state and switch audio profiles.
            recorder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(inputFormat)
                .setBufferSizeInBytes(bufferBytes)
                .build()

            player = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(outputFormat)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferBytes)
                .build()
            player?.preferredDevice = a2dpOutput

            val session = recorder?.audioSessionId ?: 0
            if (mode == SoundMode.ANC && NoiseSuppressor.isAvailable() && session > 0) {
                noiseSuppressor = NoiseSuppressor.create(session)?.apply { enabled = true }
            }
            if (AcousticEchoCanceler.isAvailable() && session > 0) {
                echoCanceler = AcousticEchoCanceler.create(session)?.apply { enabled = true }
            }

            recorder?.startRecording()
            player?.play()
            Thread.sleep(300)

            val actualInput = recorder?.routedDevice
            val actualOutput = player?.routedDevice
            if (actualOutput == null || actualOutput.type != AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                throw IllegalStateException("Android belum mengarahkan audio ke A2DP A9 Pro. Audio dihentikan agar tidak keluar dari speaker HP.")
            }
            val inputName = actualInput?.productName?.toString() ?: "Mikrofon HP"
            val filterInfo = if (mode == SoundMode.ANC) {
                if (noiseSuppressor != null) "Noise Suppressor Android aktif + filter low-rumble" else "Filter digital dasar aktif; Noise Suppressor tidak tersedia"
            } else "monitor mic HP aktif"
            onStatus("Aktif • input $inputName • output A2DP • $filterInfo")

            val buffer = ShortArray(maxOf(960, bufferBytes / 4))
            var previousInput = 0f
            var previousOutput = 0f
            while (running.get()) {
                val read = recorder?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING) ?: -1
                if (read > 0) {
                    if (mode == SoundMode.ANC) {
                        // Gentle DC/low-frequency rumble reduction. The Android NS effect,
                        // when present, does the heavier voice-noise suppression.
                        for (i in 0 until read) {
                            val x = buffer[i].toFloat()
                            val y = x - previousInput + 0.965f * previousOutput
                            previousInput = x
                            previousOutput = y
                            buffer[i] = (y * 0.88f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                    }
                    // Conservative monitoring level to reduce feedback and sudden loudness.
                    for (i in 0 until read) {
                        buffer[i] = (buffer[i].toInt() * 0.58f).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    player?.write(buffer, 0, read, AudioTrack.WRITE_BLOCKING)
                } else if (read < 0) {
                    throw IllegalStateException("Pembacaan mic HP gagal: $read")
                }
            }
        } catch (e: SecurityException) {
            if (running.get()) onStatus("Izin mikrofon/Bluetooth diperlukan.")
        } catch (e: Exception) {
            if (running.get()) onStatus("Audio berhenti: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            releaseAudio()
            // Restore the prior mode but do not start or stop SCO ourselves.
            audioManager.mode = oldMode
            running.set(false)
        }
    }

    private fun releaseAudio() {
        try { recorder?.stop() } catch (_: Exception) { }
        try { player?.pause(); player?.flush(); player?.stop() } catch (_: Exception) { }
        try { noiseSuppressor?.release() } catch (_: Exception) { }
        try { echoCanceler?.release() } catch (_: Exception) { }
        try { recorder?.release() } catch (_: Exception) { }
        try { player?.release() } catch (_: Exception) { }
        recorder = null
        player = null
        noiseSuppressor = null
        echoCanceler = null
    }

    fun stop() {
        running.set(false)
        try { recorder?.stop() } catch (_: Exception) { }
        try { player?.pause() } catch (_: Exception) { }
        worker?.join(700)
        worker = null
    }
}

private fun inspectBluetooth(context: Context): Pair<String, String> {
    return try {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter ?: return "Bluetooth tidak tersedia" to "-"
        if (!adapter.isEnabled) return "Bluetooth mati" to "-"
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val outputs = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
        val names = outputs.map { it.productName?.toString() ?: "TWS Bluetooth" }.distinct()
        val status = if (outputs.isNotEmpty()) {
            "A2DP media terdeteksi • tidak memakai mode panggilan"
        } else {
            "TWS belum muncul sebagai output A2DP"
        }
        status to names.joinToString(", ").ifBlank { "Tidak ditemukan" }
    } catch (_: SecurityException) {
        "Izin Bluetooth diperlukan" to "-"
    } catch (e: Exception) {
        "Pemeriksaan gagal: ${e.javaClass.simpleName}" to "-"
    }
}
