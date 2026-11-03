package ir.imaps.mapapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Version gate for future iMaps/Cafe Bazaar releases.
 *
 * The policy is public, independent of the APK, and can be updated by the
 * developer without rebuilding the app. The initial policy is deliberately
 * non-blocking. Raise its minimum only after a newer, identically signed APK
 * is live in Cafe Bazaar.
 *
 * For older APKs that did not ship this gate, no APK-only change can force them
 * to upgrade. Avoid altering the public WebGIS to block website visitors.
 */
public final class UpdateGate {
    private static final String POLICY_URL =
        "https://raw.githubusercontent.com/imapsgeo1-tech/imaps-android-app/main/app-update.json";
    private static final String STORE_URL =
        "https://cafebazaar.ir/app/ir.imaps.mapapp";
    private static final String PACKAGE_ID = "ir.imaps.mapapp";
    private static final String PREFS = "imaps_android_version_gate";
    private static final long RECHECK_MS = 90_000L;
    private static final long CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;

    private final Activity activity;
    private final FrameLayout root;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final int ownVersion;
    private FrameLayout blocker;
    private TextView description;
    private long lastCheck = -RECHECK_MS;
    private boolean fetching;
    private boolean destroyed;

    private int dp(float px) {
        return Math.round(activity.getResources().getDisplayMetrics().density * px);
    }
    private static GradientDrawable rounded(int color, int radiusPx, int strokeColor, int strokePx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        if (strokePx > 0) drawable.setStroke(strokePx, strokeColor);
        return drawable;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(activity);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }
    private int installedVersion() {
        try {
            PackageInfo info = activity.getPackageManager().getPackageInfo(PACKAGE_ID, 0);
            if (Build.VERSION.SDK_INT >= 28) {
                return (int) Math.min(Integer.MAX_VALUE, info.getLongVersionCode());
            }
            return info.versionCode;
        } catch (Exception e) {
            return Integer.MAX_VALUE; // Never block on a failed package lookup.
        }
    }

    public UpdateGate(Activity activity, FrameLayout root) {
        this.activity = activity;
        this.root = root;
        this.ownVersion = installedVersion();
        applyCachedPolicy();
        check();
    }

    public void onResume() {
        check();
    }

