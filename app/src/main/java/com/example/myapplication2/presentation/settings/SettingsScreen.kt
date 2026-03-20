package com.example.myapplication2.presentation.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.common.CountryRegulatoryContext
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.RegulatoryOfflineIndex
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.BuildConfig
import com.example.myapplication2.MainActivity
import com.example.myapplication2.di.AppContainer
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.ui.theme.AppDimens
import com.example.myapplication2.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════
//  MAIN SETTINGS VIEWMODEL (was ProfileViewModel)
// ════════════════════════════════════════════════════════════

class MainSettingsViewModel(private val container: AppContainer) : ViewModel() {

    val profile: StateFlow<UserProfile?> = container.observeUserProfileUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Notifications ────────────────────────────────────────────
    var notificationsEnabled by mutableStateOf(true)
    var deadlineAlerts       by mutableStateOf(true)
    var weeklyDigest         by mutableStateOf(false)
    var urgentOnly           by mutableStateOf(false)

    // ── User role / position edit ────────────────────────────────
    var isEditingRole  by mutableStateOf(false)
    var editRole       by mutableStateOf("")
    var editSector     by mutableStateOf("")
    var editNiches     by mutableStateOf(setOf<String>())
    var editCountry    by mutableStateOf("")
    var isSavingRole   by mutableStateOf(false); private set
    var roleSaved      by mutableStateOf(false);  private set

    // ── Clear data ───────────────────────────────────────────────
    // ── Country picker (main settings) ─────────────────────────────
    var showCountryPicker by mutableStateOf(false)

    var showClearDialog   by mutableStateOf(false)
    var isClearing        by mutableStateOf(false); private set
    var clearDoneMessage  by mutableStateOf<String?>(null)

    var showResetAppDialog by mutableStateOf(false)
    var isResettingApp     by mutableStateOf(false); private set

    init {
        viewModelScope.launch {
            notificationsEnabled = runCatching { container.appSettingsRepository.getNotificationsEnabled() }.getOrDefault(true)
            deadlineAlerts       = runCatching { container.appSettingsRepository.getDeadlineAlertsEnabled() }.getOrDefault(true)
            weeklyDigest         = runCatching { container.appSettingsRepository.getWeeklyDigestEnabled() }.getOrDefault(false)
            urgentOnly           = runCatching { container.appSettingsRepository.getUrgentOnlyEnabled() }.getOrDefault(false)
        }
    }

    // ── Notifications ─────────────────────────────────────────────

    fun updateNotificationsEnabled(v: Boolean) {
        notificationsEnabled = v
        viewModelScope.launch { runCatching { container.appSettingsRepository.setNotificationsEnabled(v) } }
    }
    fun updateDeadlineAlerts(v: Boolean) {
        deadlineAlerts = v
        viewModelScope.launch { runCatching { container.appSettingsRepository.setDeadlineAlertsEnabled(v) } }
    }
    fun updateWeeklyDigest(v: Boolean) {
        weeklyDigest = v
        viewModelScope.launch { runCatching { container.appSettingsRepository.setWeeklyDigestEnabled(v) } }
    }
    fun updateUrgentOnly(v: Boolean) {
        urgentOnly = v
        viewModelScope.launch { runCatching { container.appSettingsRepository.setUrgentOnlyEnabled(v) } }
    }

    // ── Role / position edit ──────────────────────────────────────

    fun startEditRole() {
        val p = profile.value ?: return
        editRole    = p.role
        editSector  = p.sector.ifBlank { SectorCatalog.DEFAULT_KEY }
        editNiches  = p.niches.toSet()
        editCountry = p.country
        isEditingRole = true
    }

    fun setSector(key: String) {
        editSector = key
        editNiches = editNiches.filter { nk ->
            NicheCatalog.findByPromptKey(nk)?.sectorKey == key
        }.toSet()
    }

    fun toggleNiche(key: String) {
        editNiches = when {
            key in editNiches -> editNiches - key
            editNiches.size >= 5 -> editNiches
            else -> editNiches + key
        }
    }

    fun saveRole() {
        val current = profile.value ?: return
        viewModelScope.launch {
            isSavingRole = true
            runCatching {
                val updated = current.copy(
                    role = editRole,
                    sector = editSector,
                    niches = editNiches.toList(),
                    country = editCountry,
                )
                container.saveUserProfileUseCase(updated)
                roleSaved     = true
                isEditingRole = false
            }
            isSavingRole = false
        }
    }

    fun cancelEditRole() { isEditingRole = false }
    fun dismissRoleSaved() { roleSaved = false }

