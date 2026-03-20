package com.example.myapplication2.presentation.carddetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.common.AppJson
import com.example.myapplication2.core.model.*
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.presentation.components.*
import com.example.myapplication2.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString

// ── Source URL mapping ────────────────────────────────────────────────────────
private val SOURCE_URLS = mapOf(
    "EUR-Lex MDR"       to "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0745",
    "EU MDR"            to "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0745",
    "MDR 2017/745"      to "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0745",
    "EUR-Lex IVDR"      to "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0746",
    "EU IVDR"           to "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0746",
    "IVDR 2017/746"     to "https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32017R0746",
    "MDCG"              to "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en",
    "EUDAMED"           to "https://ec.europa.eu/tools/eudamed",
    "EMA"               to "https://www.ema.europa.eu/en/human-regulatory/post-authorisation/medical-devices",
    "NANDO"             to "https://ec.europa.eu/growth/tools-databases/nando/",
    "ISO 14971"         to "https://www.iso.org/standard/72704.html",
    "ISO 13485"         to "https://www.iso.org/standard/59752.html",
    "IEC 62304"         to "https://www.iso.org/standard/38421.html",
    "IEC 62366"         to "https://www.iso.org/standard/63179.html",
    "MEDDEV 2.7/1"      to "https://ec.europa.eu/health/md_sector/new_regulations/guidance_en",
    "RAPS"              to "https://www.raps.org/regulatory-focus",
    "FDA"               to "https://www.fda.gov/medical-devices",
)

private fun resolveSourceUrl(resource: String): String? {
    SOURCE_URLS.forEach { (key, url) ->
        if (resource.contains(key, ignoreCase = true)) return url
    }
    if (resource.startsWith("http://") || resource.startsWith("https://")) return resource
    return "https://www.google.com/search?q=${Uri.encode(resource)}+EU+MDR+IVDR"
}

// ── ViewModel ──────────────────────────────────────────────────────────────────

class CardDetailViewModel(cardId: String, private val container: AppContainer) : ViewModel() {
    val card: StateFlow<DashboardCard?> = container.observeCardUseCase(cardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun togglePin() {
        val c = card.value ?: return
        viewModelScope.launch { container.pinCardUseCase(c.id, !c.isPinned) }
    }

    fun share(context: android.content.Context, card: DashboardCard) {
        val text = buildString {
            appendLine(card.title)
            if (card.subtitle.isNotBlank()) appendLine(card.subtitle)
            appendLine()
            if (card.body.isNotBlank() && !card.body.startsWith("{")) appendLine(card.body)
            if (card.actionChecklist.isNotEmpty()) {
                appendLine("Actions:")
                card.actionChecklist.forEach { appendLine("• $it") }
            }
            appendLine()
            appendLine("Source: MDR/IVDR Assistant — AI analysis")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, card.title)
        }
        context.startActivity(Intent.createChooser(intent, "Share card"))
    }

