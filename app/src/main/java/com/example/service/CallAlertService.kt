package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.io.IOException

class CallAlertService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var originalAlarmVolume = 0
    private var isPlaying = false

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        private const val CHANNEL_ID = "no_silent_call_channel"
        private const val NOTIFICATION_ID = 2605
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_CONTACT_NUMBER = "extra_contact_number"
        const val ACTION_STOP_ALERT = "com.example.action.STOP_ALERT"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Save original volume
        audioManager?.let { am ->
            originalAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALERT) {
            stopSelf()
            return START_NOT_STICKY
        }

        val contactName = intent?.getStringExtra(EXTRA_CONTACT_NAME) ?: "جهة اتصال هامة"
        val contactNumber = intent?.getStringExtra(EXTRA_CONTACT_NUMBER) ?: "مكالمة هامة"

        createNotificationChannel()
        val notification = createNotification(contactName)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!isPlaying) {
            playLoudAlert()
            showFloatingOverlay(contactName, contactNumber)
            isPlaying = true
        }

        return START_NOT_STICKY
    }

    private fun playLoudAlert() {
        try {
            // Maximize volume
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, AudioManager.FLAG_PLAY_SOUND)
            }

            // Obtain default ringtone URI or fallback to alarm/notification sound
            var ringtoneUri: Uri? = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            if (ringtoneUri == null) {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            if (ringtoneUri == null) {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                if (ringtoneUri != null) {
                    setDataSource(this@CallAlertService, ringtoneUri)
                }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            // Persistent vibration
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 1000, 500, 1000), 0
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(longArrayOf(0, 1000, 500, 1000), 0)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showFloatingOverlay(contactName: String, contactNumber: String) {
        // Only draw overlay if permission is granted and context supports it
        if (!Settings.canDrawOverlays(this)) {
            return
        }

        if (overlayView != null) {
            removeFloatingOverlay()
        }

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            // We will dynamically create a beautiful alert view programmatically to avoid complex XML inflating issues,
            // or create a simple custom view programmatically.
            // Under Android, creating views programmatically is extremely robust and avoids missing resource ID bugs.
            val ll = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setBackgroundColor(0xFF0F172A.toInt()) // Slate-900 Dark Blue
                setPadding(48, 48, 48, 48)
                elevation = 20f
                gravity = Gravity.CENTER
                
                // Add border/corner using state list or shapes or just solid layout
                val shapeD = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFF1E293B.toInt()) // Slate-800
                    cornerRadius = 32f
                    setStroke(4, 0xFFF97316.toInt()) // Orange border
                }
                background = shapeD
            }

            // Icon indicator
            val iconText = TextView(this).apply {
                text = "🔔"
                textSize = 36f
                gravity = Gravity.CENTER
            }
            ll.addView(iconText)

            // Alert title
            val titleView = TextView(this).apply {
                text = "No Silent : تنبيه مكالمة هامة"
                textSize = 20f
                setTextColor(0xFFF97316.toInt()) // Orange
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 8)
            }
            ll.addView(titleView)

            // Name
            val nameView = TextView(this).apply {
                text = contactName
                textSize = 24f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 4)
            }
            ll.addView(nameView)

            // Number
            val numView = TextView(this).apply {
                text = contactNumber
                textSize = 16f
                setTextColor(0x99FFFFFF.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 32)
            }
            ll.addView(numView)

            // Button to Silence/Stop
            val btnDismiss = Button(this).apply {
                text = "كتم التنبيه الفوري"
                setBackgroundColor(0xFFEF4444.toInt()) // Red button
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(24, 16, 24, 16)
                
                // Rounded corner drawable for button
                val btnShape = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFFEF4444.toInt())
                    cornerRadius = 16f
                }
                background = btnShape
                
                setOnClickListener {
                    stopSelf()
                }
            }
            ll.addView(btnDismiss)

            val layoutFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlags,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                y = 100 // Slightly lowered standard position
                windowAnimations = android.R.style.Animation_Dialog
            }

            overlayView = ll
            windowManager?.addView(overlayView, params)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatingOverlay() {
        try {
            if (windowManager != null && overlayView != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
                windowManager = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "No Silent Incoming Call Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "يفعل رنين فائق القوة لجهات الاتصال المستهدفة"
                setSound(null, null) // Disable default notification sound so it doesn't conflict
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contactName: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, CallAlertService::class.java).apply {
            action = ACTION_STOP_ALERT
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("No Silent : جاري تنبيه المكالمة!")
            .setContentText("رنين مستمر ومكثف لـ $contactName")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "كتم الرنين", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPlaying = false

        // Stop media player
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Cancel vibrations
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Restore original audio stream volume
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Remove floating overlay overlay
        removeFloatingOverlay()
    }
}
