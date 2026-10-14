package ir.imaps.mapapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME_URL =
        "https://i-maps.ir/%D8%B3%D8%A7%D9%85%D8%A7%D9%86%D9%87-%D9%86%D9%82%D8%B4%D9%87-%D8%A2%D9%86%D9%84%D8%A7%DB%8C%D9%86-%D8%A2%DB%8C%D9%85%D9%BE%D8%B3/";
    private static final int PICK_FILE = 1001;
    private static final int LOC_PERMISSION = 1002;
    private static final int NAVY = Color.rgb(11, 50, 67);
    private static final int TEAL = Color.rgb(18, 141, 153);

    private LinearLayout root, header, footer;
    private WebView web;
    private ProgressBar progress;
    private TextView errorView;
    private boolean fullscreen = false;
    private boolean loadError = false;
    private android.webkit.ValueCallback<Uri[]> uploadCallback;
    private GeolocationPermissions.Callback locationCallback;
    private String locationOrigin;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private TextView text(String s, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(s);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private TextView action(String s, LinearLayout parent, View.OnClickListener click) {
        TextView button = text(s, 13, NAVY);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(3), dp(9), dp(3), dp(9));
        button.setOnClickListener(click);
        parent.addView(button, new LinearLayout.LayoutParams(0, dp(53), 1));
        return button;
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(dp(15), dp(7), dp(15), dp(7));
        header.setBackgroundColor(NAVY);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(62)));

        TextView logo = text("i", 23, Color.WHITE);
        logo.setTypeface(null, Typeface.BOLD);
        logo.setBackground(bg(TEAL, 12));
        header.addView(logo, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView title = text("آیمپس | نقشه آنلاین", 17, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        title.setPadding(dp(9), 0, dp(9), 0);
        header.addView(title, new LinearLayout.LayoutParams(0, -1, 1));

        TextView expand = text("⛶", 26, Color.WHITE);
        expand.setContentDescription("نمایش تمام صفحه");
        expand.setOnClickListener(v -> setFullscreen(!fullscreen));
        header.addView(expand, new LinearLayout.LayoutParams(dp(42), -1));

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        web = new WebView(this);
        content.addView(web, new FrameLayout.LayoutParams(-1, -1));
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        FrameLayout.LayoutParams loadingParams = new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP);
        content.addView(progress, loadingParams);

        errorView = text("", 15, NAVY);
        errorView.setPadding(dp(22), dp(22), dp(22), dp(22));
        errorView.setBackgroundColor(Color.WHITE);
        errorView.setVisibility(View.GONE);
        errorView.setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            web.reload();
        });
        content.addView(errorView, new FrameLayout.LayoutParams(-1, -1));

        footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setBackgroundColor(Color.rgb(239, 246, 248));
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(58)));
        action("خانه", footer, v -> web.loadUrl(HOME_URL));
        action("بارگذاری مجدد", footer, v -> { errorView.setVisibility(View.GONE); web.reload(); });
        action("اشتراک گذاری", footer, v -> share());
        action("بیشتر", footer, v -> menu(v));

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                return navigate(request.getUrl());
            }
            @Override
            public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
                loadError = false;
                errorView.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPageFinished(WebView v, String url) {
                if (!loadError) errorView.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
            }
            @Override
            public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) showError("صفحه بارگذاری نشد. اتصال اینترنت را بررسی کنید.\nبرای تلاش مجدد اینجا را لمس کنید.");
            }
            @Override
            public void onReceivedHttpError(WebView v, WebResourceRequest req, WebResourceResponse resp) {
                if (req.isForMainFrame() && resp.getStatusCode() >= 400)
                    showError("سرور سایت خطای " + resp.getStatusCode() + " داده است.\nبرای تلاش مجدد اینجا را لمس کنید.");
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int percent) {
                progress.setProgress(percent);
                if (percent == 100) progress.setVisibility(View.GONE);
            }
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                Uri uri = Uri.parse(origin);
                if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                    uri.getHost() == null ||
                    !(uri.getHost().equalsIgnoreCase("i-maps.ir") ||
                      uri.getHost().endsWith(".i-maps.ir"))) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (hasLocation()) callback.invoke(origin, true, false);
                else {
                    locationCallback = callback;
                    locationOrigin = origin;
                    requestPermissions(new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    }, LOC_PERMISSION);
                }
            }
            @Override
            public boolean onShowFileChooser(WebView view,
                android.webkit.ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                try {
                    startActivityForResult(
                        Intent.createChooser(params.createIntent(), "انتخاب فایل نقشه"),
                        PICK_FILE);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    uploadCallback.onReceiveValue(null);
                    uploadCallback = null;
                    Toast.makeText(MainActivity.this, "انتخاب فایل ممکن نیست", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        web.setDownloadListener((url, agent, disposition, mimeType, length) -> {
            if (Build.VERSION.SDK_INT < 29 || !url.startsWith("https://")) {
                openOutside(url);
                return;
            }
            try {
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                String filename = URLUtil.guessFileName(url, disposition, mimeType)
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
                req.setTitle(filename);
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                if (agent != null) req.addRequestHeader("User-Agent", agent);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) req.addRequestHeader("Cookie", cookie);
                ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(req);
                Toast.makeText(this, "دانلود شروع شد", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                openOutside(url);
            }
        });

        if (state == null || web.restoreState(state) == null) web.loadUrl(HOME_URL);
    }

    private void showError(String message) {
        loadError = true;
        progress.setVisibility(View.GONE);
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private boolean navigate(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return true;
        if (scheme.equalsIgnoreCase("about") || scheme.equalsIgnoreCase("blob")) return false;
        String host = uri.getHost();
        if ((scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http")) &&
            host != null && (host.equalsIgnoreCase("i-maps.ir") ||
                             host.toLowerCase().endsWith(".i-maps.ir"))) return false;
        openOutside(uri.toString());
        return true;
    }

    private void openOutside(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "باز کردن لینک ممکن نیست", Toast.LENGTH_SHORT).show();
        }
    }

    private void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, web.getUrl() == null ? HOME_URL : web.getUrl());
        startActivity(Intent.createChooser(intent, "اشتراک گذاری نقشه"));
    }

    private void menu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(fullscreen ? "خروج از تمام صفحه" : "نمایش تمام صفحه")
            .setOnMenuItemClickListener(item -> { setFullscreen(!fullscreen); return true; });
        popup.getMenu().add("باز کردن در مرورگر")
            .setOnMenuItemClickListener(item -> { openOutside(web.getUrl() == null ? HOME_URL : web.getUrl()); return true; });
        popup.getMenu().add("صفحه اصلی")
            .setOnMenuItemClickListener(item -> { web.loadUrl(HOME_URL); return true; });
        popup.show();
    }

    private void setFullscreen(boolean value) {
        fullscreen = value;
        header.setVisibility(value ? View.GONE : View.VISIBLE);
        footer.setVisibility(value ? View.GONE : View.VISIBLE);
    }

    private boolean hasLocation() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOC_PERMISSION && locationCallback != null) {
            locationCallback.invoke(locationOrigin, hasLocation(), false);
            locationCallback = null;
            locationOrigin = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && uploadCallback != null) {
            uploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            uploadCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        web.saveState(out);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onBackPressed() {
        if (fullscreen) setFullscreen(false);
        else if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (uploadCallback != null) uploadCallback.onReceiveValue(null);
        if (locationCallback != null) locationCallback.invoke(locationOrigin, false, false);
        if (web != null) {
            ((ViewGroup) web.getParent()).removeView(web);
            web.destroy();
        }
        super.onDestroy();
    }
}
