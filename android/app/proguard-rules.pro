# XingClass release shrinking rules.

-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod

# kotlinx.serialization: keep generated serializers and companion serializer() lookups.
-keep,includedescriptorclasses class com.xingclass.app.**$$serializer { *; }
-keepclassmembers class com.xingclass.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.xingclass.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Okio optional platform integrations.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.apache.harmony.**

# Tink (used by androidx.security-crypto) references Error Prone annotations that are
# compile-time only and intentionally not packaged in the APK.
-dontwarn com.google.errorprone.annotations.**
