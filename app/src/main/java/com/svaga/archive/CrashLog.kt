package com.svaga.archive

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashLog {
    private const val FILE = "crash.txt"

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                File(context.filesDir, FILE).writeText(sw.toString())
            } catch (_: Throwable) {
                // never fail while reporting a failure
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? {
        val f = File(context.filesDir, FILE)
        return if (f.exists()) f.readText().take(3000) else null
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE).delete()
    }
}
