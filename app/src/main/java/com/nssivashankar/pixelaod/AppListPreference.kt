package com.nssivashankar.pixelaod

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.preference.MultiSelectListPreference
import java.util.concurrent.ConcurrentHashMap

class AppListPreference(context: Context, attrs: AttributeSet?) : MultiSelectListPreference(context, attrs) {

    private val iconCache = ConcurrentHashMap<String, Drawable>()

    override fun onClick() {
        val pm = context.packageManager
        
        // Show loading dialog or just load (usually fast enough for 100-200 apps)
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { appInfo ->
                AppItem(
                    packageName = appInfo.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    appInfo = appInfo
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()

        val checkedItems = BooleanArray(apps.size) { values.contains(apps[it].packageName) }

        val adapter = object : ArrayAdapter<AppItem>(context, R.layout.app_list_item, apps) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.app_list_item, parent, false)
                val item = apps[position]
                
                val iconView = view.findViewById<ImageView>(R.id.app_icon)
                val textView = view.findViewById<CheckedTextView>(R.id.app_name)
                
                textView.text = item.label
                textView.isChecked = checkedItems[position]

                // Load icon from cache or PM
                val cachedIcon = iconCache[item.packageName]
                if (cachedIcon != null) {
                    iconView.setImageDrawable(cachedIcon)
                } else {
                    iconView.setImageResource(android.R.color.transparent)
                    // In a real optimized app, we'd do this in background
                    // For now, let's at least cache it once loaded
                    val icon = pm.getApplicationIcon(item.appInfo)
                    iconCache[item.packageName] = icon
                    iconView.setImageDrawable(icon)
                }
                
                return view
            }
        }

        AlertDialog.Builder(context)
            .setTitle(title)
            .setAdapter(adapter, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newValues = apps.asSequence()
                    .filterIndexed { index, _ -> checkedItems[index] }
                    .map { it.packageName }
                    .toSet()
                if (callChangeListener(newValues)) {
                    values = newValues
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
            .apply {
                listView.setOnItemClickListener { _, _, position, _ ->
                    checkedItems[position] = !checkedItems[position]
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private data class AppItem(
        val packageName: String,
        val label: String,
        val appInfo: ApplicationInfo
    )
}