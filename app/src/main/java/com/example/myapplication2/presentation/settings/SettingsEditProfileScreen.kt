package com.example.myapplication2.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.ui.theme.PrimaryGreen
import com.example.myapplication2.ui.theme.PureWhite

private val editRoles = listOf(
    "Regulatory Affairs Manager",
    "Quality Assurance Specialist",
    "R&D Engineer",
    "Clinical Affairs",
    "Regulatory Consultant",
    "CEO / Business Owner",
)

private val editCountries = listOf(
    "Ukraine 🇺🇦", "Poland 🇵🇱", "Germany 🇩🇪", "France 🇫🇷",
    "Finland 🇫🇮", "Netherlands 🇳🇱", "USA 🇺🇸", "Other 🌍",
)

/**
 * Full-screen profile edit: [Scaffold] + [LazyColumn] only — avoids nested scroll + [Modifier.weight] chains
 * that can mis-measure or crash on some devices.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsEditProfileScreen(vm: MainSettingsViewModel) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PureWhite,
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically,
                ) {
                    IconButton(onClick = vm::cancelEditRole) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                    Text(
                        "Profile setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Button(
                        onClick = vm::saveRole,
                        enabled = !vm.isSavingRole &&
                            vm.editRole.isNotBlank() &&
                            vm.editSector.isNotBlank() &&
                            vm.editNiches.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    ) {
                        if (vm.isSavingRole) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Text(
                    "Regulatory sector",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Choose your compliance domain — niches below match this sector only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SectorCatalog.all.forEach { s ->
                        FilterChip(
                            selected = vm.editSector == s.key,
                            onClick = { vm.setSector(s.key) },
                            label = { Text("${s.icon} ${s.label}", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryGreen,
                            ),
                        )
                    }
                }
            }

            item {
                Text(
                    "Role",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    editRoles.forEach { role ->
                        val selected = vm.editRole == role
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) PrimaryGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.5.dp,
                                if (selected) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            ),
                            modifier = Modifier.fillMaxWidth().clickable { vm.editRole = role },
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                Arrangement.SpaceBetween,
                                Alignment.CenterVertically,
                            ) {
                                Text(
                                    role,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Niches in this sector (1–5) — ${vm.editNiches.size} selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NicheCatalog.forSector(vm.editSector.ifBlank { SectorCatalog.DEFAULT_KEY }).forEach { n ->
                        val selected = n.promptKey in vm.editNiches
                        FilterChip(
                            selected = selected,
                            onClick = { vm.toggleNiche(n.promptKey) },
                            label = { Text("${n.icon} ${n.name}", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryGreen,
                            ),
                        )
                    }
                }
            }

            item {
                Text(
                    "Country",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    editCountries.forEach { c ->
                        FilterChip(
                            selected = vm.editCountry == c,
                            onClick = { vm.editCountry = c },
                            label = { Text(c, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryGreen,
                            ),
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
