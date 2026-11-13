# ApexGrid Proguard Rules

# Annotations & Attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep @kotlinx.serialization.Serializable class ** { *; }
-keepclassmembers class * {
    static ** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepnames class * implements kotlinx.serialization.internal.GeneratedSerializer {
    *;
}
-keepclassmembers class * implements kotlinx.serialization.internal.GeneratedSerializer {
    *;
}
-keepclassmembers class **$$serializer {
    *;
}

# ApexGrid Domain & Remote Models
-keep class com.reva2473.f1widget.domain.model.** { *; }
-keep class com.reva2473.f1widget.data.remote.** { *; }
-keep class com.reva2473.f1widget.data.local.** { *; }

# Retrofit 2 & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jetpack Glance Widgets & AppWidgets
-keep class androidx.glance.** { *; }
-keep class androidx.glance.appwidget.** { *; }
-keep class com.reva2473.f1widget.widget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
