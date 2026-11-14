# ApexGrid Proguard Rules

# Annotations & Attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

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

# Retrofit Service Interfaces (relies on Retrofit/OkHttp consumer ProGuard rules)
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep interface com.reva2473.f1widget.data.remote.*ApiService { *; }

# Jetpack Glance AppWidgets (relies on Glance consumer ProGuard rules)
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
