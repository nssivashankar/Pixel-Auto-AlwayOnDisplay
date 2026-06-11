package com.nssivashankar.pixelaod.permissions

import android.Manifest
import android.content.Context
import android.os.Process
import android.util.Log
import com.nssivashankar.pixelaod.permissions.PlatformExtensions.getIdentifier

class GrantWriteSecureSettingsUseCase(
    private val permissionManager: PlatformPermissionManager = PlatformPermissionManager(),
) {
    companion object {
        const val TAG = "GrantWriteSecureSettingsUseCase"
    }

    fun execute(context: Context): Boolean {
        return try {
            val userId = Process.myUserHandle().getIdentifier()
            permissionManager.grantRuntimePermission(
                context.packageName,
                Manifest.permission.WRITE_SECURE_SETTINGS,
                userId
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not grant WRITE_SECURE_SETTINGS: ${e.stackTraceToString()}")
            false
        }
    }
}