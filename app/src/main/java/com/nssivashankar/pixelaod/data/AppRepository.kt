package com.nssivashankar.pixelaod.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance repository for app info and icon caching.
 * Prevents UI jank by centralizing heavy package manager operations.
 */
object AppRepository {
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private var cachedAppList: List<CachedAppInfo>? = null

    suspend fun getInstalledApps(context: Context): List<CachedAppInfo> = withContext(Dispatchers.IO) {
        cachedAppList?.let { return@withContext it }

        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { appInfo ->
                CachedAppInfo(
                    packageName = appInfo.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    appInfo = appInfo
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
        
        cachedAppList = apps
        apps
    }

    suspend fun getIcon(context: Context, appInfo: android.content.pm.ApplicationInfo): ImageBitmap? = withContext(Dispatchers.IO) {
        iconCache[appInfo.packageName]?.let { return@withContext it }

        return@withContext try {
            val drawable = context.packageManager.getApplicationIcon(appInfo)
            // Smaller bitmap size for faster rendering and less memory
            val bitmap = drawable.toBitmap(width = 100, height = 100).asImageBitmap()
            iconCache[appInfo.packageName] = bitmap
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun getCachedIcon(packageName: String): ImageBitmap? = iconCache[packageName]
}

data class CachedAppInfo(
    val packageName: String,
    val label: String,
    val appInfo: android.content.pm.ApplicationInfo
)
