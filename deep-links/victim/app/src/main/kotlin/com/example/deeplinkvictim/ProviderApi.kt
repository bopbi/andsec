package com.example.deeplinkvictim

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONException
import org.json.JSONObject

object ProviderApi {
  private const val BASE_URL = "http://10.0.2.2:8765"

  fun completeLogin(validation: CallbackValidation.Accepted): Boolean {
    val token = when (validation.request.flow) {
      LoginFlow.VULNERABLE_TOKEN -> validation.responseValue
      LoginFlow.HARDENED_CODE -> exchangeCode(validation)
    }
    return token != null && fetchProfile(token)
  }

  private fun exchangeCode(validation: CallbackValidation.Accepted): String? {
    val verifier = validation.request.codeVerifier ?: return null
    val body = listOf(
      "grant_type" to "authorization_code",
      "client_id" to "andsec-victim",
      "redirect_uri" to "andsec-login://auth/callback",
      "code" to validation.responseValue,
      "code_verifier" to verifier,
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
      if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
      val response = connection.inputStream.bufferedReader().use { it.readText() }
      val json = JSONObject(response)
      if (json.optString("token_type") != "Bearer") return null
      json.getString("access_token")
    } catch (_: IOException) {
      null
    } catch (_: JSONException) {
      null
    } finally {
      connection.disconnect()
    }
  }

  private fun fetchProfile(token: String): Boolean {
    val connection = (URL("$BASE_URL/profile").openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 5_000
      readTimeout = 5_000
      setRequestProperty("Authorization", "Bearer $token")
    }
    return try {
      if (connection.responseCode != HttpURLConnection.HTTP_OK) return false
      val response = connection.inputStream.bufferedReader().use { it.readText() }
      JSONObject(response).getString("display_name") == "Demo User"
    } catch (_: IOException) {
      false
    } catch (_: JSONException) {
      false
    } finally {
      connection.disconnect()
    }
  }
}
