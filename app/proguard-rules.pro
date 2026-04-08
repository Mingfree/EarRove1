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

# 核心保护
-keep class com.baidu.mapapi.** { *; }
-keep class com.baidu.location.** { *; }
-keep class com.baidu.platform.** { *; }

# JNI相关
-keepclasseswithmembernames class * {
    native <methods>;
}

# 必要的基础保护
-keepattributes *Annotation*, Signature
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 其他百度类使用轻量保护
-keepnames class com.baidu.** { *; }