package com.nssivashankar.pixelaod.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/nssivashankar/Pixel-Auto-AlwayOnDisplay/releases/latest"
    private const val PREFS_NAME = "update_prefs"
    private const val KEY_LAST_CHECK = "last_check_time"

    suspend fun checkForUpdates(context: Context, currentVersion: String, onUpdateAvailable: (String, String) -> Unit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        val currentTime = System.currentTimeMillis()

        // Only check once every 24 hours to save data/battery
        if (currentTime - lastCheck < 24 * 60 * 60 * 1000) return

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
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val lateParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        
        for (i in 0 until minOf(currParts.size, lateParts.size)) {
            if (lateParts[i] > currParts[i]) return true
            if (lateParts[i] < currParts[i]) return false
        }
        return lateParts.size > currParts.size
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
}
