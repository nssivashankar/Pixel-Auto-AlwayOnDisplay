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
        
        // --- DOLBY WINDOW BLUR (Android 12+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(150)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val masterSwitch = findViewById<MaterialSwitch>(R.id.master_switch)
        val prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE)
        masterSwitch.isChecked = prefs.getBoolean("master_switch", false)
        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("master_switch", isChecked) }
            AodSettings.setAodEnabled(contentResolver, isChecked)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            
            val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            val glassBg = findViewById<View>(R.id.header_glass_bg)
            val contentBg = findViewById<View>(R.id.content_background)
            val container = findViewById<View>(R.id.settings_container)

            // 1. Position Content inside the Glass zone
            // We pad the toolbar so the background (glassBg) flows behind the status bar
            toolbar.setPadding(0, systemBars.top, 0, 0)
            
            val toolbarHeight = (56 * density).toInt()
            val headerTotalHeight = toolbarHeight + systemBars.top
            
            // 2. Apply Extra Frosted Blur (Android 12+)
            // This creates the "Dolby" textured glass look
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                glassBg.setRenderEffect(
                    android.graphics.RenderEffect.createBlurEffect(30f, 30f, android.graphics.Shader.TileMode.CLAMP)
                )
            }

            // 3. The list background starts exactly where the glass header ends
            val lp = contentBg.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            lp.topMargin = headerTotalHeight
            contentBg.layoutParams = lp

            // 4. Spacing so the first item is visible below the glass
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
            }
            screen.addPreference(automationCategory)

            automationCategory.addPreference(
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "charging_mode"
                    setTitle("Charging Mode")
                    setSummary("Turn on AoD automatically when charger is connected")
                },
            )

            automationCategory.addPreference(
                AppListPreference(requireContext(), null).apply {
                    key = "watched_apps"
                    title = "Per-App Notifications"
                    summary = "Always trigger AoD for these apps"
                    dialogTitle = "Select apps to watch"
                },
            )

            automationCategory.addPreference(
                SwitchAppListPreference(requireContext(), null).apply {
                    key = "live_notif_blocked_apps"
                    setTitle("Live Notification Mode \u203A")
                    setSummary("AoD for Maps, Uber etc. \u2022 Tap to manage block list")
                    setSwitchKey("live_notif_mode")
                    dialogTitle = "Block list for live notifications"
                },
            )

            // --- Section 2: Battery Health (Pixel Specific) ---
            val batteryCategory = PreferenceCategory(requireContext()).apply {
                title = getString(R.string.battery_health_title)
            }
            screen.addPreference(batteryCategory)

            batteryCategory.addPreference(
                ListPreference(requireContext()).apply {
                    key = "charge_optimization"
                    title = getString(R.string.charge_optimization_title)
                    entries = arrayOf("Off", "Limit to 80%", "Adaptive Charging")
                    entryValues = arrayOf("0", "1", "2")
                    
                    val currentMode = AodSettings.getChargeOptimizationMode(requireContext().contentResolver)
                    value = currentMode.toString()
                    
                    // Use SummaryProvider to avoid IllegalFormatConversionException with '%'
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

            batteryCategory.addPreference(
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "adaptive_charging_legacy"
                    title = getString(R.string.adaptive_charging_title)
                    summary = getString(R.string.adaptive_charging_summary)
                    isChecked = AodSettings.isAdaptiveChargingEnabled(requireContext().contentResolver)

                    setOnPreferenceChangeListener { _, newValue ->
                        AodSettings.setAdaptiveChargingEnabled(requireContext().contentResolver, newValue as Boolean)
                        true
                    }
                }
            )

            // --- Section 3: UI & Appearance ---
            val uiCategory = PreferenceCategory(requireContext()).apply {
                title = "UI & Appearance"
            }
            screen.addPreference(uiCategory)

            uiCategory.addPreference(
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "charging_info_notif"
                    setTitle(R.string.charging_info_title)
                    setSummary(R.string.charging_info_summary)
                    setOnPreferenceChangeListener { _, newValue ->
                        if (newValue == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                            }
                        }
                        true
                    }
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