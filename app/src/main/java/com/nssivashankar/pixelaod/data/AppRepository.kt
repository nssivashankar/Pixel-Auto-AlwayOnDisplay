package com.nssivashankar.pixelaod.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance repository for app info and icon caching.
 * Prevents UI jank by centralizing heavy package manager operations.
 */
object AppRepository {
    // Cache ~100 icons (72x72 is approx 20KB per bitmap -> 2MB total)
    private val iconCache = LruCache<String, ImageBitmap>(100)
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
        iconCache.get(appInfo.packageName)?.let { return@withContext it }

        return@withContext try {
            val drawable = context.packageManager.getApplicationIcon(appInfo)
            // 72x72 is the standard for 42dp list icons on high-density displays
            val bitmap = drawable.toBitmap(width = 72, height = 72).asImageBitmap()
            iconCache.put(appInfo.packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun getCachedIcon(packageName: String): ImageBitmap? = iconCache.get(packageName)
}

@Stable
data class CachedAppInfo(
    val packageName: String,
    val label: String,
    val appInfo: android.content.pm.ApplicationInfo
)
