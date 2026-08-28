# iText 7 (AGPL) — reflection and native font/CMap loading
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Apache POI — StAX via Aalto, reflection-heavy
-keep class org.apache.poi.** { *; }
-keep class com.fasterxml.aalto.** { *; }
-keep class javax.xml.stream.** { *; }
-dontwarn org.apache.poi.**
-dontwarn javax.xml.stream.**

# Sardine (WebDAV)
-keep class com.github.sardine.** { *; }
-dontwarn com.github.sardine.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class cn.loxx.expense.**$$serializer { *; }
-keepclassmembers class cn.loxx.expense.** { *** Companion; }
-keepclasseswithmembers class cn.loxx.expense.** { kotlinx.serialization.KSerializer serializer(...); }
