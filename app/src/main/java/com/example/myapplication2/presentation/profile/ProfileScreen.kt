package com.example.myapplication2.presentation.profile

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.RegulatoryOfflineIndex
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.UserStats
import com.example.myapplication2.ui.theme.AppDimens
import com.example.myapplication2.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ──────────────────────────────────────────────────────────────────

class ProfileViewModel(private val container: AppContainer) : ViewModel() {

    val profile: StateFlow<UserProfile?> = container.observeUserProfileUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _stats = MutableStateFlow(UserStats())
    val stats: StateFlow<UserStats> = _stats.asStateFlow()

    var isEditing   by mutableStateOf(false); private set
    var editRole    by mutableStateOf("")
    var editSector  by mutableStateOf("")
    var editNiches  by mutableStateOf(setOf<String>())
    var editCountry by mutableStateOf("")
    var isSaving    by mutableStateOf(false); private set
    var savedSuccess by mutableStateOf(false); private set

    init {
        // Load real stats when profile arrives
        viewModelScope.launch {
            profile.filterNotNull().collect { loadStats() }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            runCatching { _stats.value = container.cardRepository.getUserStats() }
        }
    }

    fun startEdit(p: UserProfile) {
        editRole    = p.role
        editSector  = p.sector.ifBlank { SectorCatalog.DEFAULT_KEY }
        editNiches  = p.niches.toSet()
        editCountry = p.country
        isEditing   = true
    }

    fun setSector(key: String) {
        editSector = key
        editNiches = editNiches.filter { nk -> NicheCatalog.findByKeyOrName(nk)?.sectorKey == key }.toSet()
    }

    fun toggleNiche(key: String) {
        editNiches = when {
            key in editNiches -> editNiches - key
            editNiches.size >= 5 -> editNiches
            else -> editNiches + key
        }
    }

    fun saveEdit() {
        val current = profile.value ?: return
        viewModelScope.launch {
            isSaving = true
            runCatching {
                val updated = current.copy(
                    role = editRole,
                    sector = editSector,
                    niches = editNiches.toList(),
                    country = editCountry,
                )
                container.saveUserProfileUseCase(updated)
                savedSuccess = true
                isEditing    = false
            }.onFailure {
                // keep isEditing=true so user can retry
            }
            isSaving = false
        }
    }

    fun cancelEdit()   { isEditing    = false }
    fun clearSuccess() { savedSuccess = false }
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    onNavigateGlossary:  () -> Unit,
    onNavigateChecklist: () -> Unit,
    onNavigateSettings:  () -> Unit = {},
) {
    val profile by vm.profile.collectAsState()

    // Auto-dismiss success banner
    if (vm.savedSuccess) {
        LaunchedEffect(vm.savedSuccess) {
            kotlinx.coroutines.delay(2_000)
            vm.clearSuccess()
        }
    }

    // Refresh stats when screen becomes visible
    LaunchedEffect(Unit) { vm.loadStats() }

    when {
        vm.isEditing && profile != null -> EditProfileSheet(
            vm      = vm,
            profile = profile!!,
        )
        else -> ProfileContent(
            vm                  = vm,
            profile             = profile,
            onNavigateGlossary  = onNavigateGlossary,
            onNavigateChecklist = onNavigateChecklist,
            onNavigateSettings  = onNavigateSettings,
        )
    }
}

