package com.nssivashankar.pixelaod

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nssivashankar.pixelaod.permissions.GrantWriteSecureSettingsUseCase
import com.nssivashankar.pixelaod.permissions.ShizukuStatus
import com.nssivashankar.pixelaod.permissions.ShizukuUtils
import com.nssivashankar.pixelaod.ui.screens.SettingsScreen
import com.nssivashankar.pixelaod.ui.theme.PixelAodTheme
import rikka.shizuku.Shizuku

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        setContent {
            PixelAodTheme {
                SettingsScreen(
                    onPermissionRequest = { handleMissingPermission() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasPermission()) {
            handleMissingPermission()
        }
    }

    private fun hasPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    private fun handleMissingPermission() {
        when (ShizukuUtils.hasPermission()) {
            ShizukuStatus.PERM_GRANTED -> handleShizukuPermissionGranted()
            ShizukuStatus.PERM_NOT_GRANTED -> requestShizukuPermission()
            ShizukuStatus.SERVICE_STOPPED -> showWriteSecureSettingsPermissionDialog()
        }
    }

    private fun requestShizukuPermission() {
        Shizuku.requestPermission(0)
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    handleShizukuPermissionGranted()
                } else {
                    showMissingShizukuPermissionDialog()
                }
                Shizuku.removeRequestPermissionResultListener(this)
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
    }

    private fun handleShizukuPermissionGranted() {
        val grantWriteSecureSettingsUseCase = GrantWriteSecureSettingsUseCase()
        val granted = grantWriteSecureSettingsUseCase.execute(this)
        if (!granted) {
            showWriteSecureSettingsPermissionDialog()
        }
    }

    private fun showWriteSecureSettingsPermissionDialog() {
        val msg = getString(
            R.string.grant_write_secure_settings,
            this.packageName,
            Manifest.permission.WRITE_SECURE_SETTINGS,
        )
        MaterialAlertDialogBuilder(this)
            .setMessage(msg)
            .setNeutralButton(android.R.string.ok, null)
            .show()
    }

    private fun showMissingShizukuPermissionDialog() {
        val appName = getString(R.string.app_name)
        val msg = getString(R.string.grant_shizuku_permission, appName)
        MaterialAlertDialogBuilder(this)
            .setMessage(msg)
            .setNeutralButton(android.R.string.ok, null)
            .show()
    }
}
