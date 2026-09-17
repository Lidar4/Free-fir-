package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var hasOverlayPermissionState = mutableStateOf(false)
    private var hasNotificationPermissionState = mutableStateOf(false)
    private var isServiceRunningState = mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateStates()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    "FF Companion & Trainer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    val hasOverlayPermission by hasOverlayPermissionState
                    val hasNotificationPermission by hasNotificationPermissionState
                    val isServiceRunning by isServiceRunningState

                    val notificationLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        hasNotificationPermissionState.value = isGranted
                        if (isGranted) {
                            startOverlayService()
                        } else {
                            Toast.makeText(
                                this,
                                "Notification permission is recommended to show the running overlay status.",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Still start service, under some devices it runs even if notification is blocked
                            startOverlayService()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Banner Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Companion Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Legitimate Training Helper",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your manual companion to track drills, timer duration, hits, and headshots. This app strictly contains NO cheats, hacks, or automation.",
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // State & Permission Panel (Overlay Draw Over Other Apps)
                        AnimatedVisibility(
                            visible = !hasOverlayPermission,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            PermissionRequiredCard(
                                onGrantClicked = {
                                    requestOverlayPermission()
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = hasOverlayPermission,
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            ControlPanelCard(
                                isServiceRunning = isServiceRunning,
                                onToggleService = {
                                    if (isServiceRunning) {
                                        stopOverlayService()
                                    } else {
                                        // Request notification permission if Android 13+ and not granted yet
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val currentPerm = ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                            if (currentPerm != PackageManager.PERMISSION_GRANTED) {
                                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                startOverlayService()
                                            }
                                        } else {
                                            startOverlayService()
                                        }
                                    }
                                }
                            )
                        }

                        // Educational Instructions Section
                        EducationalGuideSection()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStates()
    }

    private fun updateStates() {
        hasOverlayPermissionState.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
        hasNotificationPermissionState.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        isServiceRunningState.value = isServiceRunning(this, OverlayService::class.java)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                startActivity(intent)
                Toast.makeText(this, "Please grant 'Display over other apps' permission.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open Settings. Please grant permission manually.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isServiceRunningState.value = true
        Toast.makeText(this, "Companion Overlay Started!", Toast.LENGTH_SHORT).show()
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
        isServiceRunningState.value = false
        Toast.makeText(this, "Companion Overlay Stopped!", Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        if (manager != null) {
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        }
        return false
    }
}

@Composable
fun PermissionRequiredCard(onGrantClicked: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Permission Alert",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Permission Required",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = "This application requires the 'Display over other apps' (SYSTEM_ALERT_WINDOW) permission to draw the training companion helper overlay over the game.",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            Button(
                onClick = onGrantClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(imageVector = Icons.Default.Security, contentDescription = "Grant")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Overlay Permission")
            }
        }
    }
}

@Composable
fun ControlPanelCard(
    isServiceRunning: Boolean,
    onToggleService: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isServiceRunning) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                    contentDescription = "Status",
                    tint = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isServiceRunning) "Companion Overlay is Active" else "Companion Overlay is Inactive",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isServiceRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (isServiceRunning) "A floating bubble is now visible on your screen. Tap it to expand/collapse the interactive training menu. You can drag it anywhere." 
                       else "Tap the button below to launch the companion overlay over your games.",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = if (isServiceRunning) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Button(
                onClick = onToggleService,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Toggle"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isServiceRunning) "Stop Companion Overlay" else "Start Companion Overlay")
            }
        }
    }
}

@Composable
fun EducationalGuideSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Companion Features Guide",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        GuideItem(
            icon = Icons.Default.Timer,
            title = "Interactive Session Timer",
            description = "Start/Pause/Reset controls to keep track of your practice sessions and drill durations."
        )

        GuideItem(
            icon = Icons.Default.SportsEsports,
            title = "Manual Practice Stats Tracker",
            description = "Increment and decrement your target hits and headshots manually as you perform practicing drills."
        )

        GuideItem(
            icon = Icons.Default.TaskAlt,
            title = "Structured Practice Drills",
            description = "Complete and mark off your daily practice checklist like Recoil Control, Drag Shots, and Gloo Wall speeds."
        )

        GuideItem(
            icon = Icons.Default.Palette,
            title = "Customizable Overlay Styling",
            description = "Swap between thematic colors and use the opacity slider to blend the widget seamlessly into your gameplay view."
        )
    }
}

@Composable
fun GuideItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
