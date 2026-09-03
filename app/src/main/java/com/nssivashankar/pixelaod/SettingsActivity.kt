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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.nssivashankar.pixelaod.permissions.GrantWriteSecureSettingsUseCase
import com.nssivashankar.pixelaod.permissions.ShizukuStatus
import com.nssivashankar.pixelaod.permissions.ShizukuUtils
import com.nssivashankar.pixelaod.ui.screens.NavigationPill
import com.nssivashankar.pixelaod.ui.screens.SettingsScreen
import com.nssivashankar.pixelaod.ui.screens.SetupScreen
import com.nssivashankar.pixelaod.ui.theme.AppHaptics
import com.nssivashankar.pixelaod.ui.theme.PixelAodTheme
import com.nssivashankar.pixelaod.utils.UpdateChecker
import rikka.shizuku.Shizuku
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
        
        lifecycleScope.launch {
            UpdateChecker.checkForUpdates(this@SettingsActivity, currentVersion) { latest, notes, url ->
                UpdateChecker.showUpdateDialog(this@SettingsActivity, latest, notes, url)
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
                AppHaptics.performToggleOn(view)
            } else {
                AppHaptics.performToggleOff(view)
            }
            prefs.edit { putBoolean("master_switch", isChecked) }
            masterSwitchEnabled = isChecked
        }

        val composeView = findViewById<ComposeView>(R.id.settings_compose_view)
        val footerButtonsView = findViewById<ComposeView>(R.id.footer_buttons_compose_view)
        
        val mirror = findViewById<View>(R.id.header_blur_mirror)
        val footerMirror = findViewById<View>(R.id.footer_blur_mirror)
        val tint = findViewById<View>(R.id.header_glass_tint)
        val footerTint = findViewById<View>(R.id.footer_glass_tint)
        val footerContainer = findViewById<View>(R.id.glass_footer_container)
        
        val initialSetupComplete = prefs.getBoolean("is_setup_complete", false)
        var isSetupComplete by mutableStateOf(initialSetupComplete)
        var currentTab by mutableIntStateOf(0)

        // --- Bridge Compose Screen ---
        composeView.setContent {
            val density = resources.displayMetrics.density
            val insets = ViewCompat.getRootWindowInsets(window.decorView)
            val systemBars = insets?.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val systemBarsTop = systemBars?.top ?: 0
            val systemBarsBottom = systemBars?.bottom ?: 0
            
            // Floating Header height: 56dp (toolbar) + 16dp (top margin) + 16dp (bottom margin)
            val headerHeightDp = (56 + 16 + 16 + (systemBarsTop / density)).dp
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
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
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

        // --- Bottom Navigation Pill (Sharp Layer) ---
        footerButtonsView.setContent {
            PixelAodTheme {
                if (isSetupComplete) {
                    NavigationPill(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            }
        }

        // Hide master UI elements during setup
        masterSwitch.visibility = if (initialSetupComplete) View.VISIBLE else View.GONE
        toolbar.visibility = if (initialSetupComplete) View.VISIBLE else View.GONE
        mirror.visibility = if (initialSetupComplete) View.VISIBLE else View.GONE
        tint.visibility = if (initialSetupComplete) View.VISIBLE else View.GONE
        footerContainer.visibility = if (initialSetupComplete) View.VISIBLE else View.GONE

        // React to setup completion
        snapshotFlow { isSetupComplete }.let {
            lifecycleScope.launch {
                it.collect { complete ->
                    if (complete) {
                        masterSwitch.visibility = View.VISIBLE
                        toolbar.visibility = View.VISIBLE
                        mirror.visibility = View.VISIBLE
                        tint.visibility = View.VISIBLE
                        footerContainer.visibility = View.VISIBLE
                    } else {
                        footerContainer.visibility = View.GONE
                    }
                }
            }
        }

        // --- HARDWARE-ACCELERATED MIRROR (Optimized for 120Hz) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Force hardware acceleration for the mirror layers
            mirror.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            footerMirror.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            val surfaceColor = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface, android.graphics.Color.BLACK
            )

            // Extremely efficient blur radius for high-refresh-rate Pixels
            val blurEffect = android.graphics.RenderEffect.createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.CLAMP)
            mirror.setRenderEffect(blurEffect)
            footerMirror.setRenderEffect(blurEffect)

            // Header Mirror (with floating alignment)
            mirror.background = object : android.graphics.drawable.Drawable() {
                private var isDrawing = false
                private val headerLocation = IntArray(2)
                private val composeLocation = IntArray(2)

                override fun draw(canvas: android.graphics.Canvas) {
                    if (isDrawing || composeView.width <= 0) return
                    isDrawing = true
                    canvas.drawColor(surfaceColor)
                    canvas.save()
                    
                    // Map coordinate system for the floating header
                    mirror.getLocationOnScreen(headerLocation)
                    composeView.getLocationOnScreen(composeLocation)
                    
                    val dy = headerLocation[1] - composeLocation[1]
                    canvas.translate(0f, -dy.toFloat())
                    
                    composeView.draw(canvas)
                    canvas.restore()
                    isDrawing = false
                }
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                @Suppress("DEPRECATION")
                override fun getOpacity(): Int = android.graphics.PixelFormat.OPAQUE
            }

            // Footer Mirror (with perfect alignment)
            footerMirror.background = object : android.graphics.drawable.Drawable() {
                private var isDrawing = false
                private val footerLocation = IntArray(2)
                private val composeLocation = IntArray(2)

                override fun draw(canvas: android.graphics.Canvas) {
                    if (isDrawing || composeView.width <= 0) return
                    isDrawing = true
                    
                    canvas.drawColor(surfaceColor)
                    
                    canvas.save()
                    // Map the coordinate system of the compose list to the footer pill
                    footerMirror.getLocationOnScreen(footerLocation)
                    composeView.getLocationOnScreen(composeLocation)
                    
                    val dy = footerLocation[1] - composeLocation[1]
                    canvas.translate(0f, -dy.toFloat())
                    
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
                        footerMirror.invalidate()
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
            
            // --- Refined Top Cap Logic ---
            val headerCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.glass_header_container)
            
            val typedValue = android.util.TypedValue()
            var actionBarHeight = (56 * density).toInt() 
            if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
                actionBarHeight = android.util.TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
            }
            
            // 1. Top Cap (Merged with Status Bar)
            val headerParams = headerCard.layoutParams
            headerParams.height = systemBars.top + actionBarHeight
            headerCard.layoutParams = headerParams
            toolbar.setPadding(0, systemBars.top, 0, 0)
            
            // Apply custom shapes for Top Cap
            val cornerRadius = 28 * density
            headerCard.shapeAppearanceModel = headerCard.shapeAppearanceModel.toBuilder()
                .setTopLeftCornerSize(0f).setTopRightCornerSize(0f)
                .setBottomLeftCornerSize(cornerRadius).setBottomRightCornerSize(cornerRadius).build()

            val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val tintColor = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            val tintAlpha = if (isDark) 0.4f else 0.6f

            tint.setBackgroundColor(tintColor)
            tint.alpha = tintAlpha
            footerTint.setBackgroundColor(tintColor)
            footerTint.alpha = tintAlpha

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
