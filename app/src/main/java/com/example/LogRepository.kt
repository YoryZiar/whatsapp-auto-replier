package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReplyLog(
    val timestamp: String,
    val groupName: String,
    val replyText: String
)

object LogRepository {
    private val _logs = MutableStateFlow<List<ReplyLog>>(emptyList())
    val logs: StateFlow<List<ReplyLog>> = _logs.asStateFlow()

    fun addLog(groupName: String, replyText: String) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        
        val newLog = ReplyLog(
            timestamp = currentTime,
            groupName = groupName,
            replyText = replyText
        )
        
        _logs.update { currentLogs ->
            val updated = currentLogs.toMutableList()
            updated.add(0, newLog)
            if (updated.size > 50) {
                updated.removeLast()
            }
            updated
        }
    }
    
    fun clearLogs() {
        _logs.update { emptyList() }
    }
}
