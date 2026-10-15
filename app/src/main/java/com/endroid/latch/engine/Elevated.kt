package com.endroid.latch.engine

/** Detects whether `su` is available (root). Shizuku can be wired later the same way. */
object Elevated {
    fun hasSu(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        val code = p.waitFor()
        code == 0
    } catch (_: Exception) {
        false
    }
}
