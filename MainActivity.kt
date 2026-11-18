package com.nardiobd.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nardiobd.app.model.*
import com.nardiobd.app.services.NardiNotificationListener
import com.nardiobd.app.ui.*
import kotlin.math.atan2
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var pitchState by mutableFloatStateOf(0f)
    private var rollState by mutableFloatStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        setContent {
            NardiObdApp(pitch = pitchState, roll = rollState)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            pitchState = Math.toDegrees(atan2(y.toDouble(), sqrt((x * x + z * z).toDouble()))).toFloat()
            rollState = Math.toDegrees(atan2(-x.toDouble(), z.toDouble())).toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}

@Composable
fun NardiObdApp(pitch: Float, roll: Float) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isDarkMode by remember { mutableStateOf(true) }
    var currentMode by remember { mutableStateOf(DriveMode.ECO) }
    var shiftBeepEnabled by remember { mutableStateOf(true) }
    var showCustomizer by remember { mutableStateOf(false) }
    var showDialerDialog by remember { mutableStateOf(false) }
    var dialNumber by remember { mutableStateOf("") }

    var widgetConfig by remember { mutableStateOf(WidgetConfig()) }

    val wazeData by NardiNotificationListener.wazeFlow.collectAsState()
    val mediaData by NardiNotificationListener.mediaFlow.collectAsState()

    var vehicleState by remember {
        mutableStateOf(
            VehicleState(
                isConnected = true,
                rpm = 2050,
                speedKmh = 68,
                coolantTemp = 89,
                mafGramsPerSec = 14.8f,
                odometerKm = 148520L
            )
        )
    }

    val activeState = vehicleState.copy(
        pitchAngle = pitch,
        rollAngle = roll,
        waze = if (wazeData.isActive) wazeData else vehicleState.waze,
        media = if (mediaData.isPlaying) mediaData else vehicleState.media
    )

    val gearAdvice = remember(activeState.rpm, activeState.speedKmh, activeState.pitchAngle, currentMode) {
        activeState.evaluateGearAdvice(currentMode)
    }

    LaunchedEffect(gearAdvice.action, gearAdvice.recommendedGear) {
        if (shiftBeepEnabled && (gearAdvice.action == ShiftAction.REDLINE_SPORT || (currentMode == DriveMode.ECO && gearAdvice.action == ShiftAction.UPSHIFT))) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (_: Exception) {}
        }
    }

    val bgColor = if (isDarkMode) Color(0xFF05070A) else Color(0xFFF2F2F7)
    val textColor = if (isDarkMode) Color.White else Color.Black

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("NARDI OBD", color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(
                        text = "ODO: ${activeState.odometerKm} km · PLX CAN",
                        color = currentMode.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DriveMode.entries.forEach { mode ->
                        val selected = mode == currentMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) mode.primaryColor else Color.Gray.copy(alpha = 0.2f))
                                .clickable {
                                    currentMode = mode
                                    widgetConfig = when (mode) {
                                        DriveMode.RUTA -> widgetConfig.copy(showWazeHud = true, showTpms = true, gaugeScale = 1.0f)
                                        DriveMode.CIUDAD -> widgetConfig.copy(showSpotify = true, showPhoneDialer = true, gaugeScale = 0.9f)
                                        DriveMode.SPORT -> widgetConfig.copy(showTachometer = true, showInclinometer = true, gaugeScale = 1.15f)
                                        DriveMode.ECO -> widgetConfig.copy(showWazeHud = true, gaugeScale = 0.95f)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = mode.title,
                                color = if (selected) Color.White else textColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = { isDarkMode = !isDarkMode }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Tema",
                            tint = textColor
                        )
                    }
                    IconButton(onClick = { showCustomizer = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = "Skin 100x100", tint = currentMode.primaryColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (widgetConfig.showWazeHud) {
                WazeHudBanner(waze = activeState.waze, isDark = isDarkMode)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (widgetConfig.showTachometer || widgetConfig.showSpeedometer) {
                        LuxuryClusterGauge(
                            rpm = activeState.rpm,
                            speedKmh = activeState.speedKmh,
                            speedMph = activeState.speedMph,
                            gearAdvice = gearAdvice,
                            mode = currentMode,
                            isDark = isDarkMode,
                            scale = widgetConfig.gaugeScale
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DynamicConsumptionCard(state = activeState, gearAdvice = gearAdvice, mode = currentMode, isDark = isDarkMode)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (widgetConfig.showTpms) {
                                Box(modifier = Modifier.weight(1f)) {
                                    TpmsCarWidget(tires = activeState.tires, isDark = isDarkMode, accentColor = currentMode.primaryColor)
                                }
                            }
                            if (widgetConfig.showInclinometer) {
                                InclinometerWidget(pitch = activeState.pitchAngle, roll = activeState.rollAngle, isDark = isDarkMode)
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (widgetConfig.showTachometer || widgetConfig.showSpeedometer) {
                        LuxuryClusterGauge(
                            rpm = activeState.rpm,
                            speedKmh = activeState.speedKmh,
                            speedMph = activeState.speedMph,
                            gearAdvice = gearAdvice,
                            mode = currentMode,
                            isDark = isDarkMode,
                            scale = widgetConfig.gaugeScale
                        )
                    }
                    DynamicConsumptionCard(state = activeState, gearAdvice = gearAdvice, mode = currentMode, isDark = isDarkMode)
                    if (widgetConfig.showTpms) {
                        TpmsCarWidget(tires = activeState.tires, isDark = isDarkMode, accentColor = currentMode.primaryColor)
                    }
                    if (widgetConfig.showInclinometer) {
                        InclinometerWidget(pitch = activeState.pitchAngle, roll = activeState.rollAngle, isDark = isDarkMode)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (widgetConfig.showSpotify || widgetConfig.showPhoneDialer) {
                MediaAndPhoneBar(
                    media = activeState.media,
                    isDark = isDarkMode,
                    onNextTrack = {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                    },
                    onOpenDialer = { showDialerDialog = true },
                    onAnswerCall = { context.startActivity(Intent(Intent.ACTION_DIAL)) },
                    onEndCall = { context.startActivity(Intent(Intent.ACTION_DIAL)) }
                )
            }
        }
    }

    if (showCustomizer) {
        AlertDialog(
            onDismissRequest = { showCustomizer = false },
            title = { Text("Personalizar Skin Nardi OBD") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tamaño de Relojes (${(widgetConfig.gaugeScale * 100).toInt()}%)")
                    Slider(
                        value = widgetConfig.gaugeScale,
                        onValueChange = { widgetConfig = widgetConfig.copy(gaugeScale = it) },
                        valueRange = 0.7f..1.3f
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = shiftBeepEnabled, onCheckedChange = { shiftBeepEnabled = it })
                        Text("Pitido de cambio de marcha (ECO / SPORT)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = widgetConfig.showTpms, onCheckedChange = { widgetConfig = widgetConfig.copy(showTpms = it) })
                        Text("Presión Neumáticos (TPMS)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = widgetConfig.showInclinometer, onCheckedChange = { widgetConfig = widgetConfig.copy(showInclinometer = it) })
                        Text("Inclinómetro del Terreno")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = widgetConfig.showWazeHud, onCheckedChange = { widgetConfig = widgetConfig.copy(showWazeHud = it) })
                        Text("Indicaciones Waze y Radares")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = widgetConfig.showSpotify, onCheckedChange = { widgetConfig = widgetConfig.copy(showSpotify = it) })
                        Text("Barra Spotify y Teléfono")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomizer = false }) { Text("Guardar Setup") }
            }
        )
    }

    if (showDialerDialog) {
        AlertDialog(
            onDismissRequest = { showDialerDialog = false },
            title = { Text("Marcar Teléfono") },
            text = {
                OutlinedTextField(
                    value = dialNumber,
                    onValueChange = { dialNumber = it },
                    label = { Text("Número de teléfono") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dialNumber.isNotBlank()) {
                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialNumber"))
                        context.startActivity(callIntent)
                    }
                    showDialerDialog = false
                }) { Text("Llamar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialerDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
