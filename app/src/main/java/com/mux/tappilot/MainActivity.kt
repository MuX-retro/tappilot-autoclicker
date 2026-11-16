package com.mux.tappilot

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("tappilot", MODE_PRIVATE)
        val pad = (24 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply { text = "TapPilot"; textSize = 32f })
        root.addView(TextView(this).apply {
            text = "1. Задайте интервал между нажатиями.\n2. Включите службу TapPilot в спец. возможностях.\n3. Перетащите прицел ◎ на нужное место и нажмите ▶.\n\nТолько для одиночных игр."
            textSize = 16f
            setPadding(0, pad / 2, 0, pad)
        })
        root.addView(TextView(this).apply { text = "Интервал, мс" })
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getLong("interval", 100L).toString())
        }
        root.addView(input)
        root.addView(Button(this).apply {
            text = "Сохранить интервал"
            setOnClickListener {
                val v = input.text.toString().toLongOrNull()?.coerceIn(10L, 600000L) ?: 100L
                prefs.edit().putLong("interval", v).apply()
                ClickService.instance?.intervalMs = v
                Toast.makeText(this@MainActivity, "Сохранено: $v мс", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(Button(this).apply {
            text = "Включить в спец. возможностях"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        setContentView(root)
    }
}
