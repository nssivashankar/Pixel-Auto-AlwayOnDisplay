package org.alberto97.aodtoggle

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "aod_prefs"
            val screen = preferenceManager.createPreferenceScreen(requireContext())

            // Feature 1: Charging Mode
            screen.addPreference(SwitchPreferenceCompat(requireContext()).apply {
                key = "charging_mode"
                title = "Charging Mode"
                summary = "Turn on AoD automatically when charger is connected"
            })

            // Feature 3: Live Notifications
            screen.addPreference(SwitchPreferenceCompat(requireContext()).apply {
                key = "live_notif_mode"
                title = "Live Notification Mode"
                summary = "Turn on AoD for live updates (Maps, Swiggy, Uber etc.)"
            })

            // Feature 2: Per-App picker
            val pm = requireContext().packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .sortedBy { pm.getApplicationLabel(it).toString() }

            screen.addPreference(MultiSelectListPreference(requireContext()).apply {
                key = "watched_apps"
                title = "Per-App Notifications"
                summary = "AoD turns on when selected apps send a notification"
                entries = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
                entryValues = apps.map { it.packageName }.toTypedArray()
            })

            // Grant Notification Access button
            screen.addPreference(Preference(requireContext()).apply {
                title = "Grant Notification Access"
                summary = "Required for per-app and live notification features"
                setOnPreferenceClickListener {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    true
                }
            })

            preferenceScreen = screen
        }
    }
}