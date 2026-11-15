package com.example.intentattacker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class MainActivity : Activity() {
  private lateinit var statusText: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    statusText = findViewById(R.id.status_text)
    showOutcome(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    showOutcome(intent)
  }

  private fun showOutcome(intent: Intent) {
    val report = intent.getStringExtra(EXTRA_REPORT)
    when {
      intent.action == ACTION_VIEW_REPORT && report == REPORT_PAYLOAD -> {
        statusText.text = getString(R.string.intercepted_report, REPORT_PAYLOAD)
        Log.i(INTENT_LAB_TAG, "attacker-report-received")
      }
      intent.action == ACTION_VIEW_REPORT -> {
        statusText.setText(R.string.invalid_report)
        Log.i(INTENT_LAB_TAG, "attacker-report-rejected")
      }
      else -> statusText.setText(R.string.initial_status)
    }
  }

  companion object {
    private const val ACTION_VIEW_REPORT = "com.example.intentvictim.action.VIEW_REPORT"
    private const val EXTRA_REPORT = "com.example.intentvictim.extra.REPORT"
    private const val REPORT_PAYLOAD = "ANDSEC-DEMO-REPORT-001"
    private const val INTENT_LAB_TAG = "IntentLab"
  }
}
