package com.nssivashankar.pixelaod

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nssivashankar.pixelaod.permissions.GrantWriteSecureSettingsUseCase
import com.nssivashankar.pixelaod.permissions.ShizukuStatus
import com.nssivashankar.pixelaod.permissions.ShizukuUtils
import com.nssivashankar.pixelaod.ui.screens.NavigationPill
import com.nssivashankar.pixelaod.ui.screens.SettingsScreen
import com.nssivashankar.pixelaod.ui.screens.SetupScreen
import com.nssivashankar.pixelaod.ui.theme.PixelAodTheme
import com.nssivashankar.pixelaod.utils.UpdateChecker
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class SettingsActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        // Force lock window to highest display refresh rate (120Hz/144Hz) to eliminate Smooth Display 60Hz drop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val maxMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
            if (maxMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = maxMode.modeId
                window.attributes = params
            }
        }

        // --- In-App Update Checker ---
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }

        lifecycleScope.launch {
            UpdateChecker.checkForUpdates(this@SettingsActivity, currentVersion) { latest, notes, url ->
                UpdateChecker.showUpdateDialog(this@SettingsActivity, latest, notes, url)
            }
        }

        setContent {
            PixelAodTheme {
                MainAppHost(
                    onPermissionRequest = { handleMissingPermission() },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onCopyAdbCommand = { copyAdbCommand() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE)
        val setupComplete = prefs.getBoolean("is_setup_complete", false)
        val skipped = prefs.getBoolean("secure_settings_skipped", false)
        if (setupComplete && !hasPermission() && !skipped) {
            handleMissingPermission()
        }
    }

    private fun hasPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    private fun handleMissingPermission() {
        when (ShizukuUtils.hasPermission()) {
            ShizukuStatus.PERM_GRANTED -> handleShizukuPermissionGranted()
            ShizukuStatus.PERM_NOT_GRANTED -> requestShizukuPermission()
            ShizukuStatus.SERVICE_STOPPED -> showWriteSecureSettingsPermissionDialog()
        }
    }

    private fun requestShizukuPermission() {
        Shizuku.requestPermission(0)
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    handleShizukuPermissionGranted()
                } else {
                    showMissingShizukuPermissionDialog()
                }
                Shizuku.removeRequestPermissionResultListener(this)
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
    }

    private fun handleShizukuPermissionGranted() {
        val grantWriteSecureSettingsUseCase = GrantWriteSecureSettingsUseCase()
        val granted = grantWriteSecureSettingsUseCase.execute(this)
        if (granted) {
            getSharedPreferences("aod_prefs", MODE_PRIVATE).edit().putBoolean("secure_settings_skipped", false).apply()
        } else {
            showWriteSecureSettingsPermissionDialog()
        }
    }

    private fun showWriteSecureSettingsPermissionDialog() {
        val msg = getString(R.string.grant_write_secure_settings, this.packageName, Manifest.permission.WRITE_SECURE_SETTINGS)

        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage(msg)
            .setNeutralButton("Copy ADB Command") { _, _ ->
                copyAdbCommand()
                showWriteSecureSettingsPermissionDialog()
            }
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun copyAdbCommand() {
        val command = "adb shell pm grant ${this.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ADB Command", command)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun showMissingShizukuPermissionDialog() {
        val appName = getString(R.string.app_name)
        val msg = getString(R.string.grant_shizuku_permission, appName)
        MaterialAlertDialogBuilder(this).setMessage(msg).setNeutralButton(android.R.string.ok, null).show()
    }
}

@Composable
private fun MainAppHost(
    onPermissionRequest: () -> Unit,
    onRequestNotifications: () -> Unit,
    onCopyAdbCommand: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aod_prefs", Context.MODE_PRIVATE) }

    var isSetupComplete by remember { mutableStateOf(prefs.getBoolean("is_setup_complete", false)) }
    var masterSwitchEnabled by remember { mutableStateOf(prefs.getBoolean("master_switch", false)) }
    var currentTab by remember { mutableIntStateOf(0) }

    if (!isSetupComplete) {
        SetupScreen(
            onComplete = {
                prefs.edit().putBoolean("is_setup_complete", true).apply()
                isSetupComplete = true
            },
            onGrantSecureSettings = onPermissionRequest,
            onCopyAdbCommand = onCopyAdbCommand,
            onRequestNotifications = onRequestNotifications
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Settings List Screen
            SettingsScreen(
                masterSwitchEnabled = masterSwitchEnabled,
                onPermissionRequest = onPermissionRequest,
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                contentPadding = PaddingValues(
                    top = 76.dp, // Compact top spacing flush with top glass header
                    bottom = 96.dp  // Clear breathing room for bottom pill
                ),
                onMasterSwitchChange = { isChecked ->
                    masterSwitchEnabled = isChecked
                    prefs.edit().putBoolean("master_switch", isChecked).apply()
                }
            )

            // High-Performance Top Glass Header Cap
            TopGlassHeader(
                masterSwitchEnabled = masterSwitchEnabled,
                onMasterSwitchChange = { isChecked ->
                    masterSwitchEnabled = isChecked
                    prefs.edit().putBoolean("master_switch", isChecked).apply()
                }
            )

            // High-Performance Floating Navigation Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 6.dp,
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .width(200.dp)
                        .height(52.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                ) {
                    NavigationPill(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopGlassHeader(
    masterSwitchEnabled: Boolean,
    onMasterSwitchChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pixel AOD",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = masterSwitchEnabled,
                    onCheckedChange = onMasterSwitchChange
                )
            }
        }
    }
}
