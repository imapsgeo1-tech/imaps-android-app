# iMaps Android Application

Android WebView application for the iMaps historical aerial photography years lookup at https://i-maps.ir/سامانه-استعلام-سالهای-عکسبرداری-آیمپس/.

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

## v1.0.2
Full-screen map UI: native header and footer removed. WordPress header/footer suppressed in embedded page while keeping mapping tools. Original 512px logo fetched at build from WordPress media library for launcher icon.

## v1.0.3
Only within Android WebView: hide WordPress Karauos/Elementor site header/footer and use fullscreen status bar, leaving map controls and public website unchanged.

## v1.0.4
Android app only: Goftino resources are blocked inside the WebView and Goftino DOM launchers are hidden. The public i-maps.ir website, its chat plugin and SEO remain untouched.

A compact native floating support button offers:
- WhatsApp: `https://wa.me/989213234340` (opens WhatsApp, or its web landing page if unavailable)
- Telephone: `ACTION_DIAL tel:09213234340` (opens the device dialer; does not place a call automatically).

This version remains a debug-signed build. Separate GitHub Actions debug builds may use different signing keys, so older test versions may need to be uninstalled first.

## v1.1.1
Fix incorrect map/converter landing page in v1.1.0; launch the historical aerial photography survey years lookup instead. Rename app accordingly and use same release signing key/application ID. Publish v1.1.1 to Bazaar instead of v1.1.0.

## v1.1.2
Fix HTTP 404 in aerial photo year lookup: load the verified WordPress page by stable ID `https://i-maps.ir/?page_id=16588` rather than an incorrectly guessed Persian slug. Same app ID and official release signing key as earlier production versions.

## v1.1.3
Disable WebView's built-in page pinch and double-tap zoom. All sidebar and controls stay fixed-size; the embedded interactive map retains its own JavaScript-based touch zoom and the map +/- controls. WordPress remains unchanged. Verify on physical Android device before publishing.

## v1.1.4
Add original user-provided IMAPS Iran Maps complete globe-and-lettering logo as a tiny native badge centered at top of the in-app survey map. Keep the full-screen site content, hidden WordPress header/footer, fixed UI zoom and JavaScript map zoom unchanged. Image is a small palette-optimized rendition of the original logo, included locally and usable without a separate image request. The website is unchanged.

## v1.1.5
Fix the logo obscuring map controls or title: render the unchanged IMAPS logo in a reserved 78dp native white header row above (outside) the WebView. Keep all map UI below this row with its own full remaining layout space. No floating logo or overlay; no added text or menu. Same package and official signing key; unchanged website, map zoom and contact tools.

## v1.1.6
Fix selecting KMZ files from an Android phone: replace the MIME-filtered WebChromeClient default picker with Android's Storage Access Framework ACTION_OPEN_DOCUMENT, CATEGORY_OPENABLE, and wildcard MIME type (*/*), with temporary read access to the chosen content URI. This is necessary because some KMZ files report application/octet-stream and were hidden by Android's MIME filters. Preserve support for other survey input types and multi-selection if the webpage requests it. No blanket storage permission, website changes, app branding changes, or map zoom changes. Use the same production signing key/application ID.