    fun delete(onDone: () -> Unit) {
        val c = card.value ?: return
        viewModelScope.launch { container.deleteCardUseCase(c.id); onDone() }
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(vm: CardDetailViewModel, onBack: () -> Unit) {
    val card by vm.card.collectAsState()
    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            icon = { Icon(Icons.Filled.DeleteForever, null, tint = ErrorRed) },
            title = { Text("Delete card?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(onBack) },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }

    val shareContext = androidx.compose.ui.platform.LocalContext.current

    card?.let { c ->
        val research = remember(c.body) {
            if (c.type == CardType.SEARCH_HISTORY && c.body.startsWith("{") && "executiveSummary" in c.body)
                runCatching { AppJson.decodeFromString<ResearchResult>(c.body) }.getOrNull()
            else null
        }

        val hColor = cardHeaderColor(c.priority)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBackIosNew, "Back") }
                    },
                    actions = {
                        IconButton(onClick = vm::togglePin) {
                            Icon(
                                if (c.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                "Pin",
                                tint = if (c.isPinned) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { shareContext?.let { vm.share(it, c) } }) {
                            Icon(Icons.Filled.Share, "Share", tint = PrimaryGreen.copy(alpha = 0.75f))
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Filled.DeleteOutline, "Delete", tint = ErrorRed.copy(alpha = 0.65f))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        ) { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 56.dp)
            ) {
                // ── Hero ──
                Box(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CardTypeChip(c.type)
                            if (c.urgencyLabel.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(6.dp), color = hColor.copy(alpha = 0.12f)) {
                                    Text(c.urgencyLabel, style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold, color = hColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            PriorityBadge(c.priority)
                        }
                        Text(c.title, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                        Text(c.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (c.confidenceLabel.isNotBlank()) {
                                Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Verified, null, tint = PrimaryGreen, modifier = Modifier.size(13.dp))
                                    Text(c.confidenceLabel, style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
                                }
                            }
                            if (c.niche.isNotBlank()) {
                                Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Category, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                    Text(c.niche.take(32), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (research != null) {
                        ResearchDetailContent(research)
                    } else {
                        StandardCardContent(c)
                    }

                    if (c.links.isNotEmpty()) {
                        DetailBox(Icons.Filled.Link, "Preparation resources", PrimaryGreen) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                c.links.forEach { LinkCard(it) }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Updated: ${c.dateMillis.toReadableDate()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (c.isPinned) Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PushPin, null, tint = PrimaryGreen, modifier = Modifier.size(12.dp))
                            Text("Pinned", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
                        }
                    }
                }
            }
        }
    } ?: Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PrimaryGreen) }
}

// ── Research Result rendering ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResearchDetailContent(r: ResearchResult) {
    val uriHandler = LocalUriHandler.current

    if (r.executiveSummary.isNotBlank()) {
        DetailBox(Icons.Filled.Summarize, "Summary", PrimaryGreen) {
            Text(r.executiveSummary, style = MaterialTheme.typography.bodyLarge)
        }
    }

    if (r.regulatoryContext.isNotBlank()) {
        DetailBox(Icons.Filled.Gavel, "Regulatory context", PrimaryGreen) {
            Text(r.regulatoryContext, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (r.keyFindings.isNotEmpty()) {
        DetailBox(Icons.Filled.Lightbulb, "Key findings (${r.keyFindings.size})", PrimaryGreen) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                r.keyFindings.forEachIndexed { i, f ->
                    Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Surface(shape = CircleShape, color = PrimaryGreen.copy(alpha = 0.13f), modifier = Modifier.size(24.dp)) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text("${i+1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }
                        Text(f, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (r.applicableDocuments.isNotEmpty()) {
        DetailBox(Icons.Filled.Article, "Documents & regulations", InfoBlue) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                r.applicableDocuments.forEach { doc ->
                    val col = if (doc.isBinding) ErrorRed else InfoBlue
                    Surface(shape = RoundedCornerShape(10.dp), color = col.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, col.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(doc.code, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = InfoBlue)
                                Text(doc.title, style = MaterialTheme.typography.bodySmall)
                                if (doc.isBinding) Surface(shape = RoundedCornerShape(4.dp), color = ErrorRed.copy(alpha = 0.1f)) {
                                    Text("Required", style = MaterialTheme.typography.labelSmall, color = ErrorRed, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                            }
                            if (doc.url.isNotBlank()) {
                                IconButton(onClick = { runCatching { uriHandler.openUri(doc.url) } }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.OpenInNew, null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (r.riskMatrix.isNotEmpty()) {
        val high = r.riskMatrix.count { it.severity == RiskLevel.HIGH }
        DetailBox(Icons.Filled.Shield, "Risk matrix ($high critical)", ErrorRed) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                r.riskMatrix.forEach { risk ->
                    val sc = when (risk.severity) { RiskLevel.HIGH -> ErrorRed; RiskLevel.MEDIUM -> WarningAmber; else -> PrimaryGreen }
                    Surface(shape = RoundedCornerShape(10.dp), color = sc.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, sc.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(risk.scenario, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("→ ${risk.consequence}", style = MaterialTheme.typography.bodySmall, color = sc)
                            Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RiskPill("Likelihood", risk.likelihood)
                                RiskPill("Severity", risk.severity)
                            }
                        }
                    }
                }
            }
        }
    }

    if (r.actionPlan.isNotEmpty()) {
        val blocking = r.actionPlan.count { it.isBlocking }
        DetailBox(Icons.Filled.Checklist, "Action plan ($blocking critical)", PrimaryGreen) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                r.actionPlan.forEach { step ->
                    var checked by remember { mutableStateOf(false) }
                    Surface(modifier = Modifier.fillMaxWidth().clickable { checked = !checked },
                        shape = RoundedCornerShape(10.dp),
                        color = if (checked) PrimaryGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (step.isBlocking) PrimaryGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape,
                                color = if (checked) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                modifier = Modifier.size(26.dp)) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (checked) Icon(Icons.Filled.Check, null, tint = PureWhite, modifier = Modifier.size(13.dp))
                                    else Text("${step.step}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(step.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                                    textDecoration = if (checked) TextDecoration.LineThrough else null)
                                Text(step.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Schedule, null, tint = WarningAmber, modifier = Modifier.size(11.dp))
                                        Text(step.timeframe, style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                                    }
                                    if (step.owner.isNotBlank()) {
                                        Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(11.dp))
                                            Text(step.owner, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            if (step.isBlocking) Surface(shape = RoundedCornerShape(4.dp), color = ErrorRed.copy(alpha = 0.1f)) {
                                Text("!", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ErrorRed,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (r.expertInsight.isNotBlank()) {
        DetailBox(Icons.Filled.RecordVoiceOver, "Expert opinion", WarningAmber) {
            Text(r.expertInsight, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
        }
    }

    if (r.marketContext.isNotBlank()) {
        DetailBox(Icons.Filled.TrendingUp, "Market context", InfoBlue) {
            Text(r.marketContext, style = MaterialTheme.typography.bodyMedium)
        }
    }

    r.socialSummary?.let { s ->
        if (s.discussions.isNotEmpty() || s.overallSentiment.isNotBlank()) {
            DetailBox(Icons.Filled.Forum, "Community (${s.discussions.size} discussions)", PrimaryGreen) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (s.overallSentiment.isNotBlank()) Text(s.overallSentiment, style = MaterialTheme.typography.bodyMedium)
                    if (s.topConcerns.isNotEmpty()) {
                        Text("Concerns:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ErrorRed)
                        s.topConcerns.forEach { BulletText(it, ErrorRed) }
                    }
                    if (s.topInsights.isNotEmpty()) {
                        Text("Insights:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        s.topInsights.forEach { BulletText(it, PrimaryGreen) }
                    }
                }
            }
        }
    }

    if (r.relatedQueries.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Related topics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            r.relatedQueries.forEach { q ->
                Surface(shape = RoundedCornerShape(10.dp), color = PrimaryGreen.copy(alpha = 0.07f),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, null, tint = PrimaryGreen, modifier = Modifier.size(13.dp))
                        Text(q, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ── Standard card content ──────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StandardCardContent(c: DashboardCard) {
    if (c.body.isNotBlank() && !c.body.startsWith("{")) {
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Text(c.body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp),
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight)
        }
    }

    c.expertOpinion?.takeIf { it.isNotBlank() }?.let {
        DetailBox(Icons.Filled.Person, "Expert opinion", PrimaryGreen) {
            Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
        }
    }

    c.analytics?.takeIf { it.isNotBlank() }?.let {
        DetailBox(Icons.Filled.BarChart, "Analytics", SuccessGreen) {
            Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }

    if (c.actionChecklist.isNotEmpty()) {
        DetailBox(Icons.Filled.Checklist, "Actions to complete (${c.actionChecklist.size})", PrimaryGreen) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                c.actionChecklist.forEach { action ->
                    var checked by remember { mutableStateOf(false) }
                    Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Checkbox(checked = checked, onCheckedChange = { checked = it },
                            modifier = Modifier.size(20.dp),
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen))
                        Text(action, style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (checked) TextDecoration.LineThrough else null,
                            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (c.riskFlags.isNotEmpty()) {
        DetailBox(Icons.Filled.Warning, "Risks", ErrorRed) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { c.riskFlags.forEach { BulletText(it, ErrorRed) } }
        }
    }

    if (c.impactAreas.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Impact areas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.impactAreas.forEach { area ->
                    Surface(shape = RoundedCornerShape(8.dp), color = PrimaryGreen.copy(alpha = 0.09f)) {
                        Text(area, style = MaterialTheme.typography.labelMedium, color = PrimaryGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
        }
    }

    if (c.resources.isNotEmpty()) {
        DetailBox(Icons.Filled.Book, "Sources", InfoBlue) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                c.resources.forEach { res ->
                    val uriHandler = LocalUriHandler.current
                    val resolvedUrl = resolveSourceUrl(res)
                    Surface(
                        onClick = { resolvedUrl?.let { runCatching { uriHandler.openUri(it) } } },
                        shape = RoundedCornerShape(8.dp),
                        color = InfoBlue.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, InfoBlue.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            Arrangement.spacedBy(8.dp), Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Article, null, tint = InfoBlue, modifier = Modifier.size(13.dp))
                            Text(res, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.OpenInNew, null, tint = InfoBlue.copy(alpha = 0.5f), modifier = Modifier.size(11.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────────

@Composable
fun DetailBox(icon: ImageVector, title: String, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            }
            content()
        }
    }
}

@Composable
private fun LinkCard(link: CardLink) {
    val uriHandler = LocalUriHandler.current
    OutlinedCard(onClick = { runCatching { uriHandler.openUri(link.url) } }, shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(link.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                if (link.sourceLabel.isNotBlank())
                    Text(link.sourceLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (link.isVerified) Icon(Icons.Filled.Verified, null, tint = PrimaryGreen, modifier = Modifier.size(13.dp))
                Icon(Icons.Filled.OpenInNew, null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
fun BulletText(text: String, color: Color) {
    Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color).padding(top = 6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RiskPill(label: String, level: RiskLevel) {
    val col = when (level) { RiskLevel.HIGH -> ErrorRed; RiskLevel.MEDIUM -> WarningAmber; else -> PrimaryGreen }
    val lbl = when (level) { RiskLevel.HIGH -> "High"; RiskLevel.MEDIUM -> "Medium"; else -> "Low" }
    Surface(shape = RoundedCornerShape(6.dp), color = col.copy(alpha = 0.1f)) {
        Text("$label: $lbl", style = MaterialTheme.typography.labelSmall, color = col,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
    }
}

@Composable
private fun cardHeaderColor(p: Priority): Color = when (p) {
    Priority.CRITICAL -> ErrorRed
    Priority.HIGH     -> WarningAmber
    Priority.MEDIUM   -> MediumPriorityGreen
    Priority.LOW      -> PriorityLowColor
}
