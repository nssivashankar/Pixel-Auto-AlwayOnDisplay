package com.nssivashankar.pixelaod

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.nssivashankar.pixelaod.permissions.GrantWriteSecureSettingsUseCase
import com.nssivashankar.pixelaod.permissions.ShizukuStatus
import com.nssivashankar.pixelaod.permissions.ShizukuUtils
import com.nssivashankar.pixelaod.ui.screens.SettingsScreen
import com.nssivashankar.pixelaod.ui.theme.PixelAodTheme
import rikka.shizuku.Shizuku
import com.nssivashankar.pixelaod.config.Settings as AodSettings

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val masterSwitch = findViewById<MaterialSwitch>(R.id.master_switch)
        val prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE)
        masterSwitch.isChecked = prefs.getBoolean("master_switch", false)
        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("master_switch", isChecked) }
            AodSettings.setAodEnabled(contentResolver, isChecked)
        }

        val mirror = findViewById<View>(R.id.header_blur_mirror)
        val composeView = findViewById<ComposeView>(R.id.settings_compose_view)
        val tint = findViewById<View>(R.id.header_glass_tint)

        // --- Bridge Compose Screen ---
        composeView.setContent {
            PixelAodTheme {
                SettingsScreen(
                    onPermissionRequest = { handleMissingPermission() }
                )
            }
        }

        // --- THE STABLE MIRROR ENGINE (Verified View-Based) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mirror.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            val surfaceColor = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface, android.graphics.Color.BLACK
            )

            mirror.setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(80f, 80f, android.graphics.Shader.TileMode.CLAMP)
            )

            mirror.background = object : android.graphics.drawable.Drawable() {
                private var isDrawing = false

                override fun draw(canvas: android.graphics.Canvas) {
                    if (isDrawing || composeView.width <= 0) return
                    isDrawing = true
                    canvas.drawColor(surfaceColor)
                    
                    canvas.save()
                    // Clip only bottom to suppress glow, top is 0 to cover title
                    canvas.clipRect(0, 0, mirror.width, mirror.height - (32 * resources.displayMetrics.density).toInt())
                    
                    // Note: In Compose, we don't have a simple scrollY on the View.
                    // However, because we use a transparent Surface in SettingsScreen,
                    // we can draw the entire ComposeView and it will match the current frame perfectly.
                    composeView.draw(canvas)
                    canvas.restore()
                    
                    isDrawing = false
                }
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                override fun getOpacity(): Int = android.graphics.PixelFormat.OPAQUE
            }

            // In Compose, we invalidate based on the view drawing itself
            mirror.viewTreeObserver.addOnPreDrawListener { mirror.invalidate(); true }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val headerTotalHeight = (56 * density).toInt() + systemBars.top
            
            toolbar.setPadding(0, systemBars.top, 0, 0)
            val params = toolbar.layoutParams
            params.height = headerTotalHeight
            toolbar.layoutParams = params

            val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (isDark) {
                tint.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHigh, android.graphics.Color.BLACK))
                tint.alpha = 0.4f 
            } else {
                tint.setBackgroundColor(android.graphics.Color.WHITE)
                tint.alpha = 0.6f
            }

            // Tell the Compose list to add padding so it doesn't start under the header
            // This is handled in the Compose Screen via WindowInsets now
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasPermission()) {
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
        MaterialAlertDialogBuilder(this).setMessage(msg).setNeutralButton(android.R.string.ok, null).show()
    }

    private fun showMissingShizukuPermissionDialog() {
        val appName = getString(R.string.app_name)
        val msg = getString(R.string.grant_shizuku_permission, appName)
        MaterialAlertDialogBuilder(this).setMessage(msg).setNeutralButton(android.R.string.ok, null).show()
    }
}
