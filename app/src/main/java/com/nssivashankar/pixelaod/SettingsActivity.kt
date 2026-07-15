package com.nssivashankar.pixelaod

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.nssivashankar.pixelaod.permissions.GrantWriteSecureSettingsUseCase
import com.nssivashankar.pixelaod.permissions.ShizukuStatus
import com.nssivashankar.pixelaod.permissions.ShizukuUtils
import com.nssivashankar.pixelaod.ui.screens.SettingsScreen
import com.nssivashankar.pixelaod.ui.screens.SetupScreen
import com.nssivashankar.pixelaod.ui.theme.PixelAodTheme
import com.nssivashankar.pixelaod.utils.UpdateChecker
import rikka.shizuku.Shizuku
import com.nssivashankar.pixelaod.config.Settings as AodSettings
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // SetupScreen will auto-detect the change via produceState loop
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // --- In-App Update Checker ---
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }
        
        MainScope().launch {
            UpdateChecker.checkForUpdates(this@SettingsActivity, currentVersion) { latest, url ->
                UpdateChecker.showUpdateDialog(this@SettingsActivity, latest, url)
            }
        }

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val masterSwitch = findViewById<MaterialSwitch>(R.id.master_switch)
        val prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE)
        
        val initialMasterSwitch = prefs.getBoolean("master_switch", false)
        var masterSwitchEnabled by mutableStateOf(initialMasterSwitch)
        masterSwitch.isChecked = initialMasterSwitch
        
        masterSwitch.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            } else {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
            prefs.edit { putBoolean("master_switch", isChecked) }
            AodSettings.setAodEnabled(contentResolver, isChecked)
            masterSwitchEnabled = isChecked
        }

        val mirror = findViewById<View>(R.id.header_blur_mirror)
        val composeView = findViewById<ComposeView>(R.id.settings_compose_view)
        val tint = findViewById<View>(R.id.header_glass_tint)
        
        val initialSetupComplete = prefs.getBoolean("is_setup_complete", false)
        var isSetupComplete by mutableStateOf(initialSetupComplete)

        // --- Bridge Compose Screen ---
        composeView.setContent {
            val density = resources.displayMetrics.density
            val insets = ViewCompat.getRootWindowInsets(window.decorView)
            val systemBars = insets?.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val systemBarsTop = systemBars?.top ?: 0
            val systemBarsBottom = systemBars?.bottom ?: 0
            
            val headerHeightDp = (56 + (systemBarsTop / density)).dp
            val bottomPaddingDp = (systemBarsBottom / density).dp
            
            PixelAodTheme {
                if (!isSetupComplete) {
                    SetupScreen(
                        onComplete = {
                            prefs.edit { putBoolean("is_setup_complete", true) }
                            isSetupComplete = true
                        },
                        onGrantSecureSettings = { handleMissingPermission() },
                        onCopyAdbCommand = { copyAdbCommand() },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                } else {
                    SettingsScreen(
                        masterSwitchEnabled = masterSwitchEnabled,
                        onPermissionRequest = { handleMissingPermission() },
                        contentPadding = PaddingValues(
                            top = headerHeightDp,
                            bottom = bottomPaddingDp + 16.dp
                        ),
                        onMasterSwitchChange = { isChecked ->
                            masterSwitch.isChecked = isChecked
                            masterSwitchEnabled = isChecked
                        }
                    )
                }
            }
        }

        // Hide master UI elements during setup
        masterSwitch.visibility = if (initialSetupComplete) View.VISIBLE else View.GONE
        toolbar.visibility = if (initialSetupComplete) View.VISIBLE else View.GONE
        mirror.visibility = if (initialSetupComplete) View.VISIBLE else View.GONE
        tint.visibility = if (initialSetupComplete) View.VISIBLE else View.GONE

        // React to setup completion
        snapshotFlow { isSetupComplete }.let {
            MainScope().launch {
                it.collect { complete ->
                    if (complete) {
                        masterSwitch.visibility = View.VISIBLE
                        toolbar.visibility = View.VISIBLE
                        mirror.visibility = View.VISIBLE
                        tint.visibility = View.VISIBLE
                    }
                }
            }
        }

        // --- HARDWARE-ACCELERATED MIRROR (Optimized for 120Hz) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Force hardware acceleration for the mirror layer
            mirror.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            val surfaceColor = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface, android.graphics.Color.BLACK
            )

            // Extremely efficient blur radius for high-refresh-rate Pixels
            mirror.setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.CLAMP)
            )

            mirror.background = object : android.graphics.drawable.Drawable() {
                private var isDrawing = false

                override fun draw(canvas: android.graphics.Canvas) {
                    if (isDrawing || composeView.width <= 0) return
                    isDrawing = true
                    
                    canvas.drawColor(surfaceColor)
                    
                    canvas.save()
                    // NO CLIPPING or SCALING - Just direct hardware capture
                    composeView.draw(canvas)
                    canvas.restore()
                    
                    isDrawing = false
                }
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                @Suppress("DEPRECATION")
                override fun getOpacity(): Int = android.graphics.PixelFormat.OPAQUE
            }

            // CRITICAL Performance Fix: 
            // 1. We remove the Global PreDrawListener which was causing circular redraws.
            // 2. We use a throttled FrameCallback to redraw the mirror at most once per frame.
            // 3. This eliminates the "starting lag" as the CPU is 100% free when the app first appears.
            var isInvalidationPending = false
            val frameCallback = object : android.view.Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (isInvalidationPending) {
                        mirror.invalidate()
                        isInvalidationPending = false
                    }
                }
            }

            composeView.viewTreeObserver.addOnScrollChangedListener {
                if (!isInvalidationPending) {
                    isInvalidationPending = true
                    android.view.Choreographer.getInstance().postFrameCallback(frameCallback)
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            
            toolbar.setPadding(0, systemBars.top, 0, 0)
            val params = toolbar.layoutParams
            params.height = (56 * density).toInt() + systemBars.top
            toolbar.layoutParams = params

            val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (isDark) {
                tint.setBackgroundColor(android.graphics.Color.BLACK)
                tint.alpha = 0.4f 
            } else {
                tint.setBackgroundColor(android.graphics.Color.WHITE)
                tint.alpha = 0.6f
            }

            insets
        }
    }

    override fun onStart() {
        super.onStart()
        val prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE)
        val setupComplete = prefs.getBoolean("is_setup_complete", false)
        if (setupComplete && !hasPermission()) {
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
        if (!granted) {
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
                // Re-show the dialog so they can still see the instructions after copying
                showWriteSecureSettingsPermissionDialog()
            }
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun copyAdbCommand() {
        val command = "adb shell pm grant ${this.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("ADB Command", command)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(this, "Command copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showMissingShizukuPermissionDialog() {
        val appName = getString(R.string.app_name)
        val msg = getString(R.string.grant_shizuku_permission, appName)
        MaterialAlertDialogBuilder(this).setMessage(msg).setNeutralButton(android.R.string.ok, null).show()
    }
}
