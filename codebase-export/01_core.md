# Core module

## core/model/CardModels.kt

```kotlin
package com.example.myapplication2.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Priority {
    CRITICAL,
    HIGH,
    MEDIUM,
}

@Serializable
enum class CardType {
    SEARCH_HISTORY,
    REGULATORY_EVENT,
    INSIGHT,
    STRATEGY,
    LEARNING_MODULE,
    ACTION_ITEM,
}

@Serializable
data class CardLink(
    val title: String,
    val url: String,
    val sourceLabel: String = "",
    val isVerified: Boolean = false,
)

@Serializable
data class SocialPost(
    val platform: String,
    val author: String,
    val text: String,
    val dateMillis: Long,
    val url: String,
)

@Serializable
enum class SearchSectionType {
    QUERY_BRIEFING,
    RELATED_EVENTS,
    EXPERT_ANALYTICS,
    STRATEGIC_FOCUS,
    SOCIAL_DISCUSSION,
}

@Serializable
data class SearchSection(
    val type: SearchSectionType,
    val title: String,
    val content: String,
    val resources: List<String> = emptyList(),
    val links: List<CardLink> = emptyList(),
)

@Serializable
data class DashboardCard(
    val id: String = UUID.randomUUID().toString(),
    val type: CardType,
    val searchQuery: String = "",
    val title: String,
    val subtitle: String,
    val body: String,
    val expertOpinion: String? = null,
    val analytics: String? = null,
    val actionChecklist: List<String> = emptyList(),
    val riskFlags: List<String> = emptyList(),
    val impactAreas: List<String> = emptyList(),
    val confidenceLabel: String = "",
    val urgencyLabel: String = "",
    val links: List<CardLink> = emptyList(),
    val resources: List<String> = emptyList(),
    val socialInsights: List<SocialPost> = emptyList(),
    val detailedSections: List<SearchSection> = emptyList(),
    val niche: String = "",
    val dateMillis: Long,
    val priority: Priority = Priority.MEDIUM,
    val isPinned: Boolean = false,
    val orderIndex: Int = 0,
)
```

## core/common/NicheCatalog.kt

```kotlin
package com.example.myapplication2.core.common

data class Niche(
    val name: String,
    val nameEn: String,
    val promptKey: String,
) {
    override fun equals(other: Any?) = other is Niche && promptKey == other.promptKey
    override fun hashCode() = promptKey.hashCode()
}

object NicheCatalog {
    val all: List<Niche> = listOf(
        Niche("Кардіоваскулярні пристрої", "Cardiovascular Devices", "Cardiovascular Devices (stents, pacemakers, valves)"),
        Niche("Ортопедичні імпланти", "Orthopedic & Trauma Devices", "Orthopedic and Trauma Devices (implants, prosthetics)"),
        Niche("Ін-вітро діагностика (IVDR)", "In Vitro Diagnostics (IVDR)", "In Vitro Diagnostic Devices IVDR"),
        Niche("Програмне забезпечення як медпристрій (SaMD)", "Software as Medical Device (SaMD)", "Software as Medical Device SaMD AI ML"),
        Niche("Активні імплантовані пристрої", "Active Implantable Devices", "Active Implantable Medical Devices AIMD"),
        Niche("Офтальмологічні пристрої", "Ophthalmic Devices", "Ophthalmic Devices (lenses, implants)"),
        Niche("Стоматологічні пристрої", "Dental & Maxillofacial", "Dental and Maxillofacial Devices"),
        Niche("Загальна хірургія та інструменти", "General Surgery Instruments", "General Surgery Instruments"),
        Niche("Догляд за ранами", "Wound Care", "Wound Care and Dressings"),
        Niche("Діагностичне обладнання", "Diagnostic Imaging", "Diagnostic Imaging Equipment"),
        Niche("Інфузійні та ін'єкційні пристрої", "Infusion & Injection", "Infusion Pumps and Injection Devices"),
        Niche("Прилади для домашнього використання", "Home Healthcare", "Home Healthcare Devices"),
        Niche("Custom-made пристрої", "Custom-Made Class III", "Custom-Made Devices Class III"),
        Niche("Legacy пристрої (до MDR)", "Legacy Devices", "Legacy Devices MDR transition"),
        Niche("AI/ML в медичних пристроях", "AI/ML Medical Devices", "AI ML Medical Devices Regulation"),
        Niche("Комбіновані продукти (drug-device)", "Drug-Device Combination", "Drug-Device Combination Products"),
        Niche("Моніторинг пацієнтів", "Patient Monitoring", "Patient Monitoring Systems"),
        Niche("Реабілітаційне обладнання", "Rehabilitation & Physiotherapy", "Rehabilitation and Physiotherapy Devices"),
    )

    fun findByPromptKey(key: String): Niche? = all.firstOrNull { it.promptKey == key }
    fun findByKeyOrName(value: String): Niche? = all.firstOrNull { it.promptKey == value || it.name == value }
}
```

## core/common/JsonProvider.kt

```kotlin
package com.example.myapplication2.core.common

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class JsonProvider @Inject constructor() {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
        isLenient = true
    }
}
```

## core/common/AppDispatchers.kt

```kotlin
package com.example.myapplication2.core.common

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Singleton
class AppDispatchers @Inject constructor() {
    val io: CoroutineDispatcher = Dispatchers.IO
}
```

## core/util/ErrorMessageHelper.kt

```kotlin
package com.example.myapplication2.core.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMessageHelper {
    fun userFriendlyMessage(throwable: Throwable?): String {
        if (throwable == null) return "Щось пішло не так. Спробуйте ще раз."
        return when (throwable) {
            is UnknownHostException -> "Немає підключення до інтернету. Перевірте мережу."
            is SocketTimeoutException -> "Час очікування вийшов. Перевірте інтернет і спробуйте знову."
            is IOException -> "Помилка мережі. Перевірте з'єднання."
            else -> {
                val msg = throwable.message.orEmpty()
                when {
                    msg.contains("401", ignoreCase = true) -> "Помилка авторизації. Перевірте налаштування API."
                    msg.contains("403", ignoreCase = true) -> "Доступ заборонено. Перевірте API ключ."
                    msg.contains("429", ignoreCase = true) -> "Забагато запитів. Зачекайте і спробуйте пізніше."
                    msg.contains("500", ignoreCase = true) || msg.contains("502", ignoreCase = true) || msg.contains("503", ignoreCase = true) -> "Сервер тимчасово недоступний. Спробуйте пізніше."
                    msg.isNotBlank() && msg.length < 120 -> msg
                    else -> "Щось пішло не так. Спробуйте ще раз."
                }
            }
        }
    }
}
```
