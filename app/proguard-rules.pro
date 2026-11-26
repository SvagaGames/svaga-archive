# Svaga Archive: the code uses no reflection (org.json, OkHttp, Compose), so the default rules are enough.
# Readable crash reports in CrashLog:
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# Glance widget receiver is created by the system by name
-keep class com.svaga.archive.widget.** { *; }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