// ── Main content ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileContent(
    vm: ProfileViewModel,
    profile: UserProfile?,
    onNavigateGlossary:  () -> Unit,
    onNavigateChecklist: () -> Unit,
    onNavigateSettings:  () -> Unit,
) {
    val stats by vm.stats.collectAsState()
    val context = LocalContext.current
    val officialLinks = remember(profile?.country, profile?.sector, profile?.niches) {
        RegulatoryOfflineIndex.mergedOfficialLinks(
            context.applicationContext,
            profile?.country ?: "",
            profile?.sector?.ifBlank { SectorCatalog.DEFAULT_KEY } ?: SectorCatalog.DEFAULT_KEY,
            profile?.niches.orEmpty(),
        )
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = AppDimens.contentBottomInset),
    ) {

        // ── Hero header ──────────────────────────────────────────────────────
        item {
            Box(
                Modifier.fillMaxWidth()
                    .background(PureWhite),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Avatar circle
                    Surface(
                        shape  = CircleShape,
                        color  = LightTeal,
                        border = BorderStroke(2.dp, PrimaryTeal.copy(alpha = 0.4f)),
                        modifier = Modifier.size(80.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text  = profile?.role?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal,
                            )
                        }
                    }

                    Text(
                        text  = profile?.role?.ifBlank { "Profile" } ?: "Incomplete profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                    )

                    if (!profile?.country.isNullOrBlank()) {
                        Text(
                            text  = profile?.country.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMutedLight,
                        )
                    }
                    if (!profile?.sector.isNullOrBlank()) {
                        Text(
                            text  = SectorCatalog.find(profile?.sector.orEmpty())?.label ?: profile?.sector.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedLight,
                        )
                    }

                    if (profile != null) {
                        Button(
                            onClick = { vm.startEdit(profile) },
                            shape   = RoundedCornerShape(12.dp),
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = PrimaryTeal,
                                contentColor   = PureWhite,
                            ),
                        ) {
                            Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit profile")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Success banner ───────────────────────────────────────────────────
        if (vm.savedSuccess) {
            item {
                Surface(
                    color  = SuccessGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape  = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Text("Profile saved", style = MaterialTheme.typography.bodyMedium, color = SuccessGreen)
                    }
                }
            }
        }

        // ── Niches ───────────────────────────────────────────────────────────
        val niches = profile?.niches.orEmpty()
        if (niches.isNotEmpty()) {
            item {
                ProfileSection("My niches", Icons.Filled.Category, PrimaryGreen) {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp),
                    ) {
                        niches.forEach { key ->
                            val niche = NicheCatalog.findByKeyOrName(key)
                            Surface(
                                shape  = RoundedCornerShape(20.dp),
                                color  = PrimaryGreen.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(niche?.icon ?: "🏥", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        niche?.nameEn ?: key,
                                        style      = MaterialTheme.typography.labelMedium,
                                        color      = PrimaryGreen,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Stats ─────────────────────────────────────────────────────────────
        item {
            ProfileSection("Statistics", Icons.Filled.BarChart, SuccessGreen) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatBox("${stats.searchCount}", "Research", PrimaryGreen)
                    StatBox("${stats.calendarCount}", "Events",     SuccessGreen)
                    StatBox("${stats.pinnedCount}",   "Pinned", WarningAmber)
                }
            }
        }

        // ── Settings ──────────────────────────────────────────────────────────
        item {
            ProfileSection("Settings", Icons.Filled.Settings, Color.Gray) {
                ListItem(
                    headlineContent   = { Text("App settings") },
                    supportingContent = { Text("Notifications, data") },
                    leadingContent    = { Icon(Icons.Filled.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    trailingContent   = { Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.clickable { onNavigateSettings() },
                )
            }
        }

        // ── Tools ──────────────────────────────────────────────────────────────
        item {
            val ctx = CountryRegulatoryContext.forCountry(profile?.country ?: "")
            ProfileSection("Tools", Icons.Filled.Build, PrimaryGreen) {
                Column {
                    ListItem(
                        headlineContent   = { Text("Regulatory Glossary") },
                        supportingContent = { Text("By sector & country • ${ctx.jurisdictionName}") },
                        leadingContent    = { Icon(Icons.Filled.MenuBook, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp)) },
                        trailingContent   = { Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.clickable { onNavigateGlossary() },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ListItem(
                        headlineContent   = { Text("Compliance checklist") },
                        supportingContent = { Text("By sector • ${ctx.jurisdictionName}") },
                        leadingContent    = { Icon(Icons.Filled.CheckCircle, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp)) },
                        trailingContent   = { Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.clickable { onNavigateChecklist() },
                    )
                }
            }
        }

        // ── About ──────────────────────────────────────────────────────────────
        item {
            val aboutCtx = CountryRegulatoryContext.forCountry(profile?.country ?: "")
            ProfileSection("About", Icons.Filled.Info, InfoBlue) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AboutRow(Icons.Filled.Public,       aboutCtx.jurisdictionName, "Regulatory focus")
                    AboutRow(Icons.Filled.AutoAwesome,  "AI",                       "OpenAI GPT-4o-mini")
                    AboutRow(Icons.Filled.Language,     "Language",                 "English")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Text(
                        "v2.1 • Regulatory Assistant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Official links (by country) ───────────────────────────────────────
        item {
            val uriHandler = LocalUriHandler.current
            val linkCtx = CountryRegulatoryContext.forCountry(profile?.country ?: "")
            ProfileSection("Official resources — ${linkCtx.jurisdictionName}", Icons.Filled.Link, PrimaryGreen) {
                Column {
                    Text(
                        "Country, sector & niche tags (bundled index + your profile)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    officialLinks.forEachIndexed { i, (label, url) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            leadingContent  = { Icon(Icons.Filled.Link, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp)) },
                            trailingContent = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.clickable { runCatching { uriHandler.openUri(url) } },
                        )
                        if (i < officialLinks.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

// ── Edit Profile Sheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileSheet(vm: ProfileViewModel, profile: UserProfile) {

    val roles = listOf(
        "Regulatory Affairs Manager", "Quality Assurance Specialist",
        "R&D Engineer", "Clinical Affairs",
        "Regulatory Consultant", "CEO / Business Owner",
    )
    val countries = listOf(
        "Ukraine 🇺🇦", "Poland 🇵🇱", "Germany 🇩🇪", "France 🇫🇷",
        "Finland 🇫🇮", "Netherlands 🇳🇱", "USA 🇺🇸", "Other 🌍",
    )

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Top bar
        Surface(shape = RectangleShape, shadowElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically,
            ) {
                IconButton(onClick = vm::cancelEdit) {
                    Icon(Icons.Filled.Close, "Cancel")
                }
                Text(
                    "Edit profile",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick  = vm::saveEdit,
                    enabled  = !vm.isSaving &&
                        vm.editRole.isNotBlank() &&
                        vm.editSector.isNotBlank() &&
                        vm.editNiches.isNotEmpty(),
                ) {
                    if (vm.isSaving) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = PureWhite)
                    } else {
                        Text("Save")
                    }
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // Role selector
            item {
                Text("Role", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.forEach { role ->
                        val selected = vm.editRole == role
                        Surface(
                            shape  = RoundedCornerShape(12.dp),
                            color  = if (selected) PrimaryGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.5.dp, if (selected) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().clickable { vm.editRole = role },
                        ) {
                            Row(Modifier.padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(
                                    role,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                                if (selected) Icon(Icons.Filled.CheckCircle, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Sector chips
            item {
                Text("Regulatory sector", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                ) {
                    SectorCatalog.all.forEach { s ->
                        FilterChip(
                            selected = vm.editSector == s.key,
                            onClick  = { vm.setSector(s.key) },
                            label    = { Text("${s.icon} ${s.label}", style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                selectedLabelColor     = PrimaryGreen,
                            ),
                        )
                    }
                }
            }

            // Niche chips
            item {
                Text(
                    "Niches (selected: ${vm.editNiches.size})",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                ) {
                    NicheCatalog.forSector(vm.editSector.ifBlank { SectorCatalog.DEFAULT_KEY }).forEach { n ->
                        val selected = n.promptKey in vm.editNiches
                        FilterChip(
                            selected    = selected,
                            onClick     = { vm.toggleNiche(n.promptKey) },
                            label       = { Text("${n.icon} ${n.name}", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (selected) { { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) } } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                selectedLabelColor     = PrimaryGreen,
                            ),
                        )
                    }
                }
            }

            // Country chips
            item {
                Text("Country", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                ) {
                    countries.forEach { c ->
                        FilterChip(
                            selected = vm.editCountry == c,
                            onClick  = { vm.editCountry = c },
                            label    = { Text(c, style = MaterialTheme.typography.labelMedium) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                selectedLabelColor     = PrimaryGreen,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSection(
    title:   String,
    icon:    ImageVector,
    color:   Color,
    content: @Composable () -> Unit,
) {
    Surface(
        shape          = RoundedCornerShape(16.dp),
        color          = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier       = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
                    Icon(icon, null, tint = color, modifier = Modifier.padding(8.dp).size(16.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            content()
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AboutRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
        Icon(icon, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
