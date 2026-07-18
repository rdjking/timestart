package com.example.timestart.platform.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Reads only activities declared as launchers; it never requests broad package visibility. */
class AndroidLaunchableAppsProvider(
    private val context: Context,
) : LaunchableAppsProvider {
    override fun getApps(): List<LaunchableApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .map { resolveInfo ->
                LaunchableApp(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(context.packageManager).toString(),
                    icon = resolveInfo.loadIcon(context.packageManager),
                )
            }
            .filterNot { app -> app.packageName == context.packageName }
            .distinctBy(LaunchableApp::packageName)
            .sortedBy { app -> app.label.lowercase() }
            .toList()
    }
}
