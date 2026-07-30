# Keep libv2ray JNI classes and methods
-keep class libv2ray.** { *; }
-keepclassmembers class libv2ray.** { *; }

# Keep VLESS VPN models
-keep class com.vless.vpn.vpn.** { *; }

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
