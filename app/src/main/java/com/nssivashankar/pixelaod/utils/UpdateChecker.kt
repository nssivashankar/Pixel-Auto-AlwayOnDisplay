package com.nssivashankar.pixelaod.utils

import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nssivashankar.pixelaod.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
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
        onUpdateAvailable: (String, String, String) -> Unit
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        val currentTime = System.currentTimeMillis()

        if (!isManual && currentTime - lastCheck < 24 * 60 * 60 * 1000) return

        withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "Pixel-Auto-AOD")
                connection.useCaches = false

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestVersion = json.getString("tag_name").replace("v", "")
                    val changelog = json.getString("body")
                    
                    // Find the APK asset URL
                    var apkUrl = ""
                    val assets = json.getJSONArray("assets")
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.getString("name").endsWith(".apk")) {
                            apkUrl = asset.getString("browser_download_url")
                            break
                        }
                    }

                    if (isNewerVersion(currentVersion, latestVersion)) {
                        prefs.edit().putLong(KEY_LAST_CHECK, currentTime).apply()
                        withContext(Dispatchers.Main) {
                            onUpdateAvailable(latestVersion, changelog, apkUrl)
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
        fun clean(v: String) = v.lowercase().replace("v", "").split("-")[0].trim()
        val cleanCurrent = clean(current)
        val cleanLatest = clean(latest)
        if (cleanCurrent == cleanLatest) return false
        val currParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val lateParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currParts.size, lateParts.size)
        for (i in 0 until maxLen) {
            val currVal = currParts.getOrElse(i) { 0 }
            val lateVal = lateParts.getOrElse(i) { 0 }
            if (lateVal > currVal) return true
            if (lateVal < currVal) return false
        }
        return false
    }

    fun showUpdateDialog(context: Context, latestVersion: String, changelog: String, downloadUrl: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Update Available ($latestVersion)")
            .setMessage("What's New:\n\n$changelog")
            .setPositiveButton("Download & Install") { _, _ ->
                if (downloadUrl.isNotEmpty()) {
                    startDownload(context, latestVersion, downloadUrl)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nssivashankar/Pixel-Auto-AlwayOnDisplay/releases/latest"))
                    context.startActivity(intent)
                }
            }
            .setNegativeButton("Remind Later", null)
            .show()
    }

    private fun startDownload(context: Context, version: String, url: String) {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "PixelAOD_v$version.apk")
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading Pixel AOD v$version")
            .setDescription("Preparing update installation...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        // Register receiver to handle download completion
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    installApk(context, destination)
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        
        android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to open installer: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun showUpdateNotification(context: Context, latestVersion: String, downloadUrl: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(UPDATE_CHANNEL_ID, "App Updates", NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(channel)
        }
        val updateIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
        val pendingIntent = PendingIntent.getActivity(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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
