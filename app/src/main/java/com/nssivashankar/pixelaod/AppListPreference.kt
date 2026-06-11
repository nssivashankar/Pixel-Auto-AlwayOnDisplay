package com.nssivashankar.pixelaod

import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.preference.MultiSelectListPreference

class AppListPreference(context: Context, attrs: AttributeSet?) : MultiSelectListPreference(context, attrs) {

    override fun onClick() {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString() }
            .toList()

        val labels = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        val packageNames = apps.map { it.packageName }.toTypedArray()
        val checkedItems = BooleanArray(packageNames.size) { values.contains(packageNames[it]) }

        val adapter = object : ArrayAdapter<String>(context, R.layout.app_list_item, labels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.app_list_item, parent, false)
                val app = apps[position]
                
                val iconView = view.findViewById<ImageView>(R.id.app_icon)
                val textView = view.findViewById<CheckedTextView>(R.id.app_name)
                
                iconView.setImageDrawable(pm.getApplicationIcon(app))
                textView.text = labels[position]
                textView.isChecked = checkedItems[position]
                
                return view
            }
        }

        AlertDialog.Builder(context)
            .setTitle(title)
            .setAdapter(adapter, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newValues = mutableSetOf<String>()
                checkedItems.forEachIndexed { index, isChecked ->
                    if (isChecked) newValues.add(packageNames[index])
                }
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
}