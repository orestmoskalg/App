package com.example.myapplication2.core.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMessageHelper {
    fun userFriendlyMessage(throwable: Throwable?): String {
        if (throwable == null) return "Something went wrong. Please try again."
        return when (throwable) {
            is UnknownHostException -> "No internet connection. Check your network."
            is SocketTimeoutException -> "Request timed out. Check internet and try again."
            is IOException -> "Network error. Check your connection."
            else -> {
                val msg = throwable.message.orEmpty()
                when {
                    msg.contains("401") -> "Invalid API key. Key is in secrets.properties (OPENAI_API_KEY)."
                    msg.contains("403") -> "Access denied. Check OPENAI_API_KEY in secrets.properties."
                    msg.contains("429") -> "Too many requests. Wait and try again later."
                    msg.contains("500") || msg.contains("502") || msg.contains("503") ->
                        "Server temporarily unavailable. Try again later."
                    msg.isNotBlank() && msg.length < 120 -> msg
                    else -> "Something went wrong. Please try again."
                }
            }
        }
    }
}
