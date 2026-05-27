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
import java.util.UUID

enum class MatchType(val label: String) {
    ExactMatch("Exact Match"),
    SimilarityMatch("Similarity Match"),
    PatternMatching("Pattern Matching"),
    WelcomeMessage("Welcome Message")
}

enum class RuleScope(val label: String) {
    Private("Private Only"),
    Group("Group Only"),
    Both("Private & Group")
}

@Entity(tableName = "rules")
data class ReplyRule(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val incomingKeyword: String,
    val matchType: MatchType,
    val replyMessage: String,
    val isActive: Boolean = true,
    val targetScope: RuleScope = RuleScope.Both,
    val replyOnlyIfMentioned: Boolean = false
)

object RuleRepository {
    private val _rules = MutableStateFlow<List<ReplyRule>>(emptyList())
    val rules: StateFlow<List<ReplyRule>> = _rules.asStateFlow()

    private var ruleDao: RuleDao? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        val db = AppDatabase.getDatabase(context)
        ruleDao = db.ruleDao()
        scope.launch {
            ruleDao?.getAllRules()?.collect { loadedRules ->
                if (loadedRules.isEmpty()) {
                    // Populate default rules
                    val defaultHelloWorld = ReplyRule(
                        incomingKeyword = "hello",
                        matchType = MatchType.SimilarityMatch,
                        replyMessage = "Hello from AutoReply! How can I help you today?",
                        targetScope = RuleScope.Both
                    )
                    val defaultWelcome = ReplyRule(
                        incomingKeyword = "",
                        matchType = MatchType.WelcomeMessage,
                        replyMessage = "Welcome! This is an automated response.",
                        targetScope = RuleScope.Private
                    )
                    ruleDao?.insertRule(defaultHelloWorld)
                    ruleDao?.insertRule(defaultWelcome)
                } else {
                    _rules.value = loadedRules
                }
            }
        }
    }

    fun addRule(rule: ReplyRule) {
        scope.launch {
            ruleDao?.insertRule(rule)
        }
    }

    fun deleteRule(id: String) {
        scope.launch {
            ruleDao?.deleteRuleById(id)
        }
    }

    fun updateRule(rule: ReplyRule) {
        scope.launch {
            ruleDao?.updateRule(rule)
        }
    }

    fun findReply(messageText: String, isGroup: Boolean, isMentioned: Boolean): String? {
        val activeRules = _rules.value.filter { rule -> 
            if (!rule.isActive) return@filter false
            
            if (rule.targetScope == RuleScope.Private && isGroup) return@filter false
            if (rule.targetScope == RuleScope.Group && !isGroup) return@filter false
            
            if (isGroup && rule.replyOnlyIfMentioned && !isMentioned) return@filter false
            
            true
        }
        
        // Exact Match
        val exact = activeRules.find { it.matchType == MatchType.ExactMatch && it.incomingKeyword.equals(messageText, ignoreCase = true) }
        if (exact != null) return exact.replyMessage
        
        // Pattern Matching (regex)
        val pattern = activeRules.find { it.matchType == MatchType.PatternMatching && 
           try { Regex(it.incomingKeyword, RegexOption.IGNORE_CASE).containsMatchIn(messageText) } catch (e: Exception) { false } 
        }
        if (pattern != null) return pattern.replyMessage
        
        // Similarity Match (contains)
        val similarity = activeRules.find { it.matchType == MatchType.SimilarityMatch && messageText.contains(it.incomingKeyword, ignoreCase = true) }
        if (similarity != null) return similarity.replyMessage
        
        // Welcome Message (Fallback)
        val welcome = activeRules.find { it.matchType == MatchType.WelcomeMessage }
        if (welcome != null) return welcome.replyMessage

        return null
    }
}
