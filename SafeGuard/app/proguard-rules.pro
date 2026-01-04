# SafeGuard ProGuard Rules - Production Ready

# ==================== GENERAL ====================
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepattributes SourceFile,LineNumberTable  # For crash reports
-renamesourcefileattribute SourceFile

# ==================== KOTLIN ====================
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin serialization
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.safeguard.app.**$$serializer { *; }
-keepclassmembers class com.safeguard.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.safeguard.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ==================== DATA MODELS ====================
-keep class com.safeguard.app.data.models.** { *; }
-keepclassmembers class com.safeguard.app.data.models.** { *; }

# ==================== ROOM DATABASE ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**
-keep class com.safeguard.app.data.local.** { *; }

# ==================== COMPOSE ====================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ==================== FIREBASE ====================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# ==================== GOOGLE MAPS & PLACES ====================
-keep class com.google.android.libraries.places.** { *; }
-keep class com.google.maps.** { *; }
-dontwarn com.google.maps.**

# ==================== SERVICES & RECEIVERS ====================
-keep class com.safeguard.app.services.** { *; }
-keep class com.safeguard.app.receivers.** { *; }
-keep class com.safeguard.app.widgets.** { *; }

# ==================== CORE MANAGERS ====================
-keep class com.safeguard.app.core.** { *; }

# ==================== COIL IMAGE LOADING ====================
-dontwarn coil.**
-keep class coil.** { *; }

# ==================== COROUTINES ====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ==================== OKHTTP (used by some libs) ====================
-dontwarn okhttp3.**
-dontwarn okio.**

# ==================== REMOVE LOGGING IN RELEASE ====================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}