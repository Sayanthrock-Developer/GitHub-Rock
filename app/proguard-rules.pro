-keepattributes Signature,*Annotation*

# Tink references Error Prone's compile-time-only annotations. They are not
# used at runtime, so R8 may safely ignore their absent annotation classes.
-dontwarn com.google.errorprone.annotations.**
-keep class kotlinx.serialization.** { *; }
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
