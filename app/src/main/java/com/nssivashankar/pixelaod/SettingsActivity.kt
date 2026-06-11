package com.nssivashankar.pixelaod

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.nssivashankar.pixelaod.config.Settings as AodSettings
import com.nssivashankar.pixelaod.permissions.GrantWriteSecureSettingsUseCase
import com.nssivashankar.pixelaod.permissions.ShizukuStatus
import com.nssivashankar.pixelaod.permissions.ShizukuUtils
import rikka.shizuku.Shizuku

class SettingsActivity : androidx.appcompat.app.AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val masterSwitch = findViewById<MaterialSwitch>(R.id.master_switch)
        val prefs = getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
        masterSwitch.isChecked = prefs.getBoolean("master_switch", false)
        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("master_switch", isChecked).apply()
            AodSettings.setAodEnabled(contentResolver, isChecked)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
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

            val automationCategory = PreferenceCategory(requireContext()).apply {
                title = "Automation"
            }
            screen.addPreference(automationCategory)

            automationCategory.addPreference(
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "charging_mode"
                    title = "Charging Mode"
                    summary = "Turn on AoD automatically when charger is connected"
                },
            )

            automationCategory.addPreference(
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "live_notif_mode"
                    title = "Live Notification Mode"
                    summary = "Turn on AoD for live updates (Maps, Swiggy, Uber etc.)"
                },
            )

            val notificationsCategory = PreferenceCategory(requireContext()).apply {
                title = "Notifications"
            }
            screen.addPreference(notificationsCategory)

            notificationsCategory.addPreference(
                AppListPreference(requireContext(), null).apply {
                    key = "watched_apps"
                    title = "Per-App Notifications"
                    summary = "AoD turns on when selected apps send a notification"
                },
            )

            val permissionsCategory = PreferenceCategory(requireContext()).apply {
                title = "Permissions"
            }
            screen.addPreference(permissionsCategory)

            permissionsCategory.addPreference(
                Preference(requireContext()).apply {
                    title = "Grant Notification Access"
                    summary = "Required for per-app and live notification features"
                    setOnPreferenceClickListener {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        true
                    }
                },
            )

            preferenceScreen = screen
        }
    }
}