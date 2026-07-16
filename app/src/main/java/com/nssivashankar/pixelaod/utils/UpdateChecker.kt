package com.nssivashankar.pixelaod.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nssivashankar.pixelaod.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/nssivashankar/Pixel-Auto-AlwayOnDisplay/releases/latest"
    private const val PREFS_NAME = "update_prefs"
    private const val KEY_LAST_CHECK = "last_check_time"
    private const val UPDATE_CHANNEL_ID = "app_updates"
    private const val UPDATE_NOTIFICATION_ID = 5001

    suspend fun checkForUpdates(
        context: Context,
        currentVersion: String,
        isManual: Boolean = false,
        onUpdateAvailable: (String, String) -> Unit
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        val currentTime = System.currentTimeMillis()

        // Only check once every 24 hours to save data/battery, unless manual check
        if (!isManual && currentTime - lastCheck < 24 * 60 * 60 * 1000) return

        withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestVersion = json.getString("tag_name").replace("v", "")
                    val downloadUrl = json.getString("html_url")

                    if (isNewerVersion(currentVersion, latestVersion)) {
                        prefs.edit().putLong(KEY_LAST_CHECK, currentTime).apply()
                        withContext(Dispatchers.Main) {
                            onUpdateAvailable(latestVersion, downloadUrl)
                        }
                    } else if (isManual) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "App is up to date ($currentVersion)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (isManual) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Failed to check for updates", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isManual) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Error checking updates: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        // Sanitize: Remove 'v' and any non-numeric suffixes (e.g., "-debug")
        val cleanCurrent = current.lowercase().replace("v", "").split("-")[0]
        val cleanLatest = latest.lowercase().replace("v", "").split("-")[0]

        val currParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val lateParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
        
        for (i in 0 until maxOf(currParts.size, lateParts.size)) {
            val currVal = currParts.getOrElse(i) { 0 }
            val lateVal = lateParts.getOrElse(i) { 0 }
            
            if (lateVal > currVal) return true
            if (lateVal < currVal) return false
        }
        return false
    }

    fun showUpdateDialog(context: Context, latestVersion: String, downloadUrl: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Update Available")
            .setMessage("A new version ($latestVersion) of Pixel AOD is available. Would you like to update now?")
            .setPositiveButton("Update Now") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                context.startActivity(intent)
            }
            .setNegativeButton("Remind Later", null)
            .show()
    }

    fun showUpdateNotification(context: Context, latestVersion: String, downloadUrl: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        val updateIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
        val pendingIntent = PendingIntent.getActivity(
            context, 0, updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bolt_24)
            .setContentTitle("Update Available")
            .setContentText("A new version ($latestVersion) is available. Tap to update.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)

        nm.notify(UPDATE_NOTIFICATION_ID, builder.build())
    }
}
