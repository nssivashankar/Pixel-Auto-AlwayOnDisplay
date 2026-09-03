package com.nssivashankar.pixelaod

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.preference.MultiSelectListPreference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

open class AppListPreference(context: Context, attrs: AttributeSet?) : MultiSelectListPreference(context, attrs) {

    protected val iconCache = ConcurrentHashMap<String, Drawable>()

    companion object {
        private val iconExecutor = Executors.newFixedThreadPool(4)
    }

    override fun onClick() {
        val pm = context.packageManager
        
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { appInfo ->
                AppItem(
                    packageName = appInfo.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    appInfo = appInfo
                )
            }
            .sortedWith(compareByDescending<AppItem> { values.contains(it.packageName) }
                .thenBy { it.label.lowercase() })
            .toList()

        val selectedPackages = values.toMutableSet()
        var filteredApps = allApps

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_app_list, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_view)
        val searchEdit = dialogView.findViewById<TextInputEditText>(R.id.search_edit_text)

        val adapter = AppAdapter(filteredApps, selectedPackages, pm)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        searchEdit.addTextChangedListener { text ->
            val query = text?.toString()?.lowercase() ?: ""
            filteredApps = if (query.isEmpty()) {
                allApps
            } else {
                allApps.filter { it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query) }
            }
            adapter.updateApps(filteredApps)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(dialogTitle ?: title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (callChangeListener(selectedPackages)) {
                    values = selectedPackages
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private inner class AppAdapter(
        private var apps: List<AppItem>,
        private val selected: MutableSet<String>,
        private val pm: PackageManager
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        fun updateApps(newApps: List<AppItem>) {
            apps = newApps
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.app_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = apps[position]
            holder.appName.text = item.label
            holder.checkBox.isChecked = selected.contains(item.packageName)

            val cachedIcon = iconCache[item.packageName]
            if (cachedIcon != null) {
                holder.appIcon.setImageDrawable(cachedIcon)
            } else {
                holder.appIcon.setImageResource(android.R.color.transparent)
                // Shared executor thread pool to keep UI smooth without thread creation overhead
                iconExecutor.execute {
                    try {
                        val icon = pm.getApplicationIcon(item.appInfo)
                        iconCache[item.packageName] = icon
                        holder.itemView.post {
                            if (holder.bindingAdapterPosition == position) {
                                holder.appIcon.setImageDrawable(icon)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            holder.itemView.setOnClickListener {
                if (selected.contains(item.packageName)) {
                    selected.remove(item.packageName)
                } else {
                    selected.add(item.packageName)
                }
                notifyItemChanged(position)
            }
        }

        override fun getItemCount() = apps.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val appIcon: ImageView = view.findViewById(R.id.app_icon)
            val appName: TextView = view.findViewById(R.id.app_name)
            val checkBox: MaterialCheckBox = view.findViewById(R.id.app_checkbox)
        }
    }

    protected data class AppItem(
        val packageName: String,
        val label: String,
        val appInfo: ApplicationInfo
    )
}