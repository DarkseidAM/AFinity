# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Google Cast SDK
-keep class com.google.android.gms.cast.** { *; }
-keep class com.makd.afinity.cast.CastOptionsProvider { *; }

# Dolby Vision JNI bridge: native method + class names must survive R8 so the
# libdovi_bridge.so JNI lookup (resolved by mangled name) still binds.
-keep class com.makd.afinity.player.exoplayer.dovi.DoviBridge {
    native <methods>;
}
-keepclasseswithmembernames class * {
    native <methods>;
}