package com.nssivashankar.pixelaod.data

import android.content.Context
import android.content.Intent
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
    private val iconCache = LruCache<String, ImageBitmap>(300)
    @Volatile
    private var cachedAppList: List<CachedAppInfo>? = null

    suspend fun getInstalledApps(context: Context): List<CachedAppInfo> = withContext(Dispatchers.IO) {
        cachedAppList?.let { return@withContext it }

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        // Single IPC query instead of 100+ individual getLaunchIntentForPackage Binder calls
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val seenPackages = mutableSetOf<String>()

        val apps = resolveInfos.mapNotNull { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            if (seenPackages.add(appInfo.packageName)) {
                CachedAppInfo(
                    packageName = appInfo.packageName,
                    label = resolveInfo.loadLabel(pm).toString(),
                    appInfo = appInfo
                )
            } else {
                null
            }
        }.sortedBy { it.label.lowercase() }

        cachedAppList = apps
        apps
    }

    suspend fun getIcon(context: Context, appInfo: android.content.pm.ApplicationInfo): ImageBitmap? = withContext(Dispatchers.IO) {
        iconCache.get(appInfo.packageName)?.let { return@withContext it }

        return@withContext try {
            val drawable = context.packageManager.getApplicationIcon(appInfo)
            val bitmap = drawable.toBitmap(width = 120, height = 120).asImageBitmap()
            iconCache.put(appInfo.packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun getCachedIcon(packageName: String): ImageBitmap? = iconCache.get(packageName)

    fun clearCache() {
        iconCache.evictAll()
        cachedAppList = null
    }
}

@Stable
data class CachedAppInfo(
    val packageName: String,
    val label: String,
    val appInfo: android.content.pm.ApplicationInfo
)
