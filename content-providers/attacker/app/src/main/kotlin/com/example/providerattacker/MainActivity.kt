package com.example.providerattacker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import java.io.FileNotFoundException
import java.io.IOException

class MainActivity : Activity() {
  private lateinit var statusText: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    statusText = findViewById(R.id.status_text)
    findViewById<Button>(R.id.read_vulnerable_button).setOnClickListener {
      readReport(VULNERABLE_URI, hardened = false)
    }
    findViewById<Button>(R.id.read_hardened_button).setOnClickListener {
      readReport(HARDENED_URI, hardened = true)
    }
    handleShareIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleShareIntent(intent)
  }

  private fun handleShareIntent(intent: Intent) {
    when {
      intent.action == ACTION_SHARE_REPORT &&
        intent.data == HARDENED_URI &&
        intent.type == "text/plain" -> {
        statusText.setText(R.string.hardened_share_received)
        Log.i(PROVIDER_LAB_TAG, "hardened-share-received")
      }
      intent.action == ACTION_SHARE_REPORT -> {
        statusText.setText(R.string.invalid_share)
        Log.i(PROVIDER_LAB_TAG, "hardened-share-rejected")
      }
      else -> statusText.setText(R.string.initial_status)
    }
  }

  private fun readReport(uri: Uri, hardened: Boolean) {
    try {
      val content = contentResolver.openInputStream(uri)?.use { input ->
        input.readBytes().toString(Charsets.UTF_8)
      }
      if (content == EXPECTED_PAYLOAD) {
        statusText.text = getString(R.string.report_read, EXPECTED_PAYLOAD)
        Log.i(PROVIDER_LAB_TAG, if (hardened) "hardened-read-succeeded" else "vulnerable-read-succeeded")
      } else {
        statusText.setText(R.string.invalid_report)
        Log.i(PROVIDER_LAB_TAG, if (hardened) "hardened-read-invalid" else "vulnerable-read-invalid")
      }
    } catch (_: SecurityException) {
      statusText.setText(if (hardened) R.string.hardened_read_denied else R.string.vulnerable_read_denied)
      Log.i(PROVIDER_LAB_TAG, if (hardened) "hardened-read-denied" else "vulnerable-read-denied")
    } catch (_: FileNotFoundException) {
      statusText.setText(if (hardened) R.string.hardened_read_failed else R.string.vulnerable_read_failed)
      Log.i(PROVIDER_LAB_TAG, if (hardened) "hardened-read-not-found" else "vulnerable-read-not-found")
    } catch (_: IOException) {
      statusText.setText(if (hardened) R.string.hardened_read_failed else R.string.vulnerable_read_failed)
      Log.i(PROVIDER_LAB_TAG, if (hardened) "hardened-read-failed" else "vulnerable-read-failed")
    }
  }

  companion object {
    private val VULNERABLE_URI = Uri.parse("content://com.example.providervictim.report/report")
    private val HARDENED_URI = Uri.parse(
      "content://com.example.providervictim.fileprovider/shared_reports/report.txt"
    )
    private const val ACTION_SHARE_REPORT = "com.example.providervictim.action.SHARE_REPORT"
    private const val EXPECTED_PAYLOAD = "ANDSEC-DEMO-REPORT-001"
    private const val PROVIDER_LAB_TAG = "ProviderLab"
  }
}
