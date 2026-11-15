package com.example.intentvictim

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
  private lateinit var statusText: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    statusText = findViewById(R.id.status_text)
    findViewById<Button>(R.id.send_vulnerable_button).setOnClickListener {
      sendReport(Intent(ACTION_VIEW_REPORT).putExtra(EXTRA_REPORT, REPORT_PAYLOAD))
    }
    findViewById<Button>(R.id.send_hardened_button).setOnClickListener {
      sendReport(
        Intent(ACTION_VIEW_REPORT)
          .putExtra(EXTRA_REPORT, REPORT_PAYLOAD)
          .setClass(this, ReportActivity::class.java)
      )
    }
  }

  private fun sendReport(intent: Intent) {
    try {
      startActivity(intent)
      statusText.setText(R.string.report_sent)
    } catch (_: ActivityNotFoundException) {
      statusText.setText(R.string.no_handler)
    }
  }

  companion object {
    const val ACTION_VIEW_REPORT = "com.example.intentvictim.action.VIEW_REPORT"
    const val EXTRA_REPORT = "com.example.intentvictim.extra.REPORT"
    const val REPORT_PAYLOAD = "ANDSEC-DEMO-REPORT-001"
  }
}
