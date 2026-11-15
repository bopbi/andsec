package com.example.providervictim

import android.content.Context
import java.io.File

object DemoReport {
  const val PAYLOAD = "ANDSEC-DEMO-REPORT-001"

  fun ensureCreated(context: Context): File {
    val directory = File(context.filesDir, "shared-reports")
    if (!directory.exists()) {
      directory.mkdirs()
    }
    return File(directory, "report.txt").apply {
      if (!exists()) {
        writeText(PAYLOAD)
      }
    }
  }
}
