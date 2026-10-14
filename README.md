# iMaps Android Application

Android WebView application for the iMaps Online Map at https://i-maps.ir.

- Native Android application, Java 17, Android Gradle Plugin 8.7.3
- Android 6.0+ (API 23)
- WebView with JavaScript and map location access (requires user permission)
- File picker, share, reload, external browser, downloads, fullscreen
- GitHub Actions produces a signed **debug** APK and verifies with apksigner

## دریافت APK

Open **Actions → Build iMaps Android APK → latest successful run → Artifacts → iMaps-Android-APK**.
Download the artifact ZIP; inside it is `app-debug.apk`. Extract and install on Android.

This is a debug-signed APK intended for testing. Production release signing requires a private keystore.
The actual interactive map content is loaded from the website and requires internet access.
