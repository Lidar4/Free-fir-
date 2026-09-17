package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private lateinit var params: WindowManager.LayoutParams

    // Handler and variables for the Overlay Session Timer
    private val handler = Handler(Looper.getMainLooper())
    private var isTimerRunning = false
    private var timeSeconds = 0
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                timeSeconds++
                onTimerTick?.invoke(timeSeconds)
                handler.postDelayed(this, 1000)
            }
        }
    }

    private var onTimerTick: ((Int) -> Unit)? = null

    companion object {
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 8801
        private const val PREFS_NAME = "ff_overlay_prefs"
        private const val KEY_HITS = "manual_hits"
        private const val KEY_HEADSHOTS = "manual_headshots"
        private const val KEY_DRILL_1 = "drill_1"
        private const val KEY_DRILL_2 = "drill_2"
        private const val KEY_DRILL_3 = "drill_3"
        private const val KEY_DRILL_4 = "drill_4"
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        setupOverlay()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    private fun setupOverlay() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)

            setContent {
                var isExpanded by remember { mutableStateOf(false) }
                var overlayOpacity by remember { mutableFloatStateOf(0.95f) }
                var isVibrationEnabled by remember { mutableStateOf(true) }
                var themeColorIndex by remember { mutableIntStateOf(0) }

                // Manual Statistics States
                var hitsCount by remember { mutableIntStateOf(prefs.getInt(KEY_HITS, 0)) }
                var headshotsCount by remember { mutableIntStateOf(prefs.getInt(KEY_HEADSHOTS, 0)) }

                // Training Checklist States
                var drill1Checked by remember { mutableStateOf(prefs.getBoolean(KEY_DRILL_1, false)) }
                var drill2Checked by remember { mutableStateOf(prefs.getBoolean(KEY_DRILL_2, false)) }
                var drill3Checked by remember { mutableStateOf(prefs.getBoolean(KEY_DRILL_3, false)) }
                var drill4Checked by remember { mutableStateOf(prefs.getBoolean(KEY_DRILL_4, false)) }

                // Timer State
                var currentTimerValue by remember { mutableStateOf("00:00") }
                var timerRunningState by remember { mutableStateOf(isTimerRunning) }

                // Hook up the service-level timer callbacks to the Compose UI state
                LaunchedEffect(Unit) {
                    onTimerTick = { totalSecs ->
                        val minutes = totalSecs / 60
                        val seconds = totalSecs % 60
                        currentTimerValue = String.format("%02d:%02d", minutes, seconds)
                    }
                    // Sync initial UI state if timer is already active
                    val minutes = timeSeconds / 60
                    val seconds = timeSeconds % 60
                    currentTimerValue = String.format("%02d:%02d", minutes, seconds)
                    timerRunningState = isTimerRunning
                }

                val colors = listOf(
                    Color(0xFFFF5722), // FF Fire Red
                    Color(0xFF00E5FF), // Cyan Pro
                    Color(0xFFFFC107), // Gold Elite
                    Color(0xFF4CAF50)  // Cobra Green
                )
                val currentThemeColor = colors[themeColorIndex]

                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(8.dp)
                ) {
                    if (!isExpanded) {
                        // Collapsed Floating Bubble
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(currentThemeColor)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        updateOverlayPosition(dragAmount.x.toInt(), dragAmount.y.toInt())
                                    }
                                }
                                .clickable {
                                    triggerVibration(isVibrationEnabled)
                                    isExpanded = true
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Open Training Companion",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        // Expanded Companion Control Card
                        Card(
                            modifier = Modifier
                                .width(310.dp)
                                .heightIn(max = 480.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        updateOverlayPosition(dragAmount.x.toInt(), dragAmount.y.toInt())
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = overlayOpacity)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                // Header row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DragIndicator,
                                            contentDescription = "Drag",
                                            tint = currentThemeColor,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(
                                            text = "FF Training Hub",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            triggerVibration(isVibrationEnabled)
                                            isExpanded = false
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Collapse Hub",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                // TIMER SECTION
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Session Timer",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = currentTimerValue,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = currentThemeColor
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                triggerVibration(isVibrationEnabled)
                                                if (timerRunningState) {
                                                    isTimerRunning = false
                                                    handler.removeCallbacks(timerRunnable)
                                                } else {
                                                    isTimerRunning = true
                                                    handler.post(timerRunnable)
                                                }
                                                timerRunningState = isTimerRunning
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (timerRunningState) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause Timer",
                                                tint = currentThemeColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                triggerVibration(isVibrationEnabled)
                                                isTimerRunning = false
                                                handler.removeCallbacks(timerRunnable)
                                                timeSeconds = 0
                                                currentTimerValue = "00:00"
                                                timerRunningState = false
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Reset Timer",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // MANUAL STATISTICS COUNTER
                                Text(
                                    text = "Manual Training Stats",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Hits Counter Box
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("Total Hits", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = hitsCount.toString(),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (hitsCount > 0) {
                                                            hitsCount--
                                                            prefs.edit().putInt(KEY_HITS, hitsCount).apply()
                                                            triggerVibration(isVibrationEnabled)
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, "Dec Hits", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        hitsCount++
                                                        prefs.edit().putInt(KEY_HITS, hitsCount).apply()
                                                        triggerVibration(isVibrationEnabled)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, "Inc Hits", modifier = Modifier.size(16.dp), tint = currentThemeColor)
                                                }
                                            }
                                        }
                                    }

                                    // Headshot Counter Box
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("Headshots", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = headshotsCount.toString(),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFBA1A1A)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (headshotsCount > 0) {
                                                            headshotsCount--
                                                            prefs.edit().putInt(KEY_HEADSHOTS, headshotsCount).apply()
                                                            triggerVibration(isVibrationEnabled)
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, "Dec HS", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        headshotsCount++
                                                        prefs.edit().putInt(KEY_HEADSHOTS, headshotsCount).apply()
                                                        triggerVibration(isVibrationEnabled)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, "Inc HS", modifier = Modifier.size(16.dp), tint = Color(0xFFBA1A1A))
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // TRAINING CHECKLIST
                                Text(
                                    text = "Training Practice Drill Tracker",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DrillCheckItem("1. Crosshair Placement (Head Level)", drill1Checked) {
                                        drill1Checked = it
                                        prefs.edit().putBoolean(KEY_DRILL_1, it).apply()
                                        triggerVibration(isVibrationEnabled)
                                    }
                                    DrillCheckItem("2. Recoil Management Training", drill2Checked) {
                                        drill2Checked = it
                                        prefs.edit().putBoolean(KEY_DRILL_2, it).apply()
                                        triggerVibration(isVibrationEnabled)
                                    }
                                    DrillCheckItem("3. Quick Gloo Wall Cover Practice", drill3Checked) {
                                        drill3Checked = it
                                        prefs.edit().putBoolean(KEY_DRILL_3, it).apply()
                                        triggerVibration(isVibrationEnabled)
                                    }
                                    DrillCheckItem("4. Drag Shot Calibration Drill", drill4Checked) {
                                        drill4Checked = it
                                        prefs.edit().putBoolean(KEY_DRILL_4, it).apply()
                                        triggerVibration(isVibrationEnabled)
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                // VISUAL SETTINGS (Accent & Opacity)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Theme:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        colors.forEachIndexed { index, color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .clickable {
                                                        triggerVibration(isVibrationEnabled)
                                                        themeColorIndex = index
                                                    }
                                                    .padding(2.dp)
                                            ) {
                                                if (themeColorIndex == index) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.5f))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Opacity,
                                        contentDescription = null,
                                        tint = currentThemeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Opacity: ${(overlayOpacity * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Slider(
                                    value = overlayOpacity,
                                    onValueChange = { overlayOpacity = it },
                                    valueRange = 0.4f..1.0f,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // FOOTER CONTROLS (Vibration, Reset, Exit)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            isVibrationEnabled = !isVibrationEnabled
                                            triggerVibration(isVibrationEnabled)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isVibrationEnabled) Icons.Default.Vibration else Icons.Default.VolumeMute,
                                            contentDescription = "Haptic Toggle",
                                            tint = if (isVibrationEnabled) currentThemeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Full Reset Button
                                    TextButton(
                                        onClick = {
                                            triggerVibration(isVibrationEnabled)
                                            // Reset Stats
                                            hitsCount = 0
                                            headshotsCount = 0
                                            prefs.edit().putInt(KEY_HITS, 0).putInt(KEY_HEADSHOTS, 0).apply()

                                            // Reset Checklist
                                            drill1Checked = false
                                            drill2Checked = false
                                            drill3Checked = false
                                            drill4Checked = false
                                            prefs.edit()
                                                .putBoolean(KEY_DRILL_1, false)
                                                .putBoolean(KEY_DRILL_2, false)
                                                .putBoolean(KEY_DRILL_3, false)
                                                .putBoolean(KEY_DRILL_4, false)
                                                .apply()

                                            // Reset Timer
                                            isTimerRunning = false
                                            handler.removeCallbacks(timerRunnable)
                                            timeSeconds = 0
                                            currentTimerValue = "00:00"
                                            timerRunningState = false
                                        }
                                    ) {
                                        Icon(Icons.Default.RestartAlt, "Reset All", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Reset Session", fontSize = 11.sp)
                                    }

                                    // Quit Button
                                    Button(
                                        onClick = {
                                            triggerVibration(isVibrationEnabled)
                                            stopSelf()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.ExitToApp, "Exit", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Quit", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        windowManager.addView(composeView, params)
    }

    @Composable
    private fun DrillCheckItem(
        label: String,
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!isChecked) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 6.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    private fun updateOverlayPosition(dx: Int, dy: Int) {
        params.x += dx
        params.y += dy
        if (composeView != null && composeView!!.isAttachedToWindow) {
            windowManager.updateViewLayout(composeView, params)
        }
    }

    private fun triggerVibration(enabled: Boolean) {
        if (!enabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FF Training Overlay Active")
            .setContentText("The interactive training statistics panel is running.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isTimerRunning = false
        handler.removeCallbacks(timerRunnable)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        if (composeView != null) {
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {
                // Ignore if already removed or not attached
            }
            composeView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
