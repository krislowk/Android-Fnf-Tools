package com.zen.fnftools

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra("trace") ?: "Unknown crash"

        val tv = TextView(this).apply {
            text = trace
            setTextColor(android.graphics.Color.WHITE)
            textSize = 12f
            setPadding(32, 64, 32, 64)
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            addView(tv)
        }
        setContentView(scroll)
    }
}
