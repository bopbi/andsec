package com.example.deeplinkvictim

enum class LoginFlow(val responseType: String) {
  VULNERABLE_TOKEN("token"),
  HARDENED_CODE("code"),
}
