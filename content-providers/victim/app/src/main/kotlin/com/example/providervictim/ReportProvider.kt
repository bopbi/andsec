package com.example.providervictim

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileNotFoundException

class ReportProvider : ContentProvider() {
  override fun onCreate(): Boolean {
    context?.let(DemoReport::ensureCreated)
    return true
  }

  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
    if (uri != REPORT_URI || mode != "r") {
      throw FileNotFoundException("Unsupported report request")
    }
    val report = DemoReport.ensureCreated(context ?: throw IllegalStateException("Provider unavailable"))
    return ParcelFileDescriptor.open(report, ParcelFileDescriptor.MODE_READ_ONLY)
  }

  override fun getType(uri: Uri): String? = if (uri == REPORT_URI) "text/plain" else null

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?,
  ): Cursor = throw UnsupportedOperationException()

  override fun insert(uri: Uri, values: ContentValues?): Uri = throw UnsupportedOperationException()

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?,
  ): Int = throw UnsupportedOperationException()

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
    throw UnsupportedOperationException()

  companion object {
    private const val AUTHORITY = "com.example.providervictim.report"
    private val REPORT_URI = Uri.parse("content://$AUTHORITY/report")
  }
}
