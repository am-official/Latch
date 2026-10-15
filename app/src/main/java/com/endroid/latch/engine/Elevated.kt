package com.endroid.latch.engine

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object Elevated {
    private const val TAG = "LatchElevated"

    fun hasSu(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo ok"))
        val out = BufferedReader(InputStreamReader(p.inputStream)).readLine()
        p.waitFor() == 0 && out?.contains("ok") == true
    } catch (_: Exception) {
        false
    }

    fun shizukuAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun shizukuPermissionGranted(): Boolean = try {
        if (!shizukuAvailable()) false
        else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun requestShizukuPermission(requestCode: Int = 9001) {
        try {
            if (!shizukuPermissionGranted()) Shizuku.requestPermission(requestCode)
        } catch (e: Throwable) {
            Log.w(TAG, "shizuku request", e)
        }
    }

    /** Shizuku.newProcess is not public on all API jars — invoke via reflection. */
    private fun shizukuProcess(cmd: Array<String>): Process? {
        return try {
            val m = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            )
            m.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            m.invoke(null, cmd, null, null) as? Process
        } catch (e: Throwable) {
            Log.w(TAG, "shizuku newProcess reflection", e)
            null
        }
    }

    fun runShell(cmd: String, preferShizuku: Boolean, allowSu: Boolean): Pair<Int, String> {
        if (preferShizuku && shizukuPermissionGranted()) {
            val process = shizukuProcess(arrayOf("sh", "-c", cmd))
            if (process != null) {
                return try {
                    val stdout = process.inputStream.bufferedReader().readText()
                    process.waitFor() to stdout.take(4000)
                } catch (e: Throwable) {
                    Log.w(TAG, "shizuku shell", e)
                    -1 to (e.message ?: "shizuku failed")
                }
            }
        }
        if (allowSu && hasSu()) {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                val stdout = p.inputStream.bufferedReader().readText()
                return p.waitFor() to stdout.take(4000)
            } catch (e: Exception) {
                Log.w(TAG, "su shell failed", e)
            }
        }
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val stdout = p.inputStream.bufferedReader().readText()
            p.waitFor() to stdout.take(4000)
        } catch (e: Exception) {
            -1 to (e.message ?: "exec failed")
        }
    }
}
