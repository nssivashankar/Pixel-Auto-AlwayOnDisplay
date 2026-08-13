package com.nssivashankar.pixelaod.workers

import android.content.Context
import android.content.pm.PackageManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nssivashankar.pixelaod.utils.UpdateChecker

class UpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val currentVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        UpdateChecker.checkForUpdates(context, currentVersion) { latest, _, url ->
            UpdateChecker.showUpdateNotification(context, latest, url)
        }

        return Result.success()
    }
}
