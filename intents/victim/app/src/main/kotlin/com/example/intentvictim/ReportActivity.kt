package com.example.intentvictim

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class ReportActivity : Activity() {
  private lateinit var reportText: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_report)
    reportText = findViewById(R.id.report_text)
    showReport(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    showReport(intent)
  }

  private fun showReport(intent: Intent) {
    val report = intent.getStringExtra(MainActivity.EXTRA_REPORT)
    if (intent.action == MainActivity.ACTION_VIEW_REPORT && report == MainActivity.REPORT_PAYLOAD) {
      reportText.text = getString(R.string.report_received, MainActivity.REPORT_PAYLOAD)
      Log.i(INTENT_LAB_TAG, "victim-report-received")
    } else {
      reportText.setText(R.string.invalid_report)
      Log.i(INTENT_LAB_TAG, "victim-report-rejected")
    }
  }

  companion object {
    private const val INTENT_LAB_TAG = "IntentLab"
  }
}
