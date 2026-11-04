package ir.imaps.mapapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Real native Android GPS action.
 *
 * The visible button is now an HTML overlay physically INSIDE Leaflet's map
 * container, anchored to its top right. Its explicit link click is handled by
 * the Android WebViewClient, which calls onLocateRequest() below.
 * Actual GPS access remains Android-native and on-demand. No JavaScript
 * interface exposes device permissions to embedded third-party frames.
 * Location is never tracked in the background or transmitted here.
 */
public final class LocationFeature {
    public static final int PERMISSION_CODE = 1004;
    private static final long POSITION_TIMEOUT_MS = 16000L;
    private final Activity activity;
    private final FrameLayout host;
    private final WebView web;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final TextView action;
    private LocationManager manager;
    private LocationListener listener;
    private Location best;
    private Runnable deadline;
    private Runnable settle;
    private boolean active;
    private boolean destroyed;

    private int dp(float n) {
        return Math.round(activity.getResources().getDisplayMetrics().density * n);
    }

    public LocationFeature(Activity activity, FrameLayout host, WebView web) {
        this.activity = activity;
        this.host = host;
        this.web = web;

        action = new TextView(activity);
        action.setText("◎ موقعیت من");
        action.setTextSize(13);
        action.setTextColor(Color.rgb(5, 66, 84));
        action.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(12), 0, dp(12), 0);
        action.setContentDescription("نمایش موقعیت فعلی من روی نقشه آیمپس");
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(13));
        bg.setStroke(dp(1), Color.rgb(196, 211, 222));
        action.setBackground(bg);
        action.setElevation(dp(7));
        action.setOnClickListener(v -> onTap());

        // The visible GPS button is injected in the WebGIS map's DOM.
        // Do NOT attach the old native button to the activity surface:
        // doing so would place it outside the scrollable map region.
    }

    private boolean trustedPage() {
        try {
            Uri uri = Uri.parse(web.getUrl() == null ? "" : web.getUrl());
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) &&
                host != null && (host.equalsIgnoreCase("i-maps.ir") ||
                    host.toLowerCase(Locale.ROOT).endsWith(".i-maps.ir"));
        } catch (Exception e) {
            return false;
        }
    }

    private void message(String content) {
        if (destroyed) return;
        Toast.makeText(activity, content, Toast.LENGTH_LONG).show();
    }
    private void loading(boolean yes) {
        active = yes;
        action.setEnabled(!yes);
        action.setAlpha(yes ? .75f : 1f);
        action.setText(yes ? "◌ مکان‌یابی…" : "◎ موقعیت من");
        // The button lives inside the real Leaflet map, so only update its
        // label here; never move the map UI or attribution.
        if (trustedPage()) {
            web.evaluateJavascript(
                "(function(){if(window.IMAPSAndroidLocation&&" +
                "window.IMAPSAndroidLocation.version===3){" +
                "window.IMAPSAndroidLocation.setBusy("+(yes ? "true" : "false")+");}})()",
                null);
        }
    }

    /** Called only for a real user-initiated, main-frame GPS link tap. */
    public void onLocateRequest() {
        onTap();
    }

    private void onTap() {
        if (active || destroyed) return;
        if (!trustedPage()) {
            message("ابتدا وارد سامانه WebGIS آیمپس شوید.");
            return;
        }

        // Confirm the exact map and the location bridge are ready BEFORE
        // asking for sensitive GPS permission.
        try {
            web.evaluateJavascript(
                "(function(){return !!(window.IMAPSAndroidLocation" +
                "&&window.IMAPSAndroidLocation.version===3" +
                "&&window.imapsWebGISReady&&window.imapsWebGISReady.map" +
                "&&document.querySelector('.imaps-gis'));})()",
                result -> {
                    if (destroyed) return;
                    if (!"true".equals(result)) {
                        message("نقشه هنوز آماده نشده است. چند لحظه دیگر دوباره بزنید.");
                        return;
                    }
                    requestUserPermission();
                });
        } catch (Exception e) {
            message("نقشه در دسترس نیست. یک‌بار صفحه را تازه‌سازی کنید.");
        }
    }

    private boolean hasLocationPermission() {
        return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
            activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestUserPermission() {
        if (destroyed) return;
        if (hasLocationPermission()) {
            findCurrentLocation();
            return;
        }
        activity.requestPermissions(new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        }, PERMISSION_CODE);
    }

    public void onPermissionResult(int requestCode) {
        if (requestCode != PERMISSION_CODE || destroyed) return;
        if (!hasLocationPermission()) {
            message("اجازه مکان‌یابی داده نشد. از تنظیمات گوشی می‌توانید آن را فعال کنید.");
        } else {
            findCurrentLocation();
        }
    }

    private void consider(Location fix) {
        if (fix == null || !active || destroyed) return;
        if (!Double.isFinite(fix.getLatitude()) ||
            !Double.isFinite(fix.getLongitude()) ||
            fix.getLatitude() < -90 || fix.getLatitude() > 90 ||
            fix.getLongitude() < -180 || fix.getLongitude() > 180) return;

        if (best == null || fix.getAccuracy() < best.getAccuracy()) {
            best = fix;
        }

        // Accurate GPS: display immediately. For approximate fixes, allow a
        // short opportunity for another provider to improve precision.
        if (best.getAccuracy() > 0 && best.getAccuracy() <= 90f) {
            deliver();
        } else if (settle == null) {
            settle = this::deliver;
            main.postDelayed(settle, 2900);
        }
    }

    private void findCurrentLocation() {
        if (destroyed || active) return;
        manager = (LocationManager)
            activity.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            message("سرویس موقعیت‌یابی گوشی در دسترس نیست.");
            return;
        }

        final boolean fine = activity.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        final boolean coarse = activity.checkSelfPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            message("دسترسی به موقعیت گوشی داده نشده است.");
            return;
        }

        boolean gps = fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gps && !network) {
            message("موقعیت مکانی گوشی خاموش است؛ GPS را فعال کنید.");
            return;
        }

        loading(true);
        best = null;
        listener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                consider(location);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        // A recent fix is an optional acceleration, never an indefinitely
        // cached previous destination.
        for (String provider : new String[]{
            LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            if ((provider.equals(LocationManager.GPS_PROVIDER) && !gps) ||
                (provider.equals(LocationManager.NETWORK_PROVIDER) && !network)) continue;
            try {
                Location recent = manager.getLastKnownLocation(provider);
                if (recent != null &&
                    System.currentTimeMillis() - recent.getTime() >= 0 &&
                    System.currentTimeMillis() - recent.getTime() < 45_000L) {
                    consider(recent);
                }
            } catch (SecurityException ignored) {}
        }
        if (!active) return;

        int subscribed = 0;
        if (gps) {
            try {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0f, listener, Looper.getMainLooper());
                subscribed++;
            } catch (SecurityException | IllegalArgumentException ignored) {}
        }
        if (network) {
            try {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0, 0f, listener, Looper.getMainLooper());
                subscribed++;
            } catch (SecurityException | IllegalArgumentException ignored) {}
        }
        if (subscribed == 0) {
            if (best != null) deliver();
            else fail("امکان دریافت موقعیت وجود ندارد؛ GPS را بررسی کنید.");
            return;
        }
        deadline = () -> {
            if (best != null) deliver();
            else fail("موقعیت دریافت نشد. GPS را روشن کنید یا به فضای باز بروید.");
        };
        main.postDelayed(deadline, POSITION_TIMEOUT_MS);
        message("در حال دریافت موقعیت فعلی گوشی…");
    }

    private void stopUpdates() {
        if (deadline != null) main.removeCallbacks(deadline);
        if (settle != null) main.removeCallbacks(settle);
        deadline = null;
        settle = null;
        if (manager != null && listener != null) {
            try { manager.removeUpdates(listener); } catch (SecurityException ignored) {}
        }
        listener = null;
    }

    private void fail(String msg) {
        stopUpdates();
        loading(false);
        message(msg);
    }

    private void deliver() {
        if (!active || destroyed) return;
        Location fix = best;
        stopUpdates();
        loading(false);
        if (fix == null) {
            message("موقعیت گوشی دریافت نشد.");
            return;
        }
        if (!trustedPage()) return;

        // Strictly numeric coordinates obtained from the native Android API.
        String javascript = String.format(Locale.US,
            "(function(){try{if(!window.IMAPSAndroidLocation)return false;" +
            "return !!window.IMAPSAndroidLocation.apply(%.8f,%.8f,%.2f);" +
            "}catch(e){return false;}})()",
            fix.getLatitude(), fix.getLongitude(),
            Math.max(0f, fix.getAccuracy()));
        web.evaluateJavascript(javascript, value -> {
            if (!"true".equals(value)) {
                message("موقعیت دریافت شد، اما نقشه آماده نمایش نیست. دوباره امتحان کنید.");
            }
        });
    }

    public void destroy() {
        destroyed = true;
        stopUpdates();
        main.removeCallbacksAndMessages(null);
        action.setOnClickListener(null);
    }
}
