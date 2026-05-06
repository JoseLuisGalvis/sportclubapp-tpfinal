package com.sportclub.app.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object AppLogger {

    enum class Level { DEBUG, INFO, WARNING, ERROR, CRITICAL }

    private const val TAG             = "SportClubApp"
    private const val DIAS_A_CONSERVAR = 30

    private var logDir: File? = null
    private val sdfFecha      = SimpleDateFormat("yyyyMMdd",           Locale.getDefault())
    private val sdfTimestamp  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(context: Context) {
        logDir = File(context.getExternalFilesDir(null), "Logs").also {
            if (!it.exists()) it.mkdirs()
        }
        limpiarLogsAntiguos()
        info("Logger inicializado — SportClubApp")
    }

    fun debug(message: String)                           = log(Level.DEBUG,    message)
    fun info(message: String)                            = log(Level.INFO,     message)
    fun warning(message: String)                         = log(Level.WARNING,  message)
    fun error(message: String, ex: Throwable? = null)    = log(Level.ERROR,    message, ex)
    fun critical(message: String, ex: Throwable? = null) = log(Level.CRITICAL, message, ex)

    private fun log(level: Level, message: String, ex: Throwable? = null) {
        val timestamp = sdfTimestamp.format(Date())
        val levelPad  = level.name.padEnd(8)
        val linea     = "[$timestamp] [$levelPad] $message"

        when (level) {
            Level.DEBUG    -> Log.d(TAG, message, ex)
            Level.INFO     -> Log.i(TAG, message, ex)
            Level.WARNING  -> Log.w(TAG, message, ex)
            Level.ERROR    -> Log.e(TAG, message, ex)
            Level.CRITICAL -> Log.wtf(TAG, message, ex)
        }

        logDir?.let { dir ->
            logScope.launch {
                try {
                    val archivo   = File(dir, "app_${sdfFecha.format(Date())}.log")
                    val contenido = buildString {
                        appendLine(linea)
                        if (ex != null) {
                            appendLine("    Exception: ${ex.javaClass.simpleName}")
                            appendLine("    Message:   ${ex.message}")
                            appendLine("    Stack:     ${ex.stackTraceToString().take(800)}")
                        }
                    }
                    archivo.appendText(contenido)
                } catch (_: Exception) { }
            }
        }
    }

    private fun limpiarLogsAntiguos() {
        val dir = logDir ?: return
        logScope.launch {
            val corte = System.currentTimeMillis() - DIAS_A_CONSERVAR.toLong() * 24 * 60 * 60 * 1000
            try {
                dir.listFiles { f -> f.name.endsWith(".log") && f.lastModified() < corte }
                    ?.forEach { it.delete() }
            } catch (_: Exception) {}
        }
    }

    fun getRutaLogs(): String      = logDir?.absolutePath ?: "No disponible"
    fun listarLogs(): List<File>   =
        logDir?.listFiles { f -> f.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
}