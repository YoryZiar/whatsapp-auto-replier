package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

data class ReplyRule(
    val id: String = UUID.randomUUID().toString(),
    val incomingKeyword: String,
    val matchType: MatchType,
    val replyMessage: String,
    val isActive: Boolean = true,
    val targetScope: RuleScope = RuleScope.Both,
    val replyOnlyIfMentioned: Boolean = false
)

object RuleRepository {
    private val _rules = MutableStateFlow<List<ReplyRule>>(listOf(
        ReplyRule(
            incomingKeyword = "hello",
            matchType = MatchType.SimilarityMatch,
            replyMessage = "Hello from AutoReply! How can I help you today?",
            targetScope = RuleScope.Both
        ),
        ReplyRule(
            incomingKeyword = "",
            matchType = MatchType.WelcomeMessage,
            replyMessage = "Welcome! This is an automated response.",
            targetScope = RuleScope.Private
        )
    ))
    val rules: StateFlow<List<ReplyRule>> = _rules.asStateFlow()

    fun addRule(rule: ReplyRule) {
        _rules.update { list -> list + rule }
    }

    fun deleteRule(id: String) {
        _rules.update { list -> list.filter { it.id != id } }
    }

    fun updateRule(rule: ReplyRule) {
        _rules.update { list -> list.map { if (it.id == rule.id) rule else it } }
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
