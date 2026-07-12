package com.nssivashankar.pixelaod

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nssivashankar.pixelaod.TileServiceExtensions.collapseQSPanel
import com.nssivashankar.pixelaod.config.AmbientDisplayConfiguration
import com.nssivashankar.pixelaod.permissions.GrantWriteSecureSettingsUseCase
import com.nssivashankar.pixelaod.permissions.ShizukuStatus
import com.nssivashankar.pixelaod.permissions.ShizukuUtils
import rikka.shizuku.Shizuku
import com.nssivashankar.pixelaod.config.Settings as AodSettings

class AodTileService : TileService() {

    private val isAodAvailable by lazy { AmbientDisplayConfiguration().isAvailable() }
    
    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        if (key == "master_switch") {
            val isEnabled = p.getBoolean(key, false)
            setTileActive(isEnabled)
        }
    }

    override fun onStartListening() {
        super.onStartListening()

        if (!isAodAvailable) {
            setTileUnavailable()
            return
        }

        val prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        
        val masterEnabled = prefs.getBoolean("master_switch", false)
        setTileActive(masterEnabled)
    }

    override fun onStopListening() {
        super.onStopListening()
        val prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onClick() {
        super.onClick()

        if (!hasPermission()) {
            handleMissingPermission()
            return
        }

        val prefs = getSharedPreferences("aod_prefs", MODE_PRIVATE)
        val currentState = prefs.getBoolean("master_switch", false)
        val newState = !currentState

        prefs.edit().putBoolean("master_switch", newState).apply()
        setTileActive(newState)

        if (!newState) {
            AodSettings.setAodEnabled(contentResolver, false)
        }
    }

    private fun handleMissingPermission() {
        when (ShizukuUtils.hasPermission()) {
            ShizukuStatus.PERM_GRANTED -> handleShizukuPermissionGranted()
            ShizukuStatus.PERM_NOT_GRANTED -> requestShizukuPermission()
            ShizukuStatus.SERVICE_STOPPED -> showWriteSecureSettingsPermissionDialog()
        }
    }

    private fun requestShizukuPermission() {
        collapseQSPanel()
        Shizuku.requestPermission(0)
        Shizuku.addRequestPermissionResultListener { _, grantResult ->
            val hasPermission = grantResult == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                handleShizukuPermissionGranted()
            } else {
                showMissingShizukuPermissionDialog()
            }
        }
    }

    private fun handleShizukuPermissionGranted() {
        val grantWriteSecureSettingsUseCase = GrantWriteSecureSettingsUseCase()
        val granted = grantWriteSecureSettingsUseCase.execute(baseContext)
        if (granted) {
            // Permission granted, toggle now
            onClick()
        } else {
            showWriteSecureSettingsPermissionDialog()
        }
    }

    private fun showWriteSecureSettingsPermissionDialog() {
        val msg = getString(
            R.string.grant_write_secure_settings,
            this.packageName,
            Manifest.permission.WRITE_SECURE_SETTINGS,
        )
        val dialog = MaterialAlertDialogBuilder(this)
            .setMessage(msg)
            .setNeutralButton(android.R.string.ok, null)
            .create()

        showDialog(dialog)
    }

    private fun showMissingShizukuPermissionDialog() {
        val appName = getString(R.string.app_name)
        val msg = getString(R.string.grant_shizuku_permission, appName)
        val dialog = MaterialAlertDialogBuilder(this)
            .setMessage(msg)
            .setNeutralButton(android.R.string.ok, null)
            .create()

        showDialog(dialog)
    }

    private fun hasPermission(): Boolean {
        val writeSecureSettings = checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
        return writeSecureSettings == PackageManager.PERMISSION_GRANTED
    }

    private fun setTileUnavailable() {
        val tile = qsTile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            tile.subtitle = getString(R.string.unsupported_device)
        tile.state = Tile.STATE_UNAVAILABLE
        tile.updateTile()
    }

    private fun setTileActive(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}