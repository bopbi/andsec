package com.example.accessibilityattacker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val statusText = findViewById<TextView>(R.id.status_text)
    findViewById<Button>(R.id.accessibility_settings_button).setOnClickListener {
      startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    findViewById<Button>(R.id.arm_click_button).setOnClickListener {
      val launchIntent = packageManager.getLaunchIntentForPackage(VICTIM_PACKAGE)
      if (launchIntent == null) {
        DemoSessionState.cancel()
        statusText.setText(R.string.victim_not_installed)
      } else {
        DemoSessionState.arm()
        statusText.setText(R.string.click_armed)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(launchIntent)
      }
    }
    findViewById<Button>(R.id.cancel_click_button).setOnClickListener {
      DemoSessionState.cancel()
      statusText.setText(R.string.click_canceled)
    }
  }

  private companion object {
    const val VICTIM_PACKAGE = "com.example.accessibilityvictim"
  }
}
