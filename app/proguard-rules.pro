# Add project specific ProGuard rules here.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keepclassmembers class * {
    public <init>(...);
}
