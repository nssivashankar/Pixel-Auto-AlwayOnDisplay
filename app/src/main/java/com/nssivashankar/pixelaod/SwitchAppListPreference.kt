package com.nssivashankar.pixelaod

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.edit
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceViewHolder

class SwitchAppListPreference(context: Context, attrs: AttributeSet?) : AppListPreference(context, attrs) {

    private var switchKey: String? = null
    
    init {
        widgetLayoutResource = R.layout.preference_switch_widget
    }

    fun setSwitchKey(key: String) {
        this.switchKey = key
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val switch = holder.findViewById(R.id.switch_widget) as? SwitchCompat
        val key = switchKey
        if (switch != null && key != null) {
            val prefs = preferenceManager.sharedPreferences
            switch.setOnCheckedChangeListener(null)
            switch.isChecked = prefs?.getBoolean(key, false) ?: false
            
            switch.setOnCheckedChangeListener { _, isChecked ->
                prefs?.edit { putBoolean(key, isChecked) }
                // Notify listener so the service can update
                callChangeListener(isChecked)
            }
            
            // Prevent the switch click from propagating to the preference (opening the dialog)
            switch.setOnClickListener { 
                // Just consume the click
            }
        }
    }
}
