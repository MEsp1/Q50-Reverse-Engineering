# ============================================================
# ProGuard rules for Q50 IVI app (API 10 / Dalvik 2.3)
#
# Goal: keep DEX under single-dex limit (65K methods),
# strip unused Kotlin stdlib, and preserve sensor callbacks.
# ============================================================

-keepattributes *Annotation*

# --- Main Activity (launcher entry point) ---
-keep public class com.q50dev.hello.MainActivity {
    public void onCreate(android.os.Bundle);
}

# --- SensorEventListener callbacks (invoked by Android framework) ---
-keep class * implements android.hardware.SensorEventListener {
    public void onSensorChanged(android.hardware.SensorEvent);
    public void onAccuracyChanged(android.hardware.Sensor, int);
}

# --- Kotlin metadata (required for reflection if used) ---
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# --- Remove debug logging in release builds ---
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# --- Keep Q50 sensor constants (referenced by type ID) ---
-keep class com.q50dev.hello.Q50Sensors { *; }
-keep class com.q50dev.hello.VehicleData { *; }
