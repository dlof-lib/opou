# قواعد OPOU لـ ProGuard/R8
-keep class com.OPEN.OU.data.model.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    native <methods>;
}
