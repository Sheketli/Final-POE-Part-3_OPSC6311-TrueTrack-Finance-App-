# TrueTrackFinance ProGuard Rules

# Keep Room entities and DAOs (narrowed scope)
-keepclassmembers class com.example.truetrackfinance.data.db.entity.** { *; }
-keep class com.example.truetrackfinance.data.db.dao.** { *; }

# Keep Hilt generated modules
-keep class com.example.truetrackfinance.di.** { *; }

# narrow down MPAndroidChart
-keep class com.github.mikephil.charting.charts.** { *; }
-keep class com.github.mikephil.charting.data.** { *; }
-keep class com.github.mikephil.charting.interfaces.** { *; }

# SQLCipher
-keep class net.sqlcipher.** { *; }

# Lottie
-keep class com.airbnb.lottie.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

# bcrypt
-keep class at.favre.lib.crypto.bcrypt.** { *; }
