package com.example.deeplinkvictim

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object LoginRequestFactory {
  const val REQUEST_TTL_MILLIS = 120_000L
  private val random = SecureRandom()

  fun create(flow: LoginFlow, nowElapsedMillis: Long): PendingLogin {
    val verifier = if (flow == LoginFlow.HARDENED_CODE) randomUrlSafeValue() else null
    return PendingLogin(
      flow = flow,
      state = randomUrlSafeValue(),
      codeVerifier = verifier,
      codeChallenge = verifier?.let(::s256Challenge),
      expiresAtElapsedMillis = nowElapsedMillis + REQUEST_TTL_MILLIS,
    )
  }

  fun s256Challenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.UTF_8))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  private fun randomUrlSafeValue(): String {
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }
}
