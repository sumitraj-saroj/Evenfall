package com.evenfall.app.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

class EvenfallHaptics(
    private val context: Context? = null,
    private val view: View? = null,
    private val isHapticsEnabledByUser: Boolean = true
) {
    val isSystemHapticsEnabled: Boolean
        get() = try {
            val resolver = context?.contentResolver
            if (resolver != null) {
                Settings.System.getInt(
                    resolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    1
                ) != 0
            } else {
                true
            }
        } catch (_: Exception) {
            true
        }

    fun canVibrate(): Boolean = isHapticsEnabledByUser && isSystemHapticsEnabled

    fun playPause() {
        if (!canVibrate()) return
        perform(HapticFeedbackConstants.KEYBOARD_TAP, 15L)
    }

    fun bookmarkSaved() {
        if (!canVibrate()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            perform(HapticFeedbackConstants.CONFIRM, 30L)
        } else {
            perform(HapticFeedbackConstants.LONG_PRESS, 30L)
        }
    }

    fun chapterSnapped() {
        if (!canVibrate()) return
        perform(HapticFeedbackConstants.CLOCK_TICK, 10L)
    }

    fun sleepTimerSet() {
        if (!canVibrate()) return
        perform(HapticFeedbackConstants.KEYBOARD_TAP, 20L)
    }

    fun longPressPopup() {
        if (!canVibrate()) return
        perform(HapticFeedbackConstants.LONG_PRESS, 40L)
    }

    private fun perform(feedbackConstant: Int, fallbackDurationMs: Long) {
        val performed = view?.performHapticFeedback(feedbackConstant) ?: false
        if (!performed) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val effectId = when (feedbackConstant) {
                            HapticFeedbackConstants.CLOCK_TICK -> VibrationEffect.EFFECT_TICK
                            HapticFeedbackConstants.KEYBOARD_TAP -> VibrationEffect.EFFECT_CLICK
                            HapticFeedbackConstants.LONG_PRESS -> VibrationEffect.EFFECT_HEAVY_CLICK
                            else -> VibrationEffect.EFFECT_CLICK
                        }
                        vibrator.vibrate(VibrationEffect.createPredefined(effectId))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(fallbackDurationMs)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun rememberEvenfallHaptics(isHapticsEnabledByUser: Boolean = true): EvenfallHaptics {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view, isHapticsEnabledByUser) {
        EvenfallHaptics(context, view, isHapticsEnabledByUser)
    }
}
