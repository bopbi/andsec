package com.example.deeplinkvictim

data class PendingLogin(
  val flow: LoginFlow,
  val state: String,
  val codeVerifier: String?,
  val codeChallenge: String?,
  val expiresAtElapsedMillis: Long,
)
