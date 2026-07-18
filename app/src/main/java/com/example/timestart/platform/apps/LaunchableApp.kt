package com.example.timestart.platform.apps

import android.graphics.drawable.Drawable

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
)

interface LaunchableAppsProvider {
    fun getApps(): List<LaunchableApp>
}
