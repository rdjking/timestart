package com.example.timestart.platform.launch

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Holds a tiny, visible overlay briefly while an exact alarm launches its selected app. */
class TemporaryLaunchOverlay(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showThenRemove(): Boolean {
        val overlay = View(appContext)
        val layoutParams = WindowManager.LayoutParams(
            1,
            1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            alpha = 0.01f
            title = "TimeStartLaunchOverlay"
        }

        var added = false
        val completed = CountDownLatch(1)
        val addOverlay: () -> Unit = {
            try {
                windowManager.addView(overlay, layoutParams)
                added = true
                Log.i(TAG, "Temporary launch overlay attached")
                mainHandler.postDelayed(
                    { runCatching { windowManager.removeViewImmediate(overlay) } },
                    REMOVE_AFTER_MILLIS,
                )
            } catch (_: WindowManager.BadTokenException) {
                // The caller turns this into a notification fallback.
                Log.e(TAG, "Temporary launch overlay was rejected")
            } catch (_: SecurityException) {
                // The special access was revoked between its check and this launch attempt.
                Log.e(TAG, "Temporary launch overlay permission was denied")
            } finally {
                completed.countDown()
            }
            Unit
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            addOverlay()
        } else {
            mainHandler.post(addOverlay)
            completed.await(OVERLAY_ADD_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        }
        return added
    }

    private companion object {
        const val REMOVE_AFTER_MILLIS = 1_500L
        const val OVERLAY_ADD_TIMEOUT_MILLIS = 1_000L
        const val TAG = "TemporaryLaunchOverlay"
    }
}
