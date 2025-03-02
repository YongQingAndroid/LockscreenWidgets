package tk.zwander.lockscreenwidgets.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class LogUtils private constructor(private val context: Context) {
    companion object {
        const val NORMAL_LOG_TAG = "LockscreenWidgets"
        const val DEBUG_LOG_TAG = "${NORMAL_LOG_TAG}Debug"

        @SuppressLint("StaticFieldLeak")
        private var instance: LogUtils? = null

        fun getInstance(context: Context): LogUtils {
            return instance ?: LogUtils(context.safeApplicationContext).also {
                instance = it
            }
        }
    }

    private val logFile = File(context.cacheDir, "log.txt")

    fun debugLog(message: String, throwable: Throwable = Exception()) {
        if (context.isDebug) {
            val fullMessage = generateFullMessage(message, throwable)

            Log.e(DEBUG_LOG_TAG, fullMessage)

            logFile.appendText("\n\n$fullMessage")
        }
    }

    fun normalLog(message: String, throwable: Throwable = Exception()) {
        val fullMessage = generateFullMessage(message, throwable)

        Log.e(NORMAL_LOG_TAG, fullMessage)

        logFile.appendText("\n\n$fullMessage")
    }

    fun resetDebugLog() {
        logFile.delete()
    }

    fun exportLog(out: OutputStream) {
        out.use { output ->
            logFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    private fun generateFullMessage(message: String, throwable: Throwable): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault())

        return "${formatter.format(Date())}\n${message}\n${Log.getStackTraceString(throwable)}"
    }
}
