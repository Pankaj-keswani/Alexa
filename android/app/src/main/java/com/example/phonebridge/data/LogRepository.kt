package com.example.phonebridge.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object LogRepository {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val memoryLogs = CopyOnWriteArrayList<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow = _logsFlow.asStateFlow()

    fun addLog(tag: String, message: String, isError: Boolean = false) {
        val entry = LogEntry(
            id = System.currentTimeMillis(),
            timestamp = timeFormat.format(Date()),
            tag = tag,
            message = message,
            isError = isError
        )
        memoryLogs.add(0, entry)
        if (memoryLogs.size > 200) {
            memoryLogs.removeAt(memoryLogs.size - 1)
        }
        _logsFlow.value = ArrayList(memoryLogs)
    }

    fun clear() {
        memoryLogs.clear()
        _logsFlow.value = emptyList()
    }
}