    fun openCountryPicker() {
        editCountry = profile.value?.country ?: ""
        showCountryPicker = true
    }

    /** Updates only the country; role and niches are preserved and not touched. */
    fun saveCountryAndClose(country: String) {
        val current = profile.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                role = current.role,
                niches = current.niches,
                deviceTypes = current.deviceTypes,
                country = country,
            )
            container.saveUserProfileUseCase(updated)
            editCountry = country
            showCountryPicker = false
        }
    }

    // ── Clear data ────────────────────────────────────────────────

    fun clearAllData() {
        viewModelScope.launch {
            isClearing = true
            runCatching { container.cardRepository.clearAll() }
            isClearing       = false
            showClearDialog  = false
            clearDoneMessage = "All data cleared"
        }
    }

    fun dismissClearDone() { clearDoneMessage = null }

    /** Full reset: profile, onboarding flags, cards, cache — then relaunch [MainActivity] like a first install. */
    fun resetAppAndRelaunch(context: Context) {
        viewModelScope.launch {
            isResettingApp = true
            runCatching {
                container.cardRepository.clearAll()
                container.cacheRepository.clearAll()
                container.userProfileRepository.clearUserProfile()
                container.appSettingsRepository.resetSessionForOnboarding()
            }
            isResettingApp = false
            showResetAppDialog = false
            isEditingRole = false
            val act = context as? ComponentActivity ?: return@launch
            act.startActivity(
                Intent(act, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                },
            )
            act.finishAffinity()
        }
    }
}

// ════════════════════════════════════════════════════════════
//  MAIN SETTINGS SCREEN
// ════════════════════════════════════════════════════════════

