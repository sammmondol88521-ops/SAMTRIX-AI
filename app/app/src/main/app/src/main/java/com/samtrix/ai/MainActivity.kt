package com.samtrix.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView
import android.widget.LinearLayout

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(10, 10, 12))
            setPadding(30, 30, 30, 30)
        }

        val title = TextView(this).apply {
            text = "SAMTRIX AI"
            textSize = 32f
            setTextColor(Color.rgb(220, 177, 83))
            gravity = Gravity.CENTER
        }

        val message = TextView(this).apply {
            text = "APP STARTED SUCCESSFULLY"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 0)
        }

        screen.addView(title)
        screen.addView(message)

        setContentView(screen)
    }
}
