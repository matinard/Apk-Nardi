package com.nardiobd.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nardiobd.app.model.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LuxuryClusterGauge(
    rpm: Int,
    speedKmh: Int,
    speedMph: Int,
    gearAdvice: GearAdvice,
    mode: DriveMode,
    isDark: Boolean,
    scale: Float = 1.0f
) {
    val textColor = if (isDark) Color.White else Color(0xFF111111)
    val ringBg = if (isDark) Color(0xFF1C222D) else Color(0xFFD1D5DB)
    val gaugeSize = (225 * scale).dp

    val shiftColor by animateColorAsState(
        targetValue = when (gearAdvice.action) {
            ShiftAction.REDLINE_SPORT -> Color(0xFFFF2D55)
            ShiftAction.UPSHIFT -> if (mode == DriveMode.ECO) Color(0xFF32D74B) else Color(0xFFFF9F0A)
            ShiftAction.DOWNSHIFT -> Color(0xFF64D2FF)
            ShiftAction.NONE -> mode.primaryColor
        },
        label = "shiftColor"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(gaugeSize)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx() * scale
                drawArc(
                    color = ringBg,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                val rpmRatio = (rpm / 5000f).coerceIn(0f, 1f)
                val sweep = rpmRatio * 270f
                val brush = Brush.sweepGradient(
                    colors = listOf(mode.primaryColor, shiftColor, Color(0xFFFF3B30))
                )
                drawArc(
                    brush = brush,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$rpm RPM",
                    color = shiftColor,
                    fontSize = (13 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$speedKmh",
                    color = textColor,
                    fontSize = (46 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "km/h · $speedMph mph",
                    color = textColor.copy(alpha = 0.65f),
                    fontSize = (11 * scale).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(shiftColor.copy(alpha = 0.2f))
                        .border(1.dp, shiftColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    val gearText = if (gearAdvice.currentGear == 0) "N" else "${gearAdvice.currentGear}ª"
                    Text(
                        text = gearText,
                        color = textColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (15 * scale).sp
                    )
                    if (gearAdvice.action != ShiftAction.NONE) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val arrow = if (gearAdvice.action == ShiftAction.DOWNSHIFT) "▼ ${gearAdvice.recommendedGear}ª" else "▲ ${gearAdvice.recommendedGear}ª"
                        Text(
                            text = arrow,
                            color = shiftColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (14 * scale).sp
                        )
                    }
                }
            }
        }

        if (gearAdvice.action != ShiftAction.NONE) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(shiftColor)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = gearAdvice.message,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun DynamicConsumptionCard(
    state: VehicleState,
    gearAdvice: GearAdvice,
    mode: DriveMode,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF12161F) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val isStopped = state.speedKmh <= 2

    val ecoColor = when {
        isStopped -> Color(0xFFFF9F0A)
        state.consumptionL100km <= 5.8f -> Color(0xFF32D74B)
        state.consumptionL100km <= 7.5f -> Color(0xFF0A84FF)
        else -> Color(0xFFFF375F)
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, mode.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isStopped) Icons.Default.LocalGasStation else Icons.Default.Speed,
                contentDescription = null,
                tint = ecoColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isStopped) "RALENTÍ (DETENIDO)" else "CONSUMO · MODO ${mode.title}",
                color = ecoColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (isStopped) {
            Text(
                text = String.format("%.2f L/h", state.consumptionLh),
                color = textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format("Coste espera: %.2f €/h", state.costPerHourEur),
                color = textColor.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        } else {
            Text(
                text = String.format("%.1f L/100km", state.consumptionL100km),
                color = textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format("Rendimiento: %.1f km/L", state.consumptionKmL),
                color = ecoColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = gearAdvice.message,
            color = if (gearAdvice.action != ShiftAction.NONE) ecoColor else textColor.copy(alpha = 0.65f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TpmsCarWidget(tires: TirePressures, isDark: Boolean, accentColor: Color) {
    val cardBg = if (isDark) Color(0xFF12161F) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    fun pressureColor(bar: Float): Color = if (bar < 2.0f || bar > 2.9f) Color(0xFFFF3B30) else Color(0xFF32D74B)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.height(95.dp)) {
            TireBadge("DEL IZQ", tires.frontLeft, pressureColor(tires.frontLeft), textColor)
            TireBadge("TRA IZQ", tires.rearLeft, pressureColor(tires.rearLeft), textColor)
        }

        Canvas(modifier = Modifier.size(54.dp, 95.dp)) {
            drawRoundRect(
                color = accentColor.copy(alpha = 0.25f),
                topLeft = Offset(10.dp.toPx(), 6.dp.toPx()),
                size = Size(size.width - 20.dp.toPx(), size.height - 12.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(accentColor, Offset(4.dp.toPx(), 22.dp.toPx()), Offset(size.width - 4.dp.toPx(), 22.dp.toPx()), 2.dp.toPx())
            drawLine(accentColor, Offset(4.dp.toPx(), size.height - 22.dp.toPx()), Offset(size.width - 4.dp.toPx(), size.height - 22.dp.toPx()), 2.dp.toPx())
        }

        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.height(95.dp)) {
            TireBadge("DEL DER", tires.frontRight, pressureColor(tires.frontRight), textColor)
            TireBadge("TRA DER", tires.rearRight, pressureColor(tires.rearRight), textColor)
        }
    }
}

@Composable
private fun TireBadge(label: String, bar: Float, statusColor: Color, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = textColor.copy(alpha = 0.6f))
        Text(text = String.format("%.1f bar", bar), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = statusColor)
    }
}

@Composable
fun InclinometerWidget(pitch: Float, roll: Float, isDark: Boolean) {
    val cardBg = if (isDark) Color(0xFF12161F) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Text(text = "INCLINACIÓN TERRENO", fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.size(72.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2 - 4.dp.toPx()
            drawCircle(Color.DarkGray.copy(alpha = 0.4f), r, c, style = Stroke(2.dp.toPx()))
            val rad = Math.toRadians(roll.toDouble())
            val dy = (pitch * 1.2f).coerceIn(-20f, 20f)
            drawLine(
                color = Color(0xFF0A84FF),
                start = Offset((c.x - r * cos(rad)).toFloat(), (c.y + r * sin(rad) + dy).toFloat()),
                end = Offset((c.x + r * cos(rad)).toFloat(), (c.y - r * sin(rad) + dy).toFloat()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Text(
            text = String.format("Pend: %+.1f° | Lat: %+.1f°", pitch, roll),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun WazeHudBanner(waze: WazeAlert, isDark: Boolean) {
    val bg = if (waze.hazardAlert != null) Color(0xFF3A1219) else if (isDark) Color(0xFF162032) else Color(0xFFE5F1FF)
    val textColor = if (isDark || waze.hazardAlert != null) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, if (waze.hazardAlert != null) Color(0xFFFF3B30) else Color(0xFF0A84FF), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Navigation, contentDescription = "Waze", tint = Color(0xFF33CCFF), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = waze.directionText, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (waze.hazardAlert != null) {
                    Text(text = waze.hazardAlert, color = Color(0xFFFF453A), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, CircleShape)
                .border(3.dp, Color.Red, CircleShape)
        ) {
            Text(text = "${waze.speedLimit}", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

@Composable
fun MediaAndPhoneBar(
    media: MediaState,
    isDark: Boolean,
    onNextTrack: () -> Unit,
    onOpenDialer: () -> Unit,
    onAnswerCall: () -> Unit,
    onEndCall: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF12161F) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.MusicNote, contentDescription = "Spotify", tint = Color(0xFF1DB954), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = media.trackTitle, color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(text = media.artistName, color = textColor.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
            }
            IconButton(onClick = onNextTrack) {
                Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = textColor)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onOpenDialer,
                modifier = Modifier.background(Color(0xFF0A84FF).copy(alpha = 0.2f), CircleShape).size(38.dp)
            ) {
                Icon(Icons.Default.Dialpad, contentDescription = "Marcar", tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onAnswerCall,
                modifier = Modifier.background(Color(0xFF32D74B).copy(alpha = 0.2f), CircleShape).size(38.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = "Atender", tint = Color(0xFF32D74B), modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onEndCall,
                modifier = Modifier.background(Color(0xFFFF3B30).copy(alpha = 0.2f), CircleShape).size(38.dp)
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "Colgar", tint = Color(0xFFFF3B30), modifier = Modifier.size(18.dp))
            }
        }
    }
}