@Composable
fun MainSettingsScreen(
    vm: MainSettingsViewModel,
    onNavigateGlossary:  () -> Unit,
    onNavigateChecklist: () -> Unit,
) {
    val profile by vm.profile.collectAsState()

    // Must run every composition — never skip before branching (edit screen is separate composable).
    val context = LocalContext.current
    val officialLinks = remember(profile?.country, profile?.sector, profile?.niches) {
        RegulatoryOfflineIndex.mergedOfficialLinks(
            context.applicationContext,
            profile?.country ?: "",
            profile?.sector?.ifBlank { SectorCatalog.DEFAULT_KEY } ?: SectorCatalog.DEFAULT_KEY,
            profile?.niches.orEmpty(),
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        vm.updateNotificationsEnabled(granted)
    }
    val onNotificationsToggle: (Boolean) -> Unit = { v ->
        if (!v) {
            vm.updateNotificationsEnabled(false)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val has = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (has) vm.updateNotificationsEnabled(true)
            else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.updateNotificationsEnabled(true)
        }
    }

    if (vm.isEditingRole) {
        SettingsEditProfileScreen(vm = vm)
    } else {

    // Clear confirm dialog
    if (vm.showClearDialog) {
        AlertDialog(
            onDismissRequest = { vm.showClearDialog = false },
            icon  = { Icon(Icons.Filled.DeleteForever, null, tint = ErrorRed) },
            title = { Text("Clear all data?") },
            text  = { Text("All calendar, research and saved cards will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = vm::clearAllData,
                    colors  = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                ) {
                    if (vm.isClearing) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    else Text("Clear")
                }
            },
            dismissButton = { TextButton({ vm.showClearDialog = false }) { Text("Cancel") } },
        )
    }

    if (vm.showResetAppDialog) {
        AlertDialog(
            onDismissRequest = { if (!vm.isResettingApp) vm.showResetAppDialog = false },
            icon = { Icon(Icons.Filled.Refresh, null, tint = PrimaryGreen) },
            title = { Text("Start over?") },
            text = {
                Text(
                    "This removes your profile, onboarding progress, saved cards and cache, then restarts the app — like a first install.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.resetAppAndRelaunch(context) },
                    enabled = !vm.isResettingApp,
                ) {
                    if (vm.isResettingApp) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Restart app", color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { vm.showResetAppDialog = false },
                    enabled = !vm.isResettingApp,
                ) { Text("Cancel") }
            },
        )
    }

    vm.clearDoneMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2_500)
            vm.dismissClearDone()
        }
    }

    if (vm.showCountryPicker) {
        val pickerCountries = listOf(
            "Ukraine 🇺🇦", "Poland 🇵🇱", "Germany 🇩🇪", "France 🇫🇷",
            "Finland 🇫🇮", "Netherlands 🇳🇱", "USA 🇺🇸", "Other 🌍",
        )
        AlertDialog(
            onDismissRequest = { vm.showCountryPicker = false },
            icon = { Icon(Icons.Filled.Public, null, tint = PrimaryGreen) },
            title = { Text("Country") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pickerCountries.forEach { c ->
                        val selected = vm.editCountry == c
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) PrimaryGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable {
                                vm.saveCountryAndClose(c)
                            },
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(c, style = MaterialTheme.typography.bodyLarge)
                                if (selected) Icon(Icons.Filled.CheckCircle, null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.showCountryPicker = false }) { Text("Close") } },
        )
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(PureWhite),
        contentPadding = PaddingValues(bottom = AppDimens.contentBottomInset),
    ) {

        // ── Header ─────────────────────────────────────────────────
        item {
            Box(
                Modifier.fillMaxWidth()
                    .background(PureWhite),
            ) {
                Column(Modifier.padding(horizontal = AppDimens.heroBlockPadding, vertical = 24.dp)) {
                    Text(
                        "Settings",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color      = PrimaryTeal,
                    )
                    Text(
                        "Sector, niches, country, notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedLight,
                    )
                }
            }
        }

        // ── Success snackbar ────────────────────────────────────────
        vm.clearDoneMessage?.let { msg ->
            item {
                Surface(
                    color  = SuccessGreen.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                    shape  = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = SuccessGreen)
                    }
                }
            }
        }

        // ── User position / role ────────────────────────────────────
        item {
            SettingsSection("Sector, role & niches", Icons.Filled.Person, PrimaryGreen) {
                Column {
                    ListItem(
                        headlineContent   = { Text(profile?.role?.ifBlank { "Not set" } ?: "Not set", fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            val sectorLine = profile?.sector?.takeIf { it.isNotBlank() }?.let { sk ->
                                SectorCatalog.find(sk)?.label ?: sk
                            }
                            val niches = profile?.niches.orEmpty()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (sectorLine != null) {
                                    Text(sectorLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (niches.isNotEmpty()) {
                                    Text(
                                        niches.take(5).joinToString(", ") { key ->
                                            NicheCatalog.findByKeyOrName(key)?.nameEn ?: key
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        },
                        leadingContent    = {
                            Surface(shape = CircleShape, color = PrimaryGreen.copy(alpha = 0.1f)) {
                                Text(
                                    profile?.role?.firstOrNull()?.uppercase() ?: "?",
                                    modifier = Modifier.padding(10.dp),
                                    style    = MaterialTheme.typography.titleMedium,
                                    color    = PrimaryGreen,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        },
                        trailingContent   = {
                            OutlinedButton(onClick = vm::startEditRole, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Icon(Icons.Filled.Edit, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit", style = MaterialTheme.typography.labelMedium)
                            }
                        },
                    )
                }
            }
        }

        // ── Country (change jurisdiction for search & calendar) ─────
        item {
            SettingsSection("Country", Icons.Filled.Language, PrimaryGreen) {
                val countries = listOf(
                    "Ukraine 🇺🇦", "Poland 🇵🇱", "Germany 🇩🇪", "France 🇫🇷",
                    "Finland 🇫🇮", "Netherlands 🇳🇱", "USA 🇺🇸", "Other 🌍",
                )
                ListItem(
                    headlineContent   = { Text(profile?.country?.ifBlank { "Not set" } ?: "Not set", fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Regulatory search and calendar use this jurisdiction") },
                    leadingContent    = {
                        Surface(shape = CircleShape, color = PrimaryGreen.copy(alpha = 0.1f)) {
                            Icon(Icons.Filled.Public, null, modifier = Modifier.padding(10.dp), tint = PrimaryGreen)
                        }
                    },
                    trailingContent   = {
                        OutlinedButton(onClick = vm::openCountryPicker, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                            Icon(Icons.Filled.Edit, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Change", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                )
            }
        }

        // ── Notifications ────────────────────────────────────────────
        item {
            SettingsSection("Notifications", Icons.Filled.Notifications, WarningAmber) {
                Column {
                    SettingsToggleRow("Enable notifications", "Regulatory deadlines and new events",
                        Icons.Filled.NotificationsActive, WarningAmber, vm.notificationsEnabled, onNotificationsToggle)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    SettingsToggleRow("Deadlines (30/14/7/3/1/0 days)", "Reminders before due dates",
                        Icons.Filled.Alarm, PrimaryGreen, vm.deadlineAlerts && vm.notificationsEnabled,
                        vm::updateDeadlineAlerts, enabled = vm.notificationsEnabled)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    SettingsToggleRow("Weekly digest", "News overview on Monday morning",
                        Icons.Filled.Schedule, InfoBlue, vm.weeklyDigest && vm.notificationsEnabled,
                        vm::updateWeeklyDigest, enabled = vm.notificationsEnabled)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    SettingsToggleRow("Critical & HIGH only", "Filter less important events",
                        Icons.Filled.PriorityHigh, ErrorRed, vm.urgentOnly && vm.notificationsEnabled,
                        vm::updateUrgentOnly, enabled = vm.notificationsEnabled)
                }
            }
        }

        // ── Tools ────────────────────────────────────────────────────
        item {
            val ctx = CountryRegulatoryContext.forCountry(profile?.country ?: "")
            SettingsSection("Tools", Icons.Filled.Build, PrimaryGreen) {
                Column {
                    ListItem(
                        headlineContent   = { Text("Regulatory Glossary") },
                        supportingContent = { Text("By sector & country • ${ctx.jurisdictionName}") },
                        leadingContent    = { Icon(Icons.Filled.MenuBook, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp)) },
                        trailingContent   = { Icon(Icons.Filled.ChevronRight, null) },
                        modifier          = Modifier.clickable { onNavigateGlossary() },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ListItem(
                        headlineContent   = { Text("Compliance Checklist") },
                        supportingContent = { Text("By sector • ${ctx.jurisdictionName}") },
                        leadingContent    = { Icon(Icons.Filled.CheckCircle, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp)) },
                        trailingContent   = { Icon(Icons.Filled.ChevronRight, null) },
                        modifier          = Modifier.clickable { onNavigateChecklist() },
                    )
                }
            }
        }

        // ── Official resources (by country) ───────────────────────────
        item {
            val uriHandler = LocalUriHandler.current
            val ctx = CountryRegulatoryContext.forCountry(profile?.country ?: "")
            SettingsSection("Official resources — ${ctx.jurisdictionName}", Icons.Filled.Link, PrimaryGreen) {
                Column {
                    Text(
                        "Bundled link index: country, sector, niche tags (${officialLinks.size} links)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    officialLinks.forEachIndexed { i, (label, url) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            leadingContent  = { Icon(Icons.Filled.Link, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp)) },
                            trailingContent = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(14.dp)) },
                            modifier        = Modifier.clickable { runCatching { uriHandler.openUri(url) } },
                        )
                        if (i < officialLinks.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    }
                }
            }
        }

        // ── Data & privacy ───────────────────────────────────────────
        item {
            SettingsSection("Data & privacy", Icons.Filled.Shield, ErrorRed) {
                Column {
                    ListItem(
                        headlineContent   = { Text("Clear all data", color = ErrorRed) },
                        supportingContent = { Text("Delete calendar, research, saved cards") },
                        leadingContent    = { Icon(Icons.Filled.DeleteForever, null, tint = ErrorRed, modifier = Modifier.size(18.dp)) },
                        modifier          = Modifier.clickable { vm.showClearDialog = true },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ListItem(
                        headlineContent   = { Text("Start over (first-time setup)") },
                        supportingContent = { Text("Erase profile & app data, then restart — full onboarding again") },
                        leadingContent    = { Icon(Icons.Filled.Refresh, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp)) },
                        modifier          = Modifier.clickable { vm.showResetAppDialog = true },
                    )
                }
            }
        }

        // ── About ───────────────────────────────────────────────────
        item {
            val aboutCtx = CountryRegulatoryContext.forCountry(profile?.country ?: "")
            SettingsSection("About", Icons.Filled.Info, InfoBlue) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AboutRow(Icons.Filled.Public,       aboutCtx.jurisdictionName, "Regulatory focus")
                    AboutRow(Icons.Filled.AutoAwesome,  "AI Engine",                "OpenAI GPT-4o-mini")
                    AboutRow(Icons.Filled.Language,     "UI language",              "English")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} · V2 (unified engine)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    }
}

// ════════════════════════════════════════════════════════════
//  HELPERS
// ════════════════════════════════════════════════════════════

@Composable
private fun SettingsSection(
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
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = color.copy(alpha = 0.12f)) {
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
private fun SettingsToggleRow(
    title:    String,
    subtitle: String,
    icon:     ImageVector,
    color:    Color,
    checked:  Boolean,
    onToggle: (Boolean) -> Unit,
    enabled:  Boolean = true,
) {
    ListItem(
        headlineContent   = { Text(title, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent    = { Icon(icon, null, tint = if (enabled) color else AppGrayLight, modifier = Modifier.size(18.dp)) },
        trailingContent   = {
            Switch(
                checked          = checked,
                onCheckedChange  = onToggle,
                enabled          = enabled,
                colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = color),
            )
        },
    )
}

@Composable
private fun AboutRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
        Icon(icon, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
