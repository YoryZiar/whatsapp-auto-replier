package com.example

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class WhatsAppReplyService : NotificationListenerService() {

    companion object {
        private val lastReplyTimes = mutableMapOf<String, Long>()
        private val lastMessageTexts = mutableMapOf<String, String>()
        private val lastGeneratedReplies = mutableMapOf<String, String>()
        private const val COOLDOWN_MS = 10_000L // 10 seconds cooldown
        private val lock = Any()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Cek apakah fitur auto reply aktif
        if (!SettingsManager.isServiceActive(this)) {
            return
        }

        // 1. Filter agar hanya memproses notifikasi dari package com.whatsapp
        if (sbn.packageName != "com.whatsapp") return

        val notification = sbn.notification
        
        // Prevent replying to group summaries
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return
        }

        val extras = notification.extras
        
        // 2. Ekstraksi title dan text
        var title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        // Hapus jumlah pesan dari title, misalnya "John Doe (2 pesan)" -> "John Doe"
        title = title.replace(Regex("\\(\\d+\\s*(messages?|pesan|baru|new)?\\)", RegexOption.IGNORE_CASE), "").trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() || text.isBlank()) return
        if (title.equals("WhatsApp", ignoreCase = true)) return
        
        // Ignore summary texts like "2 messages" or "3 pesan baru"
        if (text.matches(Regex("^\\d+\\s*(new messages?|pesan baru|messages?|pesan)$", RegexOption.IGNORE_CASE))) return
        // Ignore outgoing messages (WhatsApp usually prefixes them when included in notifications, though often they just clear)
        if (text.startsWith("You: ", ignoreCase = true) || text.startsWith("Anda: ", ignoreCase = true)) return

        val currentTime = System.currentTimeMillis()
        
        synchronized(lock) {
            val lastReplyTime = lastReplyTimes[title] ?: 0L
            val lastMsgText = lastMessageTexts[title]
            
            // Prevent replying to the same exact message if it arrives very quickly (duplicate notification)
            // or if we are still in cooldown
            if (currentTime - lastReplyTime < COOLDOWN_MS) {
                Log.d("WAReplyService", "Skipping reply to $title due to cooldown or duplicate")
                return
            }

            // Mencegah double reply berulang jika WhatsApp me-repost notifikasi dengan pesan yang sama
            if (lastMsgText == text && (currentTime - lastReplyTime < 120_000L)) {
                Log.d("WAReplyService", "Skipping reply to $title because message text is identical and within 2 minutes")
                return
            }
            
            // Prevent replying if the new notification text contains our last sent reply 
            // (happens when WhatsApp shows the conversation history in the notification)
            val lastSentReply = lastGeneratedReplies[title]
            if (lastSentReply != null && text.contains(lastSentReply, ignoreCase = true) && (currentTime - lastReplyTime < 120_000L)) {
                Log.d("WAReplyService", "Skipping reply to $title because notification text contains our own recent reply")
                return
            }

            // 3. Deteksi Pesan Grup atau Pribadi:
            val isGroup = title.contains(":")
            
            Log.d("WAReplyService", "Detected message from: $title (isGroup: $isGroup)")
            
            // 4. Ekstraksi RemoteInput (Tombol Balas)
            val replyAction = extractReplyAction(notification)
            if (replyAction != null) {
                val remoteInput = replyAction.remoteInputs?.firstOrNull()
                if (remoteInput != null) {
                    // 5. Eksekusi Balasan
                    var textWithoutBroadcastMentions = text.replace("@all", "", ignoreCase = true)
                    textWithoutBroadcastMentions = textWithoutBroadcastMentions.replace("@everyone", "", ignoreCase = true)
                    textWithoutBroadcastMentions = textWithoutBroadcastMentions.replace("@semua", "", ignoreCase = true)
                    val isMentioned = textWithoutBroadcastMentions.contains("@")
                    
                    // Juga tambahkan pengecekan tambahan untuk mencegah reply ke pesan broadcast sama sekali jika di-set Only Mention
                    val hasBroadcastMention = text.contains("@all", ignoreCase = true) || text.contains("@everyone", ignoreCase = true) || text.contains("@semua", ignoreCase = true)
                    
                    if (isGroup && hasBroadcastMention) {
                        Log.d("WAReplyService", "Ignoring group broadcast mention message.")
                        return
                    }
                    
                    val generatedReply = RuleRepository.findReply(text, isGroup, isMentioned)
                    if (generatedReply == null) {
                        Log.d("WAReplyService", "No matching rule for message: $text")
                        return
                    }
                    
                    // Update tracker immediately within the synchronized block to prevent race conditions
                    lastMessageTexts[title] = text
                    lastReplyTimes[title] = currentTime
                    lastGeneratedReplies[title] = generatedReply
                    
                    sendReply(replyAction.actionIntent, remoteInput, generatedReply)
                    
                    // Tambahkan ke log aktivitas
                    val logSource = if (isGroup) "Group: $title" else "Private: $title"
                    LogRepository.addLog(logSource, generatedReply)
                }
            }
        }
    }

    private fun extractReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                // Notifikasi balasan (reply) biasanya memiliki resultKey yang digunakan untuk menempatkan teks.
                // Kita menganggap RemoteInput pertama yang ditemukan adalah untuk membalas pesan.
                if (remoteInput.resultKey != null) {
                    return action
                }
            }
        }
        return null
    }

    private fun sendReply(pendingIntent: PendingIntent, remoteInput: android.app.RemoteInput, replyText: String) {
        val localIntent = Intent()
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        val localBundle = Bundle()
        // Masukkan teks balasan kita ke dalam bundle menggunakan resultKey dari RemoteInput
        localBundle.putCharSequence(remoteInput.resultKey, replyText)
        
        // Gabungkan RemoteInput ke dalam intent dengan hasil bundle yang sudah diisi
        android.app.RemoteInput.addResultsToIntent(
            arrayOf(remoteInput),
            localIntent,
            localBundle
        )
        
        try {
            // Jalankan PendingIntent untuk mengirim pesan ke WhatsApp
            pendingIntent.send(this, 0, localIntent)
            Log.d("WAReplyService", "Reply sent successfully")
        } catch (e: PendingIntent.CanceledException) {
            Log.e("WAReplyService", "Failed to send reply", e)
        }
    }
}
