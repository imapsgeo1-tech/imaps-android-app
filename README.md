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

## v1.2.0 animated splash and inquiry UX
- Original logo artwork is separated at runtime into the real Iran outline, golden globe and the exact IMAPS / IRAN MAPS letter arcs. The Iran layer has illustrated topo relief and contour hatching before the complete original mark assembles.
- Professional dark-blue/gold IMAPS WebGIS splash; it finishes after initial page load and short entrance, with offline fallback.
- In-app AJAX inquiry gets a centered rotating logo overlay; auto dismisses after a settled fetch/XHR and has a 22-second failsafe.
- No visible top-loading progress stripe.
- Right-hand native floating button is a single dial-phone icon, WhatsApp remains only in the web map (left).
- Minimal persistent copyright © iMaps 2026 below main WebGIS content.
- Same original survey page 16588, map pinch zoom restrictions and Android Storage Access Framework KMZ picker.

## v1.2.1 UX smoothness and original iMaps mark
- Lightweight 1.65 second native startup reveal, no runtime per-pixel splitting or slow startup loops; page can load behind it; upper safety timeout 5.5 seconds.
- Full complete 300-500 px original brand logo sourced from official WordPress media 1834-removebg-preview-1, same artwork as owner's supplied source logo, to replace prior 160 px resized logo.
- Inquiry loading is now ONLY the original transparent logo rotating in the center. No circle, background box, overlay dim, text, or extra UI. The progress bar remains removed.
- Same call-only action, footer copyright, fixed top header, zoom and KMZ selection behaviour as v1.1.6.

## v1.2.2 Cinematic 3-act transparent intro
Build-time Pillow processing (in scripts/generate_logo_layers.py) separates exact pixel-perfect 500px brand artwork into Iran outline, golden globe and lettering, keeping every pixel intact. An offline GPU-animated native glass search interface types iMaps, transitions into Iran, globe, letters, and WebGIS. The app's website loads during the intro and takes over smoothly; no network video or startup-per-pixel work. Other features unchanged.

## v1.2.3 — Remove duplicate Android inquiry spinner
The WebGIS site now supplies its own inquiry-loading animation on every platform. Remove the obsolete native Android InquiryOverlay, its injected JavaScript bridge, click listeners and separate rotating logo. Users see only the current animation from the website when they tap استعلام. Startup splash, copyright footer, KMZ document picker, map-only zoom, static top logo and phone-only contact remain unchanged.

## v1.2.4 — Remotely controlled required updates (Cafe Bazaar)
The signed APK checks a small public HTTPS policy at https://raw.githubusercontent.com/imapsgeo1-tech/imaps-android-app/main/app-update.json. It compares the installed numeric versionCode with minimum_supported_version_code. When below the minimum, a blocking, non-dismissible branded update screen prevents interaction with the in-app WebGIS and links to the official Cafe Bazaar page. The app periodically rechecks in foreground and caches a recent valid policy to help in temporary connectivity failures. It never silently installs APKs or requests additional permissions.

**Current launch policy is intentionally non-blocking:** latest_version_code=17 (v1.2.4), minimum_supported_version_code=16. Do NOT raise the minimum until a newer, identically signed APK is actually published and downloadable from Cafe Bazaar. For example, after v1.2.5 (versionCode 18) is publicly available, update app-update.json to latest_version_code=18, latest_version_name=1.2.5, minimum_supported_version_code=18 to require that release from v1.2.4 users. Update only the policy JSON: future clients don't need to be rebuilt to change the threshold. If GitHub raw is unreachable and no recent valid policy is cached, the app remains accessible (network-failure safe behavior); test accessibility from Iran.

**Important limitation:** v1.2.3 and earlier shipped without UpdateGate, so those already-installed APKs cannot be forced to update by this new APK's code. This feature applies after users install v1.2.4 or newer. Website users are never affected.

## v1.2.5 — On-demand GPS position and aerial photo year inquiry
The Android WebView app injects `app/src/main/assets/imaps-location.js` into the official HTTPS WebGIS only (no WordPress plugin changes). A compact map-control button labeled «◎ موقعیت من» triggers the device location prompt **only when tapped**. It displays an accuracy circle and position marker, centers Leaflet on the actual GPS location, and (for points inside Iranian bounds) switches to the existing geographic input mode, filling the first longitude and latitude values and clearing stale extra input rows. The user then presses the site's existing «استعلام» button to query archived aerial-photo years; location is not sent for inquiry before that tap. Outside-Iran positions can be viewed on the map but are not submitted to the Iran-only index. GPS denial, disabled location, low accuracy and errors have Persian feedback. The Android permission handler already restricts location access to trusted i-maps.ir HTTPS origins. Native startup, WebGIS loading animation, KMZ upload, zoom, update gate and signing stay untouched. No background location collection.

## v1.2.6 — Fix invisible GPS button and OSM attribution
The former fragile `L.control` injection was removed because custom WebGIS styles/async map timing could leave the button invisible and distort Leaflet control positions. `LocationFeature.java` now displays a NATIVE floating button above the WebView, inside the app content frame. On explicit tap it checks WebGIS readiness, asks for Android location permission, locates using GPS/network providers on the main thread with a bounded 16s timeout, unregisters listeners immediately after a fix, and calls `window.IMAPSAndroidLocation.apply(lat,lon,accuracy)` in the existing WebGIS. The updated JS file only displays a blue marker and accuracy halo and fills the preexisting geographic inquiry inputs; it creates no Leaflet control. Scoped CSS fixes the OpenStreetMap attribution to the lower right of the map at tiny font size. No background geolocation and no changes to the WordPress plugin.
