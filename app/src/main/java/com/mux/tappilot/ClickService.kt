package com.mux.tappilot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class ClickService : AccessibilityService() {
    companion object { var instance: ClickService? = null }

    var intervalMs = 100L
    private lateinit var wm: WindowManager
    private var target: TextView? = null
    private var button: TextView? = null
    private lateinit var targetParams: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val t = target ?: return
            val loc = IntArray(2)
            t.getLocationOnScreen(loc)
            val x = loc[0] + t.width / 2f
            val y = loc[1] + t.height / 2f
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
                .build()
            dispatchGesture(gesture, null, null)
            handler.postDelayed(this, intervalMs)
        }
    }

    private fun params(x: Int, y: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; this.x = x; this.y = y }

    @SuppressLint("ClickableViewAccessibility")
    override fun onServiceConnected() {
        instance = this
        intervalMs = getSharedPreferences("tappilot", MODE_PRIVATE).getLong("interval", 100L)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        targetParams = params(400, 800)
        target = TextView(this).apply {
            text = "◎"
            textSize = 44f
            setTextColor(Color.rgb(190, 255, 60))
            var sx = 0f; var sy = 0f; var px = 0; var py = 0
            setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = targetParams.x; py = targetParams.y }
                    MotionEvent.ACTION_MOVE -> {
                        targetParams.x = px + (e.rawX - sx).toInt()
                        targetParams.y = py + (e.rawY - sy).toInt()
                        wm.updateViewLayout(this, targetParams)
                    }
                }
                true
            }
        }
        wm.addView(target, targetParams)

        button = TextView(this).apply {
            text = "▶"
            textSize = 30f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.rgb(190, 255, 60))
            setPadding(36, 12, 36, 12)
            setOnClickListener { toggle() }
        }
        wm.addView(button, params(0, 300))
    }

    private fun toggle() {
        running = !running
        button?.text = if (running) "■" else "▶"
        // While clicking, the target must let taps pass through to the game.
        targetParams.flags = if (running)
            targetParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        else
            targetParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        target?.let { wm.updateViewLayout(it, targetParams) }
        if (running) handler.post(tick) else handler.removeCallbacks(tick)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(tick)
        target?.let { wm.removeView(it) }
        button?.let { wm.removeView(it) }
        instance = null
        super.onDestroy()
    }
}
