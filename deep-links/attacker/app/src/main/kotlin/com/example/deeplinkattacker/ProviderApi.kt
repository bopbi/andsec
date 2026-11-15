package com.example.deeplinkattacker

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONException
import org.json.JSONObject

enum class TestOutcome {
  PROFILE_RECEIVED,
  CODE_REJECTED,
  REQUEST_FAILED,
}

object ProviderApi {
  private const val BASE_URL = "http://10.0.2.2:8765"

  fun test(response: InterceptedResponse): TestOutcome = when (response.kind) {
    ResponseKind.ACCESS_TOKEN -> requestProfile(response.value)
    ResponseKind.AUTHORIZATION_CODE -> rejectCode(response.value)
  }

  private fun requestProfile(token: String): TestOutcome {
    val connection = (URL("$BASE_URL/profile").openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 5_000
      readTimeout = 5_000
      setRequestProperty("Authorization", "Bearer $token")
    }
    return try {
      if (connection.responseCode != HttpURLConnection.HTTP_OK) return TestOutcome.REQUEST_FAILED
      val body = connection.inputStream.bufferedReader().use { it.readText() }
      if (JSONObject(body).getString("display_name") == "Demo User") {
        TestOutcome.PROFILE_RECEIVED
      } else {
        TestOutcome.REQUEST_FAILED
      }
    } catch (_: IOException) {
      TestOutcome.REQUEST_FAILED
    } catch (_: JSONException) {
      TestOutcome.REQUEST_FAILED
    } finally {
      connection.disconnect()
    }
  }

  private fun rejectCode(code: String): TestOutcome {
    val body = listOf(
      "grant_type" to "authorization_code",
      "client_id" to "andsec-victim",
      "redirect_uri" to "andsec-login://auth/callback",
      "code" to code,
    ).joinToString("&") { (key, value) ->
      "${URLEncoder.encode(key, Charsets.UTF_8.name())}=${URLEncoder.encode(value, Charsets.UTF_8.name())}"
    }
    val connection = (URL("$BASE_URL/token").openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 5_000
      readTimeout = 5_000
      doOutput = true
      setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    }
    return try {
      connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
      if (connection.responseCode != HttpURLConnection.HTTP_BAD_REQUEST) return TestOutcome.REQUEST_FAILED
      val response = connection.errorStream.bufferedReader().use { it.readText() }
      if (JSONObject(response).optString("error") == "invalid_grant") {
        TestOutcome.CODE_REJECTED
      } else {
        TestOutcome.REQUEST_FAILED
      }
    } catch (_: IOException) {
      TestOutcome.REQUEST_FAILED
    } catch (_: JSONException) {
      TestOutcome.REQUEST_FAILED
    } finally {
      connection.disconnect()
    }
  }
}
