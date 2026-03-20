package com.example.myapplication2.presentation.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.AppSettingsRepository
import com.example.myapplication2.domain.usecase.SaveUserProfileUseCase
import com.example.myapplication2.presentation.components.HeroPanel
import com.example.myapplication2.presentation.components.SectionPanel
import com.example.myapplication2.presentation.components.StatusBadgeRow
import com.example.myapplication2.presentation.navigation.regulationViewModel
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val appSettingsRepository: AppSettingsRepository,
) : ViewModel() {
    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            saveUserProfileUseCase(profile)
            appSettingsRepository.saveSelectedNiches(profile.niches)
        }
    }
}

@Composable
fun OnboardingRoute(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = regulationViewModel(),
) {
    var currentStep by remember { mutableStateOf(0) }
    var role by remember { mutableStateOf("") }
    var customRole by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    val selectedNiches = remember { mutableStateListOf<String>() }
    val stepTitles = remember {
        listOf(
            "Твоя роль",
            "Країна та ринок",
            "Ніші",
        )
    }
    val selectedRole = if (customRole.isNotBlank()) customRole else role

    OnboardingScreen(
        modifier = modifier,
        currentStep = currentStep,
        stepTitles = stepTitles,
        role = role,
        customRole = customRole,
        country = country,
        selectedNiches = selectedNiches,
        onBack = { if (currentStep > 0) currentStep -= 1 },
        onNext = { if (currentStep < stepTitles.lastIndex) currentStep += 1 },
        onRoleChange = { role = it },
        onCustomRoleChange = { customRole = it },
        onCountryChange = { country = it },
        onToggleNiche = { promptKey ->
            if (selectedNiches.contains(promptKey)) {
                selectedNiches.remove(promptKey)
            } else if (selectedNiches.size < 5) {
                selectedNiches.add(promptKey)
            }
        },
        onSave = {
            viewModel.saveProfile(
                UserProfile(
                    role = selectedRole,
                    niches = selectedNiches.toList(),
                    deviceTypes = emptyList(),
                    country = country,
                ),
            )
        },
    )
}

@Composable
private fun OnboardingScreen(
    currentStep: Int,
    stepTitles: List<String>,
    role: String,
    customRole: String,
    country: String,
    selectedNiches: List<String>,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onRoleChange: (String) -> Unit,
    onCustomRoleChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onToggleNiche: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedRole = if (customRole.isNotBlank()) customRole else role
    val canContinue = when (currentStep) {
        0 -> selectedRole.isNotBlank()
        1 -> country.isNotBlank()
        else -> selectedNiches.isNotEmpty()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        HeroPanel(
            title = "Regulation onboarding",
            subtitle = "3 короткі кроки, щоб персоналізувати пошук, календар і intelligence-модулі під твій ринок, роль та ніші.",
        )
        LinearProgressIndicator(
            progress = { (currentStep + 1) / stepTitles.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        StepHeader(
            currentStep = currentStep,
            stepTitles = stepTitles,
        )

        when (currentStep) {
            0 -> RoleStep(
                role = role,
                customRole = customRole,
                onRoleChange = onRoleChange,
                onCustomRoleChange = onCustomRoleChange,
            )

            1 -> CountryStep(
                country = country,
                onCountryChange = onCountryChange,
            )

            else -> NicheStep(
                selectedNiches = selectedNiches,
                onToggleNiche = onToggleNiche,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Назад")
                }
            }

            if (currentStep < stepTitles.lastIndex) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canContinue,
                ) {
                    Text("Далі")
                }
            } else {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canContinue,
                ) {
                    Text("Завершити")
                }
            }
        }
    }
}

@Composable
private fun StepHeader(
    currentStep: Int,
    stepTitles: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Крок ${currentStep + 1} з ${stepTitles.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        StatusBadgeRow(
            items = stepTitles.mapIndexed { index, title ->
                if (index == currentStep) "${index + 1}. $title • current" else "${index + 1}. $title"
            },
        )
    }
}

@Composable
private fun RoleStep(
    role: String,
    customRole: String,
    onRoleChange: (String) -> Unit,
    onCustomRoleChange: (String) -> Unit,
) {
    val suggestedRoles = listOf(
        "RA Manager",
        "QA Manager",
        "Regulatory Specialist",
        "Clinical Affairs",
        "Founder / CEO",
    )

    SectionPanel(
        title = "Оберіть роль",
        subtitle = "Це допоможе адаптувати стиль пошуку, аналітику і стратегії до твого реального сценарію роботи.",
    ) {
        suggestedRoles.forEach { suggestedRole ->
            FilterChip(
                modifier = Modifier.fillMaxWidth(),
                selected = role == suggestedRole,
                onClick = { onRoleChange(suggestedRole) },
                label = { Text(suggestedRole) },
            )
        }
        OutlinedTextField(
            value = customRole,
            onValueChange = onCustomRoleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Або введи свою роль") },
        )
    }
}

@Composable
private fun CountryStep(
    country: String,
    onCountryChange: (String) -> Unit,
) {
    SectionPanel(
        title = "Країна та основний ринок",
        subtitle = "Наприклад: Ukraine, EU, Germany, Poland, UK. Це впливатиме на пошук джерел, подій та регуляторних акцентів.",
    ) {
        OutlinedTextField(
            value = country,
            onValueChange = onCountryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Country / Region / Target market") },
        )
    }
}

@Composable
private fun NicheStep(
    selectedNiches: List<String>,
    onToggleNiche: (String) -> Unit,
) {
    SectionPanel(
        title = "Оберіть точну нішу",
        subtitle = "Це ключовий крок для календаря, пошуку й інсайтів. Можна вибрати від 1 до 5 напрямків.",
    ) {
        NicheCatalog.all.forEach { niche ->
            FilterChip(
                modifier = Modifier.fillMaxWidth(),
                selected = selectedNiches.contains(niche.promptKey),
                onClick = { onToggleNiche(niche.promptKey) },
                label = { Text(niche.name) },
            )
        }
        Text("Обрано: ${selectedNiches.size}/5", style = MaterialTheme.typography.bodySmall)
    }
}
