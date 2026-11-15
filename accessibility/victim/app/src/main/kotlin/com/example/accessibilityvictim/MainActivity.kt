package com.example.accessibilityvictim

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
  private var counter = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val statusText = findViewById<TextView>(R.id.status_text)
    val demoActionButton = findViewById<Button>(R.id.demo_action_button)

    fun updateCounter() {
      statusText.text = getString(R.string.demo_counter, counter)
    }

    demoActionButton.setOnClickListener {
      counter += 1
      updateCounter()
    }
    findViewById<Button>(R.id.reset_button).setOnClickListener {
      counter = 0
      updateCounter()
    }
    findViewById<Switch>(R.id.accessibility_visibility_switch).setOnCheckedChangeListener { _, isChecked ->
      demoActionButton.importantForAccessibility = if (isChecked) {
        View.IMPORTANT_FOR_ACCESSIBILITY_YES
      } else {
        View.IMPORTANT_FOR_ACCESSIBILITY_NO
      }
    }
    updateCounter()
  }
}
