package com.evenfall.app.playback

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.db.dao.HistoryDao
import com.evenfall.app.data.db.entity.HistoryEntity
import com.evenfall.app.data.db.entity.HistoryEventType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothAutoResumeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var historyDao: HistoryDao

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == BluetoothDevice.ACTION_ACL_CONNECTED ||
            action == "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" ||
            action == "com.google.android.gms.car.media.STATUS"
        ) {
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch {
                val isEnabled = preferencesManager.autoResumeBluetoothEnabled.first()
                if (!isEnabled) return@launch

                // Never auto-play if another app is currently playing audio
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager?.isMusicActive == true) {
                    return@launch
                }

                // Check allowed devices if configured
                val allowedDevices = preferencesManager.autoResumeBluetoothDevices.first()
                val connectedDevice: BluetoothDevice? = try {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                } catch (_: Exception) { null }

                if (allowedDevices.isNotEmpty() && connectedDevice != null) {
                    val address = connectedDevice.address ?: ""
                    val name = try { connectedDevice.name ?: "" } catch (_: SecurityException) { "" }
                    val matches = allowedDevices.any { it.equals(address, ignoreCase = true) || it.equals(name, ignoreCase = true) }
                    if (!matches) return@launch
                }

                val currentBook = playbackManager.playbackState.value.currentBook ?: return@launch
                val currentChapter = playbackManager.playbackState.value.currentChapter

                playbackManager.play()

                val isCar = action == "com.google.android.gms.car.media.STATUS" ||
                        (connectedDevice != null && try {
                            connectedDevice.bluetoothClass?.majorDeviceClass == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO
                        } catch (_: SecurityException) { false })

                val label = if (isCar) "Resumed · Car" else "Resumed · Bluetooth"
                historyDao.insert(
                    HistoryEntity(
                        bookId = currentBook.id,
                        eventType = HistoryEventType.PLAYED,
                        timestamp = System.currentTimeMillis(),
                        positionMs = playbackManager.playbackState.value.chapterPositionMs,
                        chapterIndex = currentChapter?.chapterIndex ?: 0,
                        outputDevice = label
                    )
                )
            }
        }
    }
}
