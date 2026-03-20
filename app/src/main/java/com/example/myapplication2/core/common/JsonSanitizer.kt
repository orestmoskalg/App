package com.example.myapplication2.core.common

/**
 * Strips markdown fences and surrounding prose so JSON can be parsed.
 * Used by tests and any code path that must normalize model output.
 */
fun sanitizeJson(raw: String): String {
    var s = raw.trim()
    if (s.startsWith("```")) {
        s = s.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trim()
        val fence = s.lastIndexOf("```")
        if (fence >= 0) s = s.substring(0, fence).trim()
    }
    val obj = s.indexOf('{')
    val arr = s.indexOf('[')
    val start = when {
        obj >= 0 && arr >= 0 -> minOf(obj, arr)
        obj >= 0 -> obj
        arr >= 0 -> arr
        else -> 0
    }
    val slice = s.substring(start)
    val endObj = slice.lastIndexOf('}')
    val endArr = slice.lastIndexOf(']')
    val end = maxOf(endObj, endArr)
    return if (end >= 0) slice.substring(0, end + 1).trim() else slice.trim()
}
