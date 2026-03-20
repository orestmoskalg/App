package com.example.myapplication2.data.repository

import com.example.myapplication2.core.common.NicheCatalog
import com.example.myapplication2.core.model.CardLink
import com.example.myapplication2.core.model.CardType
import com.example.myapplication2.core.model.DashboardCard
import com.example.myapplication2.core.model.Priority
import com.example.myapplication2.core.model.SearchSection
import com.example.myapplication2.core.model.SearchSectionType
import com.example.myapplication2.core.model.SocialPost
import com.example.myapplication2.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedContentFactory @Inject constructor() {

    fun outOfScopeSearchCard(query: String, userProfile: UserProfile): DashboardCard {
        val roleLabel = userProfile.role.ifBlank { "RA / QA" }
        val niche = NicheCatalog.findByPromptKey(userProfile.niches.firstOrNull().orEmpty())?.name
            ?: userProfile.niches.firstOrNull().orEmpty()
        return DashboardCard(
            type = CardType.SEARCH_HISTORY,
            title = query.ifBlank { "Питання не по ніші" },
            subtitle = "Тема поза фокусом цього модуля.",
            body = "Запит \"$query\" виходить за межі regulatory/compliance тем, на яких спеціалізується цей модуль. Якщо намір все ж регуляторний, переформулюйте питання через вимогу, статтю, MDCG, дедлайн, технічну документацію, PMS, QMS, UDI, EUDAMED, notified body або вплив на ваш продукт і роль.",
            expertOpinion = "Цей модуль створений для того, щоб автоматизовано скорочувати час пошуку регуляторної інформації: давати аналітику, вижимку ресурсів, список потрібних дій і професійні соціальні коментарі по темі. Якщо запит не про regulatory/compliance domain, результат буде неточним, тому краще одразу перевести його в регуляторний формат.",
            analytics = "Для ролі $roleLabel це означає, що Smart Search найкраще працює тоді, коли користувач питає про статті, annexes, MDCG, deadlines, technical documentation, PMS, QMS, UDI, vigilance, device class, notified body expectations або practical impact на свій продукт.",
            actionChecklist = listOf(
                "[Середнє] Переформулюйте запит через конкретну regulation або guidance.",
                "[Середнє] Додайте роль, клас девайсу, нішу або тип документа для персоналізації.",
                "[Середнє] Уточніть, чи вам потрібні зміни, дедлайни, дії, ризики або джерела.",
            ),
            riskFlags = listOf(
                "Це питання не по ніші Smart Search.",
                "Без regulatory context модуль не дасть корисну аналітику або точні ресурси.",
            ),
            impactAreas = listOf(
                "Smart Search працює для regulatory/compliance тем у будь-яких нішах.",
                "Найкраще заходять питання про вимоги, дедлайни, guidance, ризики, дії та джерела.",
                "Для нерелевантного запиту модуль підкаже, як сформулювати тему ближче до regulatory research.",
            ),
            confidenceLabel = "Поза темою",
            urgencyLabel = "Уточнити запит",
            links = listOf(
                CardLink("EUR-Lex", "https://eur-lex.europa.eu/", "Official regulation source", true),
                CardLink("European Commission Medical Devices", "https://health.ec.europa.eu/medical-devices-sector_en", "Official guidance source", true),
                CardLink("MDCG hub", "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en", "Guidance hub", true),
            ),
            resources = listOf(
                "Що змінилось у MDR 2025 для мого класу девайсу?",
                "Які technical documentation requirements для мого продукту?",
                "Що я маю зробити як $roleLabel по MDCG або дедлайнах?",
            ),
            niche = niche,
            dateMillis = System.currentTimeMillis(),
            priority = Priority.MEDIUM,
        )
    }

    fun searchCard(query: String, userProfile: UserProfile): DashboardCard {
        val niche = NicheCatalog.findByPromptKey(userProfile.niches.firstOrNull().orEmpty())?.name
            ?: userProfile.niches.firstOrNull().orEmpty()
        val roleLabel = userProfile.role.ifBlank { "RA / QA" }
        return DashboardCard(
            type = CardType.SEARCH_HISTORY,
            title = query,
            subtitle = "Швидкий орієнтир для ролі $roleLabel${niche.takeIf { it.isNotBlank() }?.let { " у ніші $it" } ?: ""}.",
            body = "Це стартовий огляд по запиту \"$query\": він допомагає швидко зорієнтуватися, де шукати інформацію, які джерела відкрити першими і що перевірити додатково. Для вашої ролі $roleLabel важливо окремо звірити точні статті, дедлайни, MDCG та актуальні посилання в EUR-Lex, на сайті European Commission або в EUDAMED/NANDO. Найбільша цінність цього результату зараз - швидко перейти до плану перевірки і аудиту по темі.",
            expertOpinion = "Почніть із точного формулювання самого питання і зафіксуйте, який саме regulatory object ви аналізуєте: статтю, annex, guidance, lifecycle step, evidence expectation або workflow requirement. Далі звірте офіційний текст регламенту, релевантні MDCG та практичні коментарі, але не змішуйте interpretive guidance з обов'язковою нормою. Найкраща практика - оформити одну коротку internal position note для RA, QA та технічної команди, де буде чітко вказано: що саме змінилось, які документи зачеплені, які припущення ще непідтверджені і що треба ескалювати. Якщо тема торкається claims, classification logic, CER/PER, PMCF/PMPF або GSPR, її краще одразу вести як change-control issue. Для RA-команди це також означає потребу перевірити, чи не змінюється логіка підготовки до NB review, PMS argumentation або submission sequence. Практично добре працює підхід, коли ви будуєте owner matrix, список affected deliverables і timeline з трьома горизонтами: критично, до 90 днів і середньостроково. Якщо є неоднозначність, готуйте не одне загальне питання, а пакет точних escalation questions для notified body або external expert.",
            analytics = "Для вашої ролі $roleLabel це означає, що тема може вплинути не лише на reading notes, а й на структуру технічної документації, claims logic, PMS / vigilance narrative, внутрішню change-control дисципліну та готовність до external review. Для ніші $niche це особливо важливо, якщо у вас уже є відкриті зміни, evidence refresh, NB interactions або майбутні submission milestones. На практиці це створює одразу кілька рівнів впливу: нормативний, документарний, міжфункціональний і таймінговий. Найбільший ризик зазвичай не в самому тексті вимоги, а в тому, що команда пізно виявляє, які саме deliverables і рішення реально треба переглянути.",
            actionChecklist = listOf(
                "[Критичне до 30 днів] Повторити пошук при доступному API або звірити тему по офіційних джерелах.",
                "[Критичне до 30 днів] Перевірити всі дати, статті, annexes і MDCG безпосередньо в EUR-Lex або на сайті European Commission.",
                "[Критичне до 30 днів] Зробити impact assessment по продуктах, документах і ринках.",
                "[Критичне до 30 днів] Перевірити, чи зачіпає тема Technical Documentation, GSPR або CER/PER.",
                "[Критичне до 30 днів] Виділити конкретні статті, annexes, MDCG або стандарти, що відповідають саме на цей запит.",
                "[Критичне до 30 днів] Скласти short list affected documents, owners і approval path.",
                "[Високе до 90 днів] Зіставити тему з актуальними SOP, templates і change-control записами.",
                "[Високе до 90 днів] Оцінити вплив на PMS, vigilance, PMCF/PMPF або submission planning.",
                "[Високе до 90 днів] Перевірити, чи змінюються evidence thresholds, claims wording або review expectations.",
                "[Високе до 90 днів] Зібрати список assumptions, які ще не підтверджені офіційними джерелами.",
                "[Середнє] Призначити owner та reviewer і зафіксувати internal interpretation.",
                "[Середнє] Зібрати unresolved questions для external expert або notified body.",
                "[Середнє] Підготувати короткий memo для RA/QA management з ризиками і рекомендованими діями.",
                "[Середнє] Визначити follow-up triggers для повторної перевірки теми через нові guidance або comments.",
            ),
            riskFlags = listOf(
                "Орієнтовна відповідь може не охоплювати всі деталі щодо дедлайнів, статей чи guidance references.",
                "Перед остаточним compliance-рішенням варто звірити висновок по офіційних джерелах.",
                "Затримка оновлення Technical Documentation може зірвати внутрішній review cycle.",
                "Несвоєчасний перегляд claims або labeling може створити CE-ризик.",
                "Неузгодженість між RA, QA і clinical командою підвищує ризик audit findings.",
                "Пізня ескалація питання до notified body може затримати submission або review.",
                "Слабка доказова база під нову інтерпретацію вимог може призвести до major nonconformity.",
                "Помилкова інтерпретація guidance як обов'язкової норми може дати неправильний action path.",
                "Невиявлені залежності в CER/PER, PMS або change-control можуть спричинити повторну роботу.",
                "Запізніла фіксація owner accountability часто створює gap між RA і QA execution.",
            ),
            impactAreas = listOf(
                "Це стартовий результат: використовуйте його як орієнтир, щоб швидко перейти до правильних джерел і перевірок.",
                "Точні дати, статті та document-level sources потрібно перевірити в офіційних джерелах.",
                "Тема напряму зачіпає Technical Documentation і аргументацію відповідності.",
                "Потрібно перевірити, чи змінюються evidence expectations або claims logic.",
                "Є ризик впливу на PMS / vigilance та кросфункціональне погодження.",
                "Важливо оцінити таймінг для NB review, audits або submission planning.",
                "Потрібно чітко відокремити binding requirement від expert interpretation.",
                "Є потенційний вплив на owner matrix, escalation logic і change-control process.",
                "Може знадобитися перегляд CER/PER, PMCF/PMPF або performance evidence strategy.",
                "Тема може впливати на planning для submissions, variations або readiness for review.",
            ),
            confidenceLabel = "Орієнтовна відповідь",
            urgencyLabel = "Звірити в джерелах",
            links = listOf(
                CardLink("EUR-Lex", "https://eur-lex.europa.eu/", "Official regulation source", true),
                CardLink("European Commission", "https://health.ec.europa.eu/medical-devices-sector_en", "Official guidance source", true),
                CardLink("MDCG publications", "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en", "Guidance hub", true),
                CardLink("Team-NB", "https://www.team-nb.org/", "Notified body context", true),
                CardLink("NANDO", "https://webgate.ec.europa.eu/single-market-compliance-space/notified-bodies", "Notified body database", true),
                CardLink("ISO overview", "https://www.iso.org/home.html", "Standards context", true),
            ),
            resources = listOf(
                "Які саме розділи Technical Documentation потрібно оновити першими?",
                "Чи впливає це на CER/PER, PMCF/PMPF або PMS plan?",
                "Що варто ескалювати до notified body або external expert?",
            ),
            socialInsights = listOf(
                SocialPost(
                    platform = "LinkedIn",
                    author = "Regulatory Affairs Network",
                    text = "Teams in $niche are prioritizing documented impact assessments and change-control discipline.",
                    dateMillis = System.currentTimeMillis(),
                    url = "https://www.linkedin.com/",
                ),
                SocialPost(
                    platform = "X",
                    author = "MedTech Regulatory Watch",
                    text = "The strongest teams treat new regulatory interpretation signals as workflow triggers, not just reading material.",
                    dateMillis = System.currentTimeMillis(),
                    url = "https://x.com/",
                ),
                SocialPost(
                    platform = "LinkedIn",
                    author = "QA Lead Europe",
                    text = "Найчастіший коментар команд: найбільший ризик не в самій вимозі, а в запізнілому оновленні Technical Documentation.",
                    dateMillis = System.currentTimeMillis(),
                    url = "https://www.linkedin.com/",
                ),
                SocialPost(
                    platform = "X",
                    author = "NB Review Observer",
                    text = "У коментарях до теми регулярно звучить, що notified body очікує чіткішої логіки impact assessment та owner accountability.",
                    dateMillis = System.currentTimeMillis(),
                    url = "https://x.com/",
                ),
            ),
            detailedSections = listOf(
                SearchSection(
                    type = SearchSectionType.QUERY_BRIEFING,
                    title = "Регуляторний бекграунд",
                    content = "Для теми \"$query\" потрібно звірити релевантні статті MDR/IVDR, відповідні annex sections, актуальні MDCG guidance documents і дату набрання чинності або дату застосування конкретних вимог. У практичному сенсі це означає, що команда повинна відокремити юридично обов'язковий текст від interpretive guidance і зафіксувати, які саме норми реально застосовуються до вашого продукту та ролі.",
                    resources = listOf("Виділити статті та annexes.", "Перевірити чинні MDCG.", "Зафіксувати effective dates."),
                    links = listOf(CardLink("Medical devices sector", "https://health.ec.europa.eu/medical-devices-sector_en", "Commission", true), CardLink("MDCG hub", "https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en", "Guidance hub", true)),
                ),
                SearchSection(
                    type = SearchSectionType.RELATED_EVENTS,
                    title = "Вплив на вашу ситуацію",
                    content = "Для вашої ролі $roleLabel${niche.takeIf { it.isNotBlank() }?.let { " у ніші $it" } ?: ""} це означає потребу швидко зрозуміти, які документи, claims, evidence packages та внутрішні процеси треба перевірити першими. Якщо тема стосується classification logic, GSPR, CER/PER або PMS, вона може створити не лише regulatory workload, а й затримки в погодженні змін, submission planning і внутрішньому approval process.",
                    resources = listOf("Звірити affected docs.", "Оцінити вплив на claims.", "Погодити owner matrix."),
                    links = listOf(CardLink("EUR-Lex timeline", "https://eur-lex.europa.eu/", "Legal timeline", true), CardLink("Commission events", "https://health.ec.europa.eu/medical-devices-sector_en", "Official updates", true)),
                ),
                SearchSection(
                    type = SearchSectionType.EXPERT_ANALYTICS,
                    title = "Рекомендації експерта",
                    content = "Найкращий підхід - почати з impact assessment і одразу перевести тему у список контрольованих дій з owners, документами та термінами. Якщо бачите потенційний вплив на technical file narrative, evidence expectations або NB interaction, краще готувати короткий internal position paper і окремо фіксувати питання, де потрібна зовнішня експертна валідація. Такий підхід зменшує ризик хаотичних правок і повторної роботи перед аудитом або review.",
                    resources = listOf("Створити internal memo.", "Зібрати external questions.", "Оновити action log."),
                ),
                SearchSection(
                    type = SearchSectionType.STRATEGIC_FOCUS,
                    title = "Наступні кроки та запитання",
                    content = "Після первинного огляду теми варто визначити три речі: які документи потрібно переглянути першими, що ескалювати на рівень RA/QA management і які питання вимагають зовнішнього підтвердження. Найкраще працює короткий action path: перевірити affected deliverables, сформувати перелік конкретних питань і запланувати follow-up review з командою.",
                    resources = listOf("Що оновлюємо першим?", "Що ескалюємо до NB?", "Які дані ще потрібні?"),
                ),
                SearchSection(
                    type = SearchSectionType.SOCIAL_DISCUSSION,
                    title = "Обговорення в соціальних мережах",
                    content = "У професійних обговореннях по цій темі найчастіше повторюються коментарі про складність практичної інтерпретації, очікування notified body, неясність щодо evidence thresholds та потребу швидко переводити тему у change-control workflow. Для користувача це корисно тим, що показує не лише офіційну норму, а й реальні pain points команд, які вже намагаються її застосувати.",
                    resources = listOf("Типові питання від RA-команд.", "Повторювані зауваження про Technical Documentation.", "Сигнали щодо NB expectations."),
                    links = listOf(
                        CardLink("LinkedIn discussion", "https://www.linkedin.com/", "Professional discussion", true),
                        CardLink("X discussion", "https://x.com/", "Fast practitioner comments", true),
                    ),
                ),
            ),
            niche = niche,
            dateMillis = System.currentTimeMillis(),
            priority = Priority.HIGH,
        )
    }

    fun calendarCards(niches: List<String>): List<DashboardCard> {
        val now = System.currentTimeMillis()
        val oneYearMs = 365L * 24 * 60 * 60 * 1000
        val eventTitles = listOf(
            "EUDAMED mandatory registration deadline",
            "MDCG guidance update",
            "Legacy device transition deadline",
            "Custom-made Class III compliance",
            "Technical documentation review",
            "PMS plan update required",
            "PMCF data submission",
            "Notified body surveillance audit",
            "MDR Article 120 transition",
            "IVDR implementation milestone",
            "Harmonised standard update",
            "Clinical evaluation report update",
            "UDI compliance deadline",
            "NANDO notified body designation",
            "Commission implementing act",
            "Vigilance reporting requirement",
            "IMDRF alignment update",
            "QMS MDR/IVDR alignment",
            "GSPR gap assessment",
            "Annex VIII classification change",
        )
        return (0 until 50).map { index ->
            val promptKey = niches.getOrNull(index % niches.size).orEmpty()
            val nicheLabel = NicheCatalog.findByPromptKey(promptKey)?.name ?: promptKey
            val title = eventTitles[index % eventTitles.size] + " ($nicheLabel)"
            val dayOffset = -365 + (index * 1460 / 49)
            val dateMillis = now + dayOffset * 86_400_000L
            DashboardCard(
                type = CardType.REGULATORY_EVENT,
                title = title,
                subtitle = "Regulatory event",
                body = "Upcoming milestone, guidance revision, or stakeholder activity relevant to $nicheLabel.",
                expertOpinion = "Focus on evidence gaps and supplier dependencies before the deadline becomes critical.",
                analytics = "This niche is likely to create downstream workload in quality documentation and market surveillance.",
                links = listOf(
                    CardLink("Official sources", "https://health.ec.europa.eu/medical-devices-sector_en"),
                    CardLink("EUR-Lex", "https://eur-lex.europa.eu/"),
                ),
                resources = listOf("Prepare internal review meeting.", "Check whether claim language must be updated."),
                niche = nicheLabel,
                dateMillis = dateMillis,
                priority = when (index % 3) {
                    0 -> Priority.CRITICAL
                    1 -> Priority.HIGH
                    else -> Priority.MEDIUM
                },
            )
        }
    }

    fun insightCards(userProfile: UserProfile): List<DashboardCard> {
        val niche = NicheCatalog.findByPromptKey(userProfile.niches.firstOrNull().orEmpty())?.name
            ?: userProfile.niches.firstOrNull().orEmpty()
        return listOf(
            DashboardCard(
                type = CardType.INSIGHT,
                title = "Critical alert for $niche",
                subtitle = "Immediate regulatory impact",
                body = "A new interpretation trend may affect evidence expectations and change-control planning.",
                expertOpinion = "Treat this as a cross-functional issue: RA, QA, clinical, and product teams should align on the response owner.",
                analytics = "Expected impact score is high because review delays can cascade into submission timing.",
                links = listOf(CardLink("Commission updates", "https://health.ec.europa.eu/medical-devices-sector_en")),
                resources = listOf("Draft impact assessment.", "Review open CAPAs."),
                niche = niche,
                dateMillis = System.currentTimeMillis(),
                priority = Priority.CRITICAL,
            ),
            DashboardCard(
                type = CardType.ACTION_ITEM,
                title = "Update compliance checklist",
                subtitle = "Action item",
                body = "Create an updated checklist for documents and owners impacted by the latest guidance.",
                expertOpinion = "Small teams should assign one owner and one reviewer to avoid silent gaps.",
                analytics = "Fastest win is to audit current technical file sections against the new interpretation.",
                niche = niche,
                dateMillis = System.currentTimeMillis(),
                priority = Priority.HIGH,
            ),
        )
    }

    fun strategyCards(userProfile: UserProfile): List<DashboardCard> {
        val niche = NicheCatalog.findByPromptKey(userProfile.niches.firstOrNull().orEmpty())?.name
            ?: userProfile.niches.firstOrNull().orEmpty()
        return List(3) { index ->
            DashboardCard(
                type = CardType.STRATEGY,
                title = "Compliance strategy ${index + 1}",
                subtitle = "Strategic path for $niche",
                body = "A practical roadmap covering documentation, evidence, supplier controls, and submission sequencing.",
                expertOpinion = "Use this strategy when you need predictable execution rather than the fastest possible release path.",
                analytics = "Balances risk reduction and operational load across RA/QA teams.",
                links = listOf(CardLink("MDR overview", "https://eur-lex.europa.eu/")),
                resources = listOf("Gap assessment", "Owner matrix", "Submission sequence"),
                niche = niche,
                dateMillis = System.currentTimeMillis() - index * 86_400_000L,
                priority = Priority.MEDIUM,
            )
        }
    }

    fun learningCards(userProfile: UserProfile): List<DashboardCard> {
        val niche = NicheCatalog.findByPromptKey(userProfile.niches.firstOrNull().orEmpty())?.name
            ?: userProfile.niches.firstOrNull().orEmpty()
        return List(4) { index ->
            DashboardCard(
                type = CardType.LEARNING_MODULE,
                title = "Learning module ${index + 1}",
                subtitle = "Training for $niche",
                body = "Focused learning card with practical examples, resource links, and what to watch in audits or submissions.",
                expertOpinion = "Teams learn faster when every module ends with one operational check they can perform immediately.",
                analytics = "Useful for onboarding RA/QA specialists and keeping change awareness current.",
                links = listOf(CardLink("Training resource", "https://health.ec.europa.eu/medical-devices-sector_en")),
                resources = listOf("Key takeaways", "Quiz points", "Audit prep notes"),
                niche = niche,
                dateMillis = System.currentTimeMillis() - index * 43_200_000L,
                priority = Priority.MEDIUM,
            )
        }
    }
}
