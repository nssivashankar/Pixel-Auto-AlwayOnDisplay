package com.nssivashankar.pixelaod

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.content.edit
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
import java.util.Calendar
import java.util.Locale

class SettingsActivity : androidx.appcompat.app.AppCompatActivity() {

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
            preferenceScreen = screen

            val automationCategory = PreferenceCategory(requireContext()).apply {
                setTitle("Automation")
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
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "live_notif_mode"
                    setTitle("Live Notification Mode")
                    setSummary("Turn on AoD for live updates (Maps, Swiggy, Uber etc.)")
                },
            )

            automationCategory.addPreference(
                SwitchPreferenceCompat(requireContext()).apply {
                    key = "dnd_mode"
                    setTitle(R.string.dnd_mode_title)
                    setSummary(R.string.dnd_mode_summary)
                },
            )

            val quietHoursCategory = PreferenceCategory(requireContext()).apply {
                setTitle(R.string.scheduled_dnd_title)
            }
            screen.addPreference(quietHoursCategory)

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

            val notificationsCategory = PreferenceCategory(requireContext()).apply {
                setTitle("Notifications")
            }
            screen.addPreference(notificationsCategory)

            notificationsCategory.addPreference(
                AppListPreference(requireContext(), null).apply {
                    key = "watched_apps"
                    setTitle("Per-App Notifications")
                    setSummary("AoD turns on when selected apps send a notification")
                },
            )

            val permissionsCategory = PreferenceCategory(requireContext()).apply {
                setTitle("Permissions")
            }
            screen.addPreference(permissionsCategory)

            permissionsCategory.addPreference(
                Preference(requireContext()).apply {
                    setTitle("Grant Notification Access")
                    setSummary("Required for per-app and live notification features")
                    setOnPreferenceClickListener {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        true
                    }
                },
            )
        }

        private fun createTimePreference(key: String, titleRes: Int, default: String): Preference {
            val prefs = requireContext().getSharedPreferences("aod_prefs", Context.MODE_PRIVATE)
            return Preference(requireContext()).apply {
                this.key = key
                setTitle(titleRes)
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
                        true
                    ).show()
                    true
                }
                
            }
        }
    }
}