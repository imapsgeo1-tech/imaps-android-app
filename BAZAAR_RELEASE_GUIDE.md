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
- Name: سامانه استعلام عکس هوایی آیمپس
- Application ID: `ir.imaps.mapapp` (same as the first signed release)
- Release version: `1.1.4` (`versionCode=10`)
- Minimum Android: API 23 (Android 6.0)
- Native contact buttons: WhatsApp and device dialer, with no direct call permission
- Web content: https://i-maps.ir/?page_id=16588

## Proposed Bazaar listing (Farsi)
Title: سامانه استعلام عکس هوایی آیمپس

Short: استعلام سال‌های عکس‌برداری هوایی قدیمی، مقیاس و تعداد تصاویر بر اساس موقعیت مکانی.

Description:
سامانه استعلام عکس هوایی آیمپس امکان بررسی و جست‌وجوی سال‌های عکس‌برداری هوایی ثبت‌شده در پایگاه اطلاعاتی سامانه را بر اساس موقعیت مکانی در اختیار کاربران قرار می‌دهد. با استفاده از نقشه و ابزارهای جست‌وجوی برنامه، می‌توانید اطلاعات مربوط به سال‌های عکس‌برداری، مقیاس و تعداد عکس‌های موجود را بررسی کنید.

امکانات:
- تعیین محدوده موردنظر روی نقشه
- استعلام سال‌های عکس‌برداری هوایی قدیمی
- مشاهده مقیاس و تعداد عکس‌های ثبت‌شده در نتایج
- استفاده از خدمات آنلاین سامانه آیمپس
- ارتباط با پشتیبانی از طریق واتساپ و تلفن

توجه: نمایش نتیجه استعلام به اطلاعات موجود در پایگاه داده بستگی دارد و لزوماً به معنی موجودبودن همه تصاویر تاریخی برای تمام مناطق نیست. استفاده از برنامه به اینترنت نیاز دارد.

وب‌سایت: https://i-maps.ir/
پشتیبانی: 09213234340

Note: Verify the actual on-device app workflow and take genuine app screenshots before publishing.
