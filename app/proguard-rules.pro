# Add project specific ProGuard rules here.
# https://developer.android.com/guide/developing/tools/proguard.html

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit / OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Serialization (R8 + kotlinx.serialization generated $$serializer / Companion)
# https://github.com/Kotlin/kotlinx.serialization/blob/master/README.md#android
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclasseswithmembers class **$$serializer { *; }
-keepclassmembers class **$$serializer {
    *** INSTANCE;
}
-keepclasseswithmembers class **$Companion {
    ** serializer(...);
}
# App package: JSON models + API DTOs (internal @Serializable in GrokApi/OpenAiApi too)
-keep,includedescriptorclasses class com.example.myapplication2.**$$serializer { *; }
-keepclassmembers class com.example.myapplication2.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.example.myapplication2.domain.model.UserProfile { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Data classes used by serialization (keep for JSON)
-keepclassmembers class com.example.myapplication2.core.model.** { *; }
-keepclassmembers class com.example.myapplication2.domain.model.** { *; }

# WorkManager — CoroutineWorker instantiated via reflection
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    <init>(android.content.Context,androidx.work.WorkerParameters);
}