package com.example.myapplication2.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.myapplication2.BuildConfig
import com.example.myapplication2.core.common.JsonProvider
import com.example.myapplication2.data.local.AppDatabase
import com.example.myapplication2.data.remote.GrokApi
import com.example.myapplication2.data.repository.AppSettingsRepositoryImpl
import com.example.myapplication2.data.repository.CacheRepositoryImpl
import com.example.myapplication2.data.repository.CalendarRepositoryImpl
import com.example.myapplication2.data.repository.CardRepositoryImpl
import com.example.myapplication2.data.repository.GenerationRepositoryImpl
import com.example.myapplication2.data.repository.InsightsRepositoryImpl
import com.example.myapplication2.data.repository.LearningRepositoryImpl
import com.example.myapplication2.data.repository.LinkValidator
import com.example.myapplication2.data.repository.NotificationRepositoryImpl
import com.example.myapplication2.data.repository.SearchRepositoryImpl
import com.example.myapplication2.data.repository.SeedContentFactory
import com.example.myapplication2.data.repository.StrategyRepositoryImpl
import com.example.myapplication2.data.repository.UserProfileRepositoryImpl
import com.example.myapplication2.domain.usecase.ExpertSearchUseCase
import com.example.myapplication2.domain.usecase.GenerateInsightsUseCase
import com.example.myapplication2.domain.usecase.GenerateRegulatoryCalendarUseCase
import com.example.myapplication2.domain.usecase.GenerateStrategiesUseCase
import com.example.myapplication2.domain.usecase.LoadLearningModulesUseCase
import com.example.myapplication2.domain.usecase.ObserveCardUseCase
import com.example.myapplication2.domain.usecase.ObservePinnedCardsUseCase
import com.example.myapplication2.domain.usecase.ObserveSearchHistoryUseCase
import com.example.myapplication2.domain.usecase.ObserveUserProfileUseCase
import com.example.myapplication2.domain.usecase.PinCardUseCase
import com.example.myapplication2.domain.usecase.RefreshCalendarUseCase
import com.example.myapplication2.domain.usecase.ReorderPinnedCardsUseCase
import com.example.myapplication2.domain.usecase.SaveUserProfileUseCase
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val jsonProvider = JsonProvider()
    val json: Json = jsonProvider.json

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { appContext.preferencesDataStoreFile("regulation_settings") },
    )

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "regulation.db",
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(240, TimeUnit.SECONDS)
        .readTimeout(240, TimeUnit.SECONDS)
        .callTimeout(300, TimeUnit.SECONDS)
        .addInterceptor(
            Interceptor { chain ->
                val builder = chain.request().newBuilder()
                if (BuildConfig.GROK_API_KEY.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer ${BuildConfig.GROK_API_KEY}")
                }
                builder.addHeader("Content-Type", "application/json")
                chain.proceed(builder.build())
            },
        )
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .build()

    private val grokApi: GrokApi = Retrofit.Builder()
        .baseUrl("https://api.x.ai/v1/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GrokApi::class.java)

    private val seedContentFactory = SeedContentFactory()
    private val linkValidator = LinkValidator(okHttpClient)

    private val cardRepository = CardRepositoryImpl(database.cardDao(), jsonProvider)
    private val userProfileRepository = UserProfileRepositoryImpl(dataStore, jsonProvider)
    private val appSettingsRepository = AppSettingsRepositoryImpl(dataStore, jsonProvider)
    private val generationRepository = GenerationRepositoryImpl(dataStore, jsonProvider)
    private val cacheRepository = CacheRepositoryImpl(database.cacheDao())
    private val searchRepository = SearchRepositoryImpl(grokApi, jsonProvider, seedContentFactory, linkValidator, cacheRepository)
    private val calendarRepository = CalendarRepositoryImpl(grokApi, jsonProvider, seedContentFactory, linkValidator, cacheRepository)
    private val insightsRepository = InsightsRepositoryImpl(seedContentFactory)
    private val strategyRepository = StrategyRepositoryImpl(seedContentFactory)
    private val learningRepository = LearningRepositoryImpl(seedContentFactory)
    private val notificationRepository = NotificationRepositoryImpl(appContext)

    val observePinnedCardsUseCase = ObservePinnedCardsUseCase(cardRepository)
    val observeSearchHistoryUseCase = ObserveSearchHistoryUseCase(cardRepository)
    val observeCardUseCase = ObserveCardUseCase(cardRepository)
    val observeUserProfileUseCase = ObserveUserProfileUseCase(userProfileRepository)
    val saveUserProfileUseCase = SaveUserProfileUseCase(userProfileRepository)
    val pinCardUseCase = PinCardUseCase(cardRepository)
    val reorderPinnedCardsUseCase = ReorderPinnedCardsUseCase(cardRepository)
    val expertSearchUseCase = ExpertSearchUseCase(searchRepository, cardRepository, generationRepository)
    val generateRegulatoryCalendarUseCase = GenerateRegulatoryCalendarUseCase(
        calendarRepository,
        cardRepository,
        generationRepository,
        cacheRepository,
        json,
    )
    val generateInsightsUseCase = GenerateInsightsUseCase(insightsRepository, cardRepository, generationRepository)
    val generateStrategiesUseCase = GenerateStrategiesUseCase(strategyRepository, cardRepository, generationRepository)
    val loadLearningModulesUseCase = LoadLearningModulesUseCase(
        learningRepository,
        cardRepository,
        generationRepository,
    )
    val refreshCalendarUseCase = RefreshCalendarUseCase(
        appSettingsRepository,
        userProfileRepository,
        generateRegulatoryCalendarUseCase,
        notificationRepository,
    )

    val userProfileRepo = userProfileRepository
    val appSettingsRepo = appSettingsRepository
    val cardRepo = cardRepository
    val cacheRepo = cacheRepository
}
