package com.nardiobd.app.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.nardiobd.app.model.MediaState
import com.nardiobd.app.model.WazeAlert

class NardiNotificationListener : NotificationListenerService() {

    companion object {
        private val _wazeFlow = MutableStateFlow(WazeAlert())
        val wazeFlow = _wazeFlow.asStateFlow()

        private val _mediaFlow = MutableStateFlow(MediaState())
        val mediaFlow = _mediaFlow.asStateFlow()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        when {
            pkg.contains("waze", ignoreCase = true) -> {
                val hazard = when {
                    text.contains("radar", true) || title.contains("radar", true) -> "⚠️ RADAR DETECTADO"
                    text.contains("polic", true) || title.contains("polic", true) -> "🚓 POLICÍA REPORTADA"
                    text.contains("peligro", true) || title.contains("peligro", true) -> "🚧 PELIGRO EN VÍA"
                    else -> null
                }
                _wazeFlow.value = WazeAlert(
                    isActive = true,
                    directionText = if (title.isNotEmpty()) "$title · $text" else text,
                    speedLimit = 120,
                    hazardAlert = hazard
                )
            }
            pkg.contains("spotify", ignoreCase = true) -> {
                _mediaFlow.value = MediaState(
                    isPlaying = true,
                    trackTitle = title.ifEmpty { "Spotify" },
                    artistName = text
                )
            }
        }
    }
}
