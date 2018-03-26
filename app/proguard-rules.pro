#######################################################################################
#################################### UPNP PROGUARD ####################################
#######################################################################################


#######################################################################################
##---------------Begin: proguard configuration common for all Android apps ----------##
#######################################################################################

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose
-dump class_files.txt
-printseeds seeds.txt
-printusage unused.txt
-printmapping mapping.txt
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-allowaccessmodification
-keepattributes *Annotation*
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-repackageclasses ''

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
-dontnote com.android.vending.licensing.ILicensingService

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Preserve all native method names and the names of their classes.
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Preserve static fields of inner classes of R classes that might be accessed through introspection.
-keepclassmembers class **.R$* {
  public static <fields>;
}

# Preserve the special static methods that are required in all enumeration classes.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep public class * {
    public protected *;
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
  public *;
}


#######################################################################################
##-------------------Begin: proguard configuration for okhttp3 ----------------------##
#######################################################################################

-keep class com.squareup.okhttp3.** {*;}

-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

-keepattributes Signature
-keepattributes Annotation
-keep class okhttp3.* { ; }
-keep interface okhttp3.* { ; }
-dontwarn okhttp3.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform.**
-dontwarn okhttp3.internal.huc.OkHttpsURLConnection.**
-dontwarn okhttp3.internal.huc.DelegatingHttpsURLConnection.**


#######################################################################################
##----------------Begin: proguard configuration for EventBus ------------------------##
#######################################################################################

-keepattributes Annotation
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
  public *;
}


#######################################################################################
##-------------------Begin: proguard configuration for Picasso ----------------------##
#######################################################################################

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase


#######################################################################################
##------------------Begin: proguard configuration for RxAndroid ---------------------##
#######################################################################################

# Rxjava-promises
-keep class com.darylteo.rx.** { *; }
-dontwarn com.darylteo.rx.**

# RxJava 0.21
-keep class rx.schedulers.Schedulers {
    public static <methods>;
}
-keep class rx.schedulers.ImmediateScheduler {
    public <methods>;
}
-keep class rx.schedulers.TestScheduler {
    public <methods>;
}
-keep class rx.schedulers.Schedulers {
    public static ** test();
}

## Retrolambda specific rules ##
# as per official recommendation: https://github.com/evant/gradle-retrolambda#proguard
-dontwarn java.lang.invoke.*