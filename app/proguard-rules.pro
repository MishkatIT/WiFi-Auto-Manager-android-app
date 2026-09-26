# Proguard rules for WiFi Auto Manager

# Domain models & entities (Room & Gson reflection)
-keep class com.example.wifiautomanager.data.local.db.entity.** { *; }
-keep class com.example.wifiautomanager.domain.model.** { *; }
-keep class com.example.wifiautomanager.domain.rule.** { *; }

# WorkManager
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
