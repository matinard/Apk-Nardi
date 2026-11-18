package com.nardiobd.app.model

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

enum class DriveMode(
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val upshiftRpm: Int,
    val downshiftRpm: Int,
    val description: String
) {
    ECO("ECO", Color(0xFF32D74B), Color(0xFF00C7BE), 1950, 1250, "Cambia pronto (1.950 rpm) para aprovechar el par diésel y ahorrar"),
    CIUDAD("CIUDAD", Color(0xFF30D158), Color(0xFF64D2FF), 2200, 1350, "Equilibrio entre agilidad urbana y bajo consumo"),
    RUTA("RUTA", Color(0xFF0A84FF), Color(0xFF5E5CE6), 2450, 1500, "Crucero óptimo en autopista y adelantamientos seguros"),
    SPORT("SPORT", Color(0xFFFF375F), Color(0xFFFF9F0A), 3850, 2300, "Estira hasta potencia máxima (3.850-4.000 rpm) y mantiene turbo cargado")
}

enum class ShiftAction {
    NONE,
    UPSHIFT,
    DOWNSHIFT,
    REDLINE_SPORT
}

data class GearAdvice(
    val currentGear: Int,
    val recommendedGear: Int,
    val action: ShiftAction,
    val message: String,
    val isUrgent: Boolean
)

data class TirePressures(
    val frontLeft: Float = 2.4f,
    val frontRight: Float = 2.4f,
    val rearLeft: Float = 2.3f,
    val rearRight: Float = 2.3f
)

data class WazeAlert(
    val isActive: Boolean = false,
    val directionText: String = "Continúa recto 1,2 km",
    val speedLimit: Int = 120,
    val hazardAlert: String? = null
)

data class MediaState(
    val isPlaying: Boolean = false,
    val trackTitle: String = "Sin reproducción",
    val artistName: String = "Spotify listo"
)

data class WidgetConfig(
    val showTachometer: Boolean = true,
    val showSpeedometer: Boolean = true,
    val showTpms: Boolean = true,
    val showInclinometer: Boolean = true,
    val showWazeHud: Boolean = true,
    val showSpotify: Boolean = true,
    val showPhoneDialer: Boolean = true,
    val gaugeScale: Float = 1.0f
)

data class VehicleState(
    val isConnected: Boolean = false,
    val deviceName: String = "OBD i30 PLXDevices-07CC",
    val rpm: Int = 880,
    val speedKmh: Int = 0,
    val engineLoadPct: Int = 25,
    val coolantTemp: Int = 88,
    val oilTemp: Int = 91,
    val mafGramsPerSec: Float = 4.2f,
    val odometerKm: Long = 148520L,
    val tripKm: Float = 12.4f,
    val pitchAngle: Float = 1.5f,
    val rollAngle: Float = -0.4f,
    val tires: TirePressures = TirePressures(),
    val waze: WazeAlert = WazeAlert(isActive = true, directionText = "Gira a la derecha en 350 m", speedLimit = 90, hazardAlert = "Radar móvil reportado"),
    val media: MediaState = MediaState(isPlaying = true, trackTitle = "Midnight City", artistName = "M83"),
    val fuelPriceEur: Float = 1.58f
) {
    val speedMph: Int get() = (speedKmh * 0.621371).toInt()
    val consumptionLh: Float get() = (mafGramsPerSec * 3600f) / (14.5f * 832f)
    val costPerHourEur: Float get() = consumptionLh * fuelPriceEur
    val consumptionL100km: Float get() = if (speedKmh > 2) (consumptionLh / speedKmh) * 100f else 0f
    val consumptionKmL: Float get() = if (consumptionL100km > 0.1f) 100f / consumptionL100km else 0f

    fun detectCurrentGear(): Int {
        if (speedKmh < 4 || rpm < 750) return 0
        val ratio = rpm.toFloat() / speedKmh.toFloat()
        val gearRatios = listOf(
            1 to 115f,
            2 to 62f,
            3 to 41f,
            4 to 30f,
            5 to 23.5f,
            6 to 19.2f
        )
        return gearRatios.minByOrNull { abs(it.second - ratio) }?.first ?: 0
    }

    fun evaluateGearAdvice(mode: DriveMode): GearAdvice {
        val gear = detectCurrentGear()
        if (gear == 0) {
            return GearAdvice(0, 1, ShiftAction.NONE, "Punto muerto / Ralentí", false)
        }

        val slopeOffset = if (pitchAngle > 4.0f) 250 else 0
        val targetUpshift = mode.upshiftRpm + slopeOffset
        val targetDownshift = mode.downshiftRpm + (if (pitchAngle > 4.0f) 150 else 0)

        return when {
            mode == DriveMode.SPORT && rpm >= 4150 && gear < 6 -> {
                GearAdvice(gear, gear + 1, ShiftAction.REDLINE_SPORT, "¡CAMBIA A ${gear + 1}ª YA! (${rpm} rpm)", true)
            }
            rpm >= targetUpshift && gear < 6 -> {
                val label = if (mode == DriveMode.ECO) "SUBE A ${gear + 1}ª (Ahorro ECO)" else "SUBE A ${gear + 1}ª"
                GearAdvice(gear, gear + 1, ShiftAction.UPSHIFT, label, mode == DriveMode.SPORT)
            }
            rpm <= targetDownshift && gear > 1 && speedKmh > 12 -> {
                val label = if (mode == DriveMode.SPORT) "BAJA A ${gear - 1}ª (Par óptimo)" else "BAJA A ${gear - 1}ª (Motor bajo)"
                GearAdvice(gear, gear - 1, ShiftAction.DOWNSHIFT, label, false)
            }
            else -> {
                GearAdvice(gear, gear, ShiftAction.NONE, "Marcha ${gear}ª óptima (${mode.title})", false)
            }
        }
    }
}
