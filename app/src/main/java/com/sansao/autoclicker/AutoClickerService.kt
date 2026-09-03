package com.sansao.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class AutoClickerService : AccessibilityService() {

    companion object {
        var instance: AutoClickerService? = null
        val isRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun dispatchClick(x: Float, y: Float, durationMs: Long = 50L) {
        val clickPath = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(clickPath, 0, durationMs))
            .build()

        dispatchGesture(gesture, null, null)
    }
}
