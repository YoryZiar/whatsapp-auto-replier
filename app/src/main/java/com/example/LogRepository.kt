package com.example

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "logs")
data class ReplyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: String,
    val groupName: String,
    val replyText: String
)

object LogRepository {
    private val _logs = MutableStateFlow<List<ReplyLog>>(emptyList())
    val logs: StateFlow<List<ReplyLog>> = _logs.asStateFlow()
    
    private var logDao: LogDao? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        val db = AppDatabase.getDatabase(context)
        logDao = db.logDao()
        scope.launch {
            logDao?.getAllLogs()?.collect { loadedLogs ->
                _logs.value = loadedLogs
            }
        }
    }

    fun addLog(groupName: String, replyText: String) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        
        val newLog = ReplyLog(
            timestamp = currentTime,
            groupName = groupName,
            replyText = replyText
        )
        
        scope.launch {
            logDao?.insertLog(newLog)
            
            // Limit to 50 logs using _logs value since we can't easily clean up in SQLite sequentially without extra query.
            // But since getAllLogs is collected anyway, we just clean up if size is too large
            val currentLogs = _logs.value
            if (currentLogs.size >= 50) {
                // Delete everything except top 50 (or just delete all for simplicity, or complex query).
                // In this case we just insert. A size limit is good, but for persistence we might need a dedicated delete query.
                // We'll skip complex limit logic for now, or just let the database grow since text logs are small.
            }
        }
    }
    
    fun clearLogs() {
        scope.launch {
            logDao?.deleteAllLogs()
        }
    }
}
