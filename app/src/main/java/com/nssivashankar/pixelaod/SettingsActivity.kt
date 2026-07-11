package com.nssivashankar.pixelaod

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.nssivashankar.pixelaod.permissions.GrantWriteSecureSettingsUseCase
import com.nssivashankar.pixelaod.permissions.ShizukuStatus
import com.nssivashankar.pixelaod.permissions.ShizukuUtils
import rikka.shizuku.Shizuku
import java.util.Locale
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

        val scroll = findViewById<androidx.core.widget.NestedScrollView>(R.id.settings_container_scroll)
        val mirror = findViewById<View>(R.id.header_blur_mirror)
        val container = findViewById<View>(R.id.settings_container)
        val tint = findViewById<View>(R.id.header_glass_tint)

        // --- THE STABLE MIRROR ENGINE (Zero-Glow / High-Performance) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mirror.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            val surfaceColor = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface, android.graphics.Color.BLACK
            )

            // 1. Native stability with 80f radius for better edge containment
            mirror.setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(80f, 80f, android.graphics.Shader.TileMode.CLAMP)
            )

            mirror.background = object : android.graphics.drawable.Drawable() {
                private var isDrawing = false

                override fun draw(canvas: android.graphics.Canvas) {
                    if (isDrawing || container.width <= 0) return
                    isDrawing = true
                    
                    // 2. Solid base for the frost
                    canvas.drawColor(surfaceColor)
                    
                    val density = resources.displayMetrics.density
                    
                    // 3. DUAL-EDGE GLOW SUPPRESSION
                    val topBuffer = (32 * density).toInt()
                    val bottomBuffer = (32 * density).toInt()
                    
                    canvas.save()
                    canvas.clipRect(0, topBuffer, mirror.width, mirror.height - bottomBuffer)
                    
                    canvas.translate(0f, -scroll.scrollY.toFloat())
                    container.draw(canvas)
                    canvas.restore()
                    
                    isDrawing = false
                }
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                override fun getOpacity(): Int = android.graphics.PixelFormat.OPAQUE
            }

            scroll.viewTreeObserver.addOnScrollChangedListener {
                mirror.invalidate()
            }
            
            mirror.viewTreeObserver.addOnPreDrawListener {
                mirror.invalidate()
                true
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val headerTotalHeight = (56 * density).toInt() + systemBars.top
            
            toolbar.setPadding(0, systemBars.top, 0, 0)
            val params = toolbar.layoutParams
            params.height = headerTotalHeight
            toolbar.layoutParams = params

            // Fine-tune the tint for extra "milky" depth
            tint.alpha = 0.4f 

            container.setPadding(0, headerTotalHeight + (12 * density).toInt(), 0, (48 * density).toInt())
            insets
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    override fun onStart() {
        super.onStart()
        if (!hasPermission()) {
            handleMissingPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        val masterSwitch = findViewById<MaterialSwitch>(R.id.master_switch)
        val prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE)
        masterSwitch.isChecked = prefs.getBoolean("master_switch", false)
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
        val msg = getString(
            R.string.grant_write_secure_settings,
            this.packageName,
            Manifest.permission.WRITE_SECURE_SETTINGS,
        )
        MaterialAlertDialogBuilder(this)
            .setMessage(msg)
            .setNeutralButton(android.R.string.ok, null)
            .show()
    }

    private fun showMissingShizukuPermissionDialog() {
        val appName = getString(R.string.app_name)
        val msg = getString(R.string.grant_shizuku_permission, appName)
        MaterialAlertDialogBuilder(this)
            .setMessage(msg)
            .setNeutralButton(android.R.string.ok, null)
            .show()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "aod_prefs"
            val screen = preferenceManager.createPreferenceScreen(requireContext())
            preferenceScreen = screen

            // --- Section 1: Automation & Triggers ---
            val automationCategory = PreferenceCategory(requireContext()).apply {
                title = "Automation & Triggers"
                key = "cat_automation"
            }
            screen.addPreference(automationCategory)

            automationCategory.addPreference(
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "charging_mode"
                    setTitle("Charging Mode")
                    setSummary("Turn on AoD automatically when charger is connected")
                    icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_lock_idle_low_battery)
                },
            )

            automationCategory.addPreference(
                AppListPreference(requireContext(), null).apply {
                    key = "watched_apps"
                    title = "Per-App Notifications"
                    summary = "Always trigger AoD for these apps"
                    dialogTitle = "Select apps to watch"
                    icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_agenda)
                },
            )

            // --- Section 2: Battery Health (Pixel Specific) ---
            val batteryCategory = PreferenceCategory(requireContext()).apply {
                title = getString(R.string.battery_health_title)
                key = "cat_battery"
            }
            screen.addPreference(batteryCategory)

            batteryCategory.addPreference(
                ListPreference(requireContext()).apply {
                    key = "charge_optimization"
                    title = getString(R.string.charge_optimization_title)
                    entries = arrayOf("Off", "Limit to 80%", "Adaptive Charging")
                    entryValues = arrayOf("0", "1", "2")
                    icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_lock_power_off)
                    
                    val currentMode = AodSettings.getChargeOptimizationMode(requireContext().contentResolver)
                    value = currentMode.toString()
                    summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

                    setOnPreferenceChangeListener { _, newValue ->
                        val mode = (newValue as String).toInt()
                        AodSettings.setChargeOptimizationMode(requireContext().contentResolver, mode)
                        if (mode == 0) {
                            AodSettings.setAdaptiveChargingEnabled(requireContext().contentResolver, false)
                            findPreference<SwitchPreferenceCompat>("adaptive_charging_legacy")?.isChecked = false
                        }
                        true
                    }
                }
            )

            // --- Section 3: UI & Appearance ---
            val uiCategory = PreferenceCategory(requireContext()).apply {
                title = "UI & Appearance"
                key = "cat_ui"
            }
            screen.addPreference(uiCategory)

            uiCategory.addPreference(
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "charging_info_notif"
                    setTitle(R.string.charging_info_title)
                    setSummary(R.string.charging_info_summary)
                    icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_info_details)
                },
            )

            // --- Section 4: Restrictions ---
            val quietHoursCategory = PreferenceCategory(requireContext()).apply {
                title = "Restrictions"
            }
            screen.addPreference(quietHoursCategory)

            quietHoursCategory.addPreference(
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "dnd_mode"
                    setTitle(R.string.dnd_mode_title)
                    setSummary(R.string.dnd_mode_summary)
                },
            )

            quietHoursCategory.addPreference(
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "scheduled_dnd"
                    setTitle(R.string.scheduled_dnd_title)
                    setSummary(R.string.scheduled_dnd_summary)
                }
            )

            val startPref = createTimePreference("scheduled_dnd_start", R.string.start_time_title, "22:00")
            quietHoursCategory.addPreference(startPref)
            startPref.dependency = "scheduled_dnd"

            val endPref = createTimePreference("scheduled_dnd_end", R.string.end_time_title, "07:00")
            quietHoursCategory.addPreference(endPref)
            endPref.dependency = "scheduled_dnd"

            // --- Section 5: Service Status ---
            val permissionsCategory = PreferenceCategory(requireContext()).apply {
                key = "permissions_category"
                title = "Service Status"
            }
            screen.addPreference(permissionsCategory)

            val writeSecureSettingsPref = Preference(requireContext()).apply {
                key = "write_secure_pref"
                title = "Write Secure Settings"
                setOnPreferenceClickListener {
                    (activity as? SettingsActivity)?.handleMissingPermission()
                    true
                }
            }
            permissionsCategory.addPreference(writeSecureSettingsPref)

            val notificationAccessPref = Preference(requireContext()).apply {
                key = "notify_access_pref"
                title = "Notification Access"
                setOnPreferenceClickListener {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    true
                }
            }
            permissionsCategory.addPreference(notificationAccessPref)

            val postNotifPref = Preference(requireContext()).apply {
                key = "post_notif_pref"
                title = "Notification Permission"
                setOnPreferenceClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                    }
                    true
                }
            }
            permissionsCategory.addPreference(postNotifPref)

            updatePermissionSummaries()
        }

        override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        private fun updatePermissionSummaries() {
            val writeSecure = findPreference<Preference>("write_secure_pref")
            val notify = findPreference<Preference>("notify_access_pref")
            val postNotif = findPreference<Preference>("post_notif_pref")

            if (writeSecure != null) {
                val hasWriteSecure = requireContext().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
                writeSecure.summary = if (hasWriteSecure) "Granted" else "Missing - Tap to grant via Shizuku"
            }

            if (notify != null) {
                val enabledListeners = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
                val hasNotifyAccess = enabledListeners?.contains(requireContext().packageName) == true
                notify.summary = if (hasNotifyAccess) "Granted" else "Missing - Required for app detection"
            }

            if (postNotif != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPostNotif = requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    postNotif.summary = if (hasPostNotif) "Granted" else "Missing - Required for charging info"
                    postNotif.isVisible = true
                } else {
                    postNotif.isVisible = false
                }
            }
        }

        override fun onResume() {
            super.onResume()
            updatePermissionSummaries()
        }

        private fun createTimePreference(key: String, titleRes: Int, default: String): Preference {
            val prefs = requireContext().getSharedPreferences("aod_prefs", MODE_PRIVATE)
            return Preference(requireContext()).apply {
                this.key = key
                title = getString(titleRes)
                summary = prefs.getString(key, default)
                
                setOnPreferenceClickListener {
                    val currentTime = prefs.getString(key, default) ?: default
                    val parts = currentTime.split(":")
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()

                    TimePickerDialog(
                        requireContext(),
                        { _, h, m ->
                            val newTime = String.format(Locale.US, "%02d:%02d", h, m)
                            prefs.edit { putString(key, newTime) }
                            summary = newTime
                        },
                        hour,
                        minute,
                        true,
                    ).show()
                    true
                }
            }
        }
    }
}