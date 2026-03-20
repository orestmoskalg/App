package com.example.myapplication2.core.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

// ── Regulatory Document ────────────────────────────────────────────────────────

@Keep
@Serializable
data class RegulatoryDocument(
    val code: String,
    val title: String,
    val type: DocType,
    val url: String = "",
    val isBinding: Boolean = true,
)

@Keep
@Serializable
enum class DocType {
    REGULATION, GUIDANCE, STANDARD, TEMPLATE, DATABASE, DECISION
}

// ── Risk Matrix ────────────────────────────────────────────────────────────────

@Keep
@Serializable
data class RiskItem(
    val scenario: String,
    val consequence: String,
    val likelihood: RiskLevel,
    val severity: RiskLevel,
)

@Keep
@Serializable
enum class RiskLevel { HIGH, MEDIUM, LOW }

// ── Action Plan ────────────────────────────────────────────────────────────────

@Keep
@Serializable
data class ActionStep(
    val step: Int,
    val title: String,
    val description: String,
    val timeframe: String,
    val owner: String = "",
    val isBlocking: Boolean = false,
)

// ── Social Discussions ─────────────────────────────────────────────────────────

@Keep
@Serializable
enum class SocialPlatform {
    LINKEDIN, REDDIT, TWITTER_X, MEDTECH_FORUM,
    RAPS, ELSEVIERCONNECT, MDR_NEWS, REGULATORY_FOCUS, OTHER
}

@Keep
@Serializable
enum class DiscussionSentiment {
    CONCERN, POSITIVE, DEBATE, NEUTRAL, URGENT
}

@Keep
@Serializable
data class SocialDiscussion(
    val platform: SocialPlatform,
    val title: String,
    val snippet: String,
    val author: String = "",
    val url: String,
    val engagement: String = "",
    val postedDate: String = "",
    val sentiment: DiscussionSentiment = DiscussionSentiment.NEUTRAL,
    val keyTakeaway: String = "",
)

@Keep
@Serializable
data class SocialSummary(
    val overallSentiment: String,
    val topConcerns: List<String>,
    val topInsights: List<String>,
    val discussions: List<SocialDiscussion>,
)

// ── Research Result ────────────────────────────────────────────────────────────

@Keep
@Serializable
data class ResearchResult(
    val query: String,
    val executiveSummary: String,
    val regulatoryContext: String,
    val keyFindings: List<String>,
    val applicableDocuments: List<RegulatoryDocument>,
    val riskMatrix: List<RiskItem>,
    val actionPlan: List<ActionStep>,
    val expertInsight: String,
    val marketContext: String,
    val socialSummary: SocialSummary? = null,
    val relatedQueries: List<String>,
    val affectedNiches: List<String>,
    val complianceDeadline: String = "",
    val confidenceScore: Int = 85,
    val sources: List<String> = emptyList(),
    val timestampMillis: Long = 0L, // 0 means unset; caller sets the real value
)