    /** Recheck at most once every 90s, except for an explicit Retry tap. */
    private void check() {
        check(false);
    }
    private void check(boolean force) {
        if (destroyed || fetching) return;
        long elapsed = SystemClock.elapsedRealtime();
        if (!force && elapsed - lastCheck < RECHECK_MS) return;
        lastCheck = elapsed;
        fetching = true;

        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection)
                    new URL(POLICY_URL).openConnection();
                connection.setConnectTimeout(5500);
                connection.setReadTimeout(5500);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                connection.setUseCaches(false);
                try {
                    int status = connection.getResponseCode();
                    if (status != 200) throw new IllegalStateException("HTTP " + status);
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (InputStream in = connection.getInputStream()) {
                        byte[] buf = new byte[2048];
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            if (bytes.size() + n > 16_384) {
                                throw new IllegalStateException("Policy unexpectedly large");
                            }
                            bytes.write(buf, 0, n);
                        }
                    }
                    JSONObject json = new JSONObject(
                        new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                    if (json.optInt("schema_version", -1) != 1 ||
                        !PACKAGE_ID.equals(json.optString("package_id", ""))) {
                        throw new IllegalStateException("Unrecognized update policy");
                    }
                    int latest = json.optInt("latest_version_code", -1);
                    int minimum = json.optInt("minimum_supported_version_code", -1);
                    if (latest < 1 || latest > 1_000_000 ||
                        minimum < 1 || minimum > latest) {
                        throw new IllegalStateException("Invalid version thresholds");
                    }
                    String name = json.optString("latest_version_name", "");
                    if (!name.matches("[0-9]+(\\.[0-9]+){1,3}")) name = "";
                    final String versionName = name;
                    ui.post(() -> {
                        fetching = false;
                        if (destroyed) return;
                        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putInt("latest", latest)
                            .putInt("minimum", minimum)
                            .putString("latestName", versionName)
                            .putLong("savedAt", System.currentTimeMillis())
                            .apply();
                        enforce(minimum, latest, versionName);
                    });
                } finally {
                    connection.disconnect();
                }
            } catch (Exception ignored) {
                // Fail open if we have never received a valid policy.
                // A recent cached mandatory policy is still enforced offline.
                ui.post(() -> {
                    fetching = false;
                    if (!destroyed) applyCachedPolicy();
                });
            }
        }, "imaps-version-policy").start();
    }

    private void applyCachedPolicy() {
        SharedPreferences pref = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long saved = pref.getLong("savedAt", 0);
        if (saved <= 0 || saved > now || now - saved > CACHE_MAX_AGE_MS) return;
        enforce(pref.getInt("minimum", 0), pref.getInt("latest", 0),
            pref.getString("latestName", ""));
    }

    private void enforce(int minimum, int latest, String latestName) {
        if (destroyed) return;
        if (minimum < 1 || latest < minimum || ownVersion >= minimum) {
            dismiss();
            return;
        }
        showBlockingUpdate(latestName);
    }

    private void showBlockingUpdate(String latestName) {
        if (blocker != null) {
            if (description != null) description.setText(message(latestName));
            return;
        }

        blocker = new FrameLayout(activity);
        blocker.setBackgroundColor(Color.rgb(5, 20, 39));
        blocker.setClickable(true);
        blocker.setFocusable(true);
        blocker.setFocusableInTouchMode(true);
        root.addView(blocker, new FrameLayout.LayoutParams(-1, -1));
        blocker.bringToFront();

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(24), dp(26), dp(24), dp(28));
        panel.setBackground(rounded(Color.rgb(12, 43, 66), dp(22),
            Color.rgb(168, 134, 67), dp(1)));
        FrameLayout.LayoutParams box = new FrameLayout.LayoutParams(
            -1, -2, Gravity.CENTER);
        box.leftMargin = dp(24);
        box.rightMargin = dp(24);
        blocker.addView(panel, box);

        ImageView brand = new ImageView(activity);
        brand.setImageResource(R.drawable.imaps_full_logo);
        brand.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brand.setContentDescription("لوگوی اصلی آیمپس");
        panel.addView(brand, new LinearLayout.LayoutParams(dp(92), dp(92)));

        TextView heading = text("به‌روزرسانی ضروری آیمپس", 19,
            Color.rgb(245, 197, 103), true);
        LinearLayout.LayoutParams headingParams =
            new LinearLayout.LayoutParams(-1, dp(58));
        headingParams.topMargin = dp(8);
        panel.addView(heading, headingParams);

        description = text(message(latestName), 14,
            Color.rgb(221, 232, 241), false);
        description.setLineSpacing(dp(5), 1f);
        LinearLayout.LayoutParams descParams =
            new LinearLayout.LayoutParams(-1, -2);
        descParams.bottomMargin = dp(22);
        panel.addView(description, descParams);

        TextView update = text("دریافت نسخه جدید از کافه‌بازار", 15, Color.WHITE, true);
        update.setBackground(rounded(Color.rgb(13, 132, 137), dp(13), 0, 0));
        update.setClickable(true);
        update.setOnClickListener(v -> openBazaar());
        panel.addView(update, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView retry = text("بررسی مجدد نسخه", 12,
            Color.rgb(177, 201, 217), false);
        retry.setClickable(true);
        retry.setOnClickListener(v -> check(true));
        LinearLayout.LayoutParams retryParams =
            new LinearLayout.LayoutParams(-1, dp(42));
        retryParams.topMargin = dp(10);
        panel.addView(retry, retryParams);

        blocker.requestFocus();
    }

    private String message(String name) {
        return "برای ادامه استفاده از سامانه استعلام عکس‌های هوایی، "
            + "باید اپلیکیشن را به نسخه جدید"
            + (name == null || name.isEmpty() ? "" : " " + name)
            + " ارتقا دهید. پس از نصب، دوباره وارد آیمپس شوید.";
    }

    private void openBazaar() {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(STORE_URL)));
        } catch (ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(activity, "باز کردن کافه‌بازار ممکن نیست",
                Toast.LENGTH_LONG).show();
        }
    }

    private void dismiss() {
        if (blocker == null) return;
        ViewGroup parent = (ViewGroup) blocker.getParent();
        if (parent != null) parent.removeView(blocker);
        blocker = null;
        description = null;
    }

    public void destroy() {
        destroyed = true;
        ui.removeCallbacksAndMessages(null);
        dismiss();
    }
}
