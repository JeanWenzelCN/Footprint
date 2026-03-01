# ============================
# Footprint ProGuard Rules
# ============================

# --- 1. Keep all app classes (Room, Compose, Models) ---
-keep class com.footprint.** { *; }
-dontwarn kotlinx.coroutines.**

# --- 2. Flutter Engine ---
-keep class io.flutter.** { *; }
-keep class io.flutter.embedding.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.plugins.** { *; }
-dontwarn io.flutter.**

# --- 3. AMap (高德地图 & 定位) SDK ---
-keep class com.amap.api.** { *; }
-keep class com.autonavi.** { *; }
-keep class com.loc.** { *; }
-keep class com.amap.api.maps.** { *; }
-keep class com.amap.api.location.** { *; }
-keep class com.amap.api.fence.** { *; }
-keep class com.amap.api.services.** { *; }
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**

# --- 4. AMap Flutter Plugin ---
-keep class com.amap.flutter.** { *; }
-dontwarn com.amap.flutter.**

# --- 5. Gson (JSON serialization) ---
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.gson.**

# --- 6. Room Database ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# --- 7. AndroidX & Lifecycle ---
-keep class androidx.lifecycle.** { *; }
-keep class androidx.core.** { *; }
-dontwarn androidx.**

# --- 8. Kotlin Coroutines / Serialization ---
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# --- 9. Google Play Services ---
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# --- 10. Coil (Image Loading) ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- 11. permission_handler Flutter plugin ---
-keep class com.baseflow.permissionhandler.** { *; }
-dontwarn com.baseflow.permissionhandler.**

# --- 12. image_picker Flutter plugin ---
-keep class io.flutter.plugins.imagepicker.** { *; }
-dontwarn io.flutter.plugins.imagepicker.**

# --- 13. Prevent stripping of native methods ---
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- 14. Keep Enum classes ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- 15. Keep Parcelable implementations ---
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# --- 16. Keep R class ---
-keep class **.R$* { *; }

# --- 17. Keep Compose ---
-dontwarn androidx.compose.**
