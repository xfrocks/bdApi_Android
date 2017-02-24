# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/sondh/bin/adt-bundle-mac-x86_64-20130917/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# http://stackoverflow.com/questions/32535346/android-proguard-error-in-android-studio
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**
-dontwarn android.net.**

# https://github.com/bumptech/glide/wiki/Integration-Libraries#generic-proguard
-keep public class * implements com.bumptech.glide.module.GlideModule

#https://www.guardsquare.com/en/proguard/manual/examples#serializable
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
