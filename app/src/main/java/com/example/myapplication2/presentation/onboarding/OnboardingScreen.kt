package com.example.myapplication2.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.common.SectorCatalog
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.ui.theme.*

@Composable
fun OnboardingScreen(
    initialProfile: UserProfile? = null,
    onComplete: (UserProfile) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var selectedCountry by remember { mutableStateOf(initialProfile?.country.orEmpty()) }
    var selectedSector by remember { mutableStateOf(initialProfile?.sector.orEmpty()) }
    var selectedNiches by remember {
        mutableStateOf(initialProfile?.niches?.toSet() ?: emptySet())
    }

    val roles = listOf(
        "Regulatory Affairs Manager" to "🎯",
        "Quality Assurance Specialist" to "✅",
        "Compliance Officer" to "📋",
        "R&D Engineer" to "🔬",
        "Legal / Policy" to "⚖️",
        "Regulatory Consultant" to "💼",
        "CEO / Business Owner" to "🚀",
    )

    val countries = listOf(
        "Ukraine 🇺🇦", "Poland 🇵🇱", "Germany 🇩🇪", "France 🇫🇷",
        "Finland 🇫🇮", "Netherlands 🇳🇱", "USA 🇺🇸", "Other 🌍",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ContentBg, LightTealBg.copy(alpha = 0.45f), ContentBg),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= step) PrimaryGreen else BorderGray),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedContent(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                },
            ) { currentStep ->
                when (currentStep) {
                    0 -> StepCountry(
                        countries = countries,
                        selected = selectedCountry,
                        onSelect = { selectedCountry = it },
                        onNext = { if (selectedCountry.isNotEmpty()) step = 1 },
                    )
                    1 -> StepSector(
                        selected = selectedSector,
                        onSelect = { selectedSector = it },
                        onNext = { if (selectedSector.isNotEmpty()) step = 2 },
                    )
                    2 -> StepNiches(
                        sectorKey = selectedSector,
                        selected = selectedNiches,
                        onNext = { if (it.isNotEmpty()) { selectedNiches = it; step = 3 } },
                    )
                    3 -> StepRole(roles = roles) { role ->
                        onComplete(
                            UserProfile(
                                role = role,
                                sector = selectedSector,
                                niches = selectedNiches.toList(),
                                country = selectedCountry,
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCountry(
    countries: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Where do you work?",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryTextDark,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Country / jurisdiction first — all research and prompts follow this choice.",
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryTextMedium,
        )
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(countries) { country ->
                val isSelected = selected == country
                Card(
                    onClick = { onSelect(country) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) LightTealBg else PureWhite,
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PrimaryGreen else BorderGray,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            country,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryTextDark,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = selected.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = PureWhite,
                disabledContainerColor = BorderGray,
                disabledContentColor = TertiaryGray,
            ),
        ) {
            Text("Next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepSector(
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Regulatory sector",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryTextDark,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick the compliance domain (not only medical devices — food, chemicals, digital, etc.).",
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryTextMedium,
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(SectorCatalog.all) { s ->
                val isSelected = selected == s.key
                Card(
                    onClick = { onSelect(s.key) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) LightTealBg else PureWhite,
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PrimaryGreen else BorderGray,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(s.icon, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            s.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryTextDark,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = selected.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = PureWhite,
                disabledContainerColor = BorderGray,
                disabledContentColor = TertiaryGray,
            ),
        ) {
            Text("Next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepNiches(
    sectorKey: String,
    selected: Set<String>,
    onNext: (Set<String>) -> Unit,
) {
    var current by remember(sectorKey) { mutableStateOf(selected) }
    val niches = remember(sectorKey) { NicheCatalog.forSector(sectorKey) }
    Column(Modifier.fillMaxSize()) {
        Text(
            "Your niches",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryTextDark,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Select 1–5 focus tags inside this sector",
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryTextMedium,
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            niches.forEach { niche ->
                val isSelected = current.contains(niche.promptKey)
                Card(
                    onClick = {
                        current = if (isSelected) current - niche.promptKey
                        else if (current.size < 5) current + niche.promptKey
                        else current
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) LightTealBg else PureWhite,
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PrimaryGreen else BorderGray,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(niche.icon, style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                niche.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryTextDark,
                            )
                            Text(
                                niche.nameEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryTextMedium,
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Filled.CheckCircle, null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onNext(current) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = current.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = PureWhite,
                disabledContainerColor = BorderGray,
                disabledContentColor = TertiaryGray,
            ),
        ) {
            Text(
                "Next (${current.size} selected)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StepRole(roles: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Your role",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryTextDark,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "How do you work with regulations?",
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryTextMedium,
        )
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(roles) { (role, icon) ->
                Card(
                    onClick = { onSelect(role) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, BorderGray),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(icon, style = MaterialTheme.typography.headlineLarge)
                        Text(
                            role,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryTextDark,
                        )
                    }
                }
            }
        }
    }
}
