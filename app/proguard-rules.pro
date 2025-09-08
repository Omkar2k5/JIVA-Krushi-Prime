# Core keep rules for Jetpack Compose to avoid R8 verification issues
-keep class androidx.compose.runtime.internal.ComposableLambdaImpl { *; }
-keepclassmembers class ** { @androidx.compose.runtime.Composable <methods>; }
-keep class kotlin.jvm.functions.** { *; }

# Keep the Day End screen and dispatcher screen wrappers
-keep class com.example.jiva.screens.DayEndReportScreenKt { *; }
-keep class com.example.jiva.screens.PlaceholderScreensKt { *; }

# Optional: broadly keep Compose runtime (can be relaxed later)
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin/Coroutines warnings suppression
-dontwarn kotlin.**
-keep class kotlin.coroutines.** { *; }

# Preserve line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable