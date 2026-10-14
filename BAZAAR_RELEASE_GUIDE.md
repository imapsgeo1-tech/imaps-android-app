# انتشار نسخه رسمی آیمپس در کافه‌بازار

This repo contains only source code. The signing key is NEVER committed.

## First-time signing setup
1. Store the private signing keystore and its recovery information offline.
2. Open GitHub repository Settings → Secrets and variables → Actions → New repository secret.
3. Name: `IMAPS_RELEASE_KEY_BUNDLE`.
4. Value: Copy the single entire line of the privately delivered `GITHUB_ACTIONS_SECRET.txt` file.
5. Open Actions → **Build signed iMaps Release APK** → Run workflow.
6. On a successful run, download the **iMaps-Official-Release-APK** artifact.
7. Extract `app-release.apk` and verify it on a real Android phone.
8. Submit the signed APK to Café Bazaar developer panel along with app name, description, privacy policy and actual app screenshots.

## Security and update rules
- Never put the keystore, password, or base64 secret in GitHub source, issue, PR, chat screenshots, public docs, or websites.
- Back up the private keystore and recovery details in at least two secure locations.
- Reuse the SAME key and EXACT applicationId `ir.imaps.mapapp` for every future production version.
- Increment `versionCode` for every update.
- Prior test/debug APKs are signed by other keys. Uninstall them before installing the first official release. Afterwards, official releases with this key can update each other.
- Google Play Protect can warn on side-loaded apps, including legitimately signed releases. Release signing does NOT guarantee that the warning disappears.
- The app needs an internet connection to load the iMaps website. Location access is only requested with consent.
- Nothing is changed in the WordPress site by this repository.

## Package
- Name: نقشه آنلاین آیمپس
- Application ID: `ir.imaps.mapapp`
- Release version: `1.1.0` (`versionCode=6`)
- Minimum Android: API 23 (Android 6.0)
- Native contact buttons: WhatsApp and device dialer, with no direct call permission
- Web content: https://i-maps.ir/

## Proposed Bazaar listing (Farsi)
Title: نقشه آنلاین آیمپس - تبدیل مختصات

Short: نقشه آنلاین و تبدیل مختصات UTM و جغرافیایی با امکانات کاربردی آیمپس.

Description:
نقشه آنلاین آیمپس، دسترسی سریع به ابزارهای نقشه و تبدیل مختصات را در گوشی اندرویدی فراهم می‌کند. نقشه و ابزارهای سامانه از وب‌سایت i-maps.ir بارگذاری می‌شوند و برای استفاده به اینترنت نیاز دارند.

امکانات:
- مشاهده و جابه‌جایی روی نقشه آنلاین
- استفاده از ابزارهای سامانه نقشه آیمپس و تبدیل مختصات
- دسترسی به موقعیت مکانی با اجازه کاربر
- انتخاب فایل در ابزارهای پشتیبانی‌کننده
- ارتباط با پشتیبانی از طریق واتساپ یا شماره‌گیر تلفن

پشتیبانی: 09213234340

Note: Validate the exact on-device functions and capture authentic screenshots before store submission.
