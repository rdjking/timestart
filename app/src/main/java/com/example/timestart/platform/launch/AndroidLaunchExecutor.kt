package com.example.timestart.platform.launch

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.timestart.domain.execution.LaunchExecutor
import com.example.timestart.domain.execution.LaunchRequestResult

/** Android implementation that requests a launch through the target package's launcher intent. */
class AndroidLaunchExecutor(context: Context) : LaunchExecutor {
    private val appContext = context.applicationContext
    private val temporaryOverlay = TemporaryLaunchOverlay(appContext)

    override fun requestLaunch(packageName: String): LaunchRequestResult {
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: return LaunchRequestResult.TargetUnavailable.also {
                Log.w(TAG, "No launchable target: $packageName")
            }

        if (!OverlayPermission.isGranted(appContext)) {
            return LaunchRequestResult.OverlayPermissionRequired.also {
                Log.w(TAG, "Overlay permission is not granted")
            }
        }

        return try {
            if (!temporaryOverlay.showThenRemove()) {
                return LaunchRequestResult.Failed("Android could not attach the temporary launch overlay").also {
                    Log.e(TAG, "Temporary launch overlay could not be attached")
                }
            }
            appContext.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            LaunchRequestResult.Requested.also {
                Log.i(TAG, "Requested launch for $packageName with active overlay")
            }
        } catch (exception: ActivityNotFoundException) {
            LaunchRequestResult.Failed(exception.message ?: "Launcher activity was not found")
        } catch (exception: SecurityException) {
            LaunchRequestResult.Failed(exception.message ?: "Android denied the launch request")
        }
    }

    private companion object {
        const val TAG = "AndroidLaunchExecutor"
    }
}
