# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep model classes
-keep class com.ntu.qidong.digitaltwin.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
