package org.alberto97.aodtoggle

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.FrameLayout
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class SettingsActivity : androidx.appcompat.app.AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FrameLayout(this).apply {
            id = android.R.id.content
        }
        setContentView(container)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "aod_prefs"
            val screen = preferenceManager.createPreferenceScreen(requireContext())

            screen.addPreference(SwitchPreferenceCompat(requireContext()).apply {
                key = "charging_mode"
                title = "Charging Mode"
                summary = "Turn on AoD automatically when charger is connected"
            })

            screen.addPreference(SwitchPreferenceCompat(requireContext()).apply {
                key = "live_notif_mode"
                title = "Live Notification Mode"
                summary = "Turn on AoD for live updates (Maps, Swiggy, Uber etc.)"
            })

            val pm = requireContext().packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { true }
                .sortedBy { pm.getApplicationLabel(it).toString() }

            screen.addPreference(MultiSelectListPreference(requireContext()).apply {
                key = "watched_apps"
                title = "Per-App Notifications"
                summary = "AoD turns on when selected apps send a notification"
                entries = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
                entryValues = apps.map { it.packageName }.toTypedArray()
            })

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