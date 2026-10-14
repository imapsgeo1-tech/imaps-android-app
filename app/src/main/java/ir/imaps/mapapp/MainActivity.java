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
        "https://i-maps.ir/?page_id=16588";
    private static final int PICK_FILE = 1001;
    private static final int LOC_PERMISSION = 1002;
    private static final int NAVY = Color.rgb(11, 50, 67);
    private static final int TEAL = Color.rgb(18, 141, 153);

    private LinearLayout root;
    private WebView web;
    private ProgressBar progress;
    private TextView errorView;
    private boolean fullscreen = false;
    private boolean loadError = false;
    private int lastHeaderPatch = -20;
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
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(NAVY);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        web = new WebView(this);
        content.addView(web, new FrameLayout.LayoutParams(-1, -1));
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        // Prevent pinching/double-tapping the WebView page. Let Leaflet handle map zoom.
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
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

        web.setWebViewClient(new WebViewClient() {
            // Goftino is hidden only in this app. Normal website visitors still see it.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri target = request.getUrl();
                String host = target.getHost();
                if (host != null) {
                    host = host.toLowerCase(java.util.Locale.ROOT);
                    if (host.equals("goftino.com") || host.endsWith(".goftino.com")
                            || host.equals("goftino.net") || host.endsWith(".goftino.net")
                            || host.equals("goftino.io") || host.endsWith(".goftino.io")) {
                        return new WebResourceResponse("text/plain", "UTF-8",
                            new java.io.ByteArrayInputStream(new byte[0]));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                return navigate(request.getUrl());
            }
            @Override
            public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
                loadError = false;
                errorView.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
                lastHeaderPatch = -20;
            }
            @Override
            public void onPageCommitVisible(WebView v, String url) {
                hideWebsiteHeader();
            }
            @Override
            public void onPageFinished(WebView v, String url) {
                if (!loadError) errorView.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
                hideWebsiteHeader();
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
                if (percent >= 10 && percent - lastHeaderPatch >= 20) {
                    lastHeaderPatch = percent;
                    hideWebsiteHeader();
                }
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

        ContactOverlay.attach(this, content);

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
        startActivity(Intent.createChooser(intent, "اشتراک گذاری سامانه استعلام"));
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
        if (value) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    // CSS and DOM changes are applied only in the Android app WebView.
    private void hideWebsiteHeader() {
        Uri uri = Uri.parse(web.getUrl() == null ? HOME_URL : web.getUrl());
        String host = uri.getHost();
        if (host == null || !(host.equalsIgnoreCase("i-maps.ir") ||
              host.toLowerCase().endsWith(".i-maps.ir"))) return;
        String script = "(function(){try{var id='imaps-android-only-style';var css=\"#wpadminbar,#masthead,#site-header,#site-header-wrap,#header,#header-wrap,body > header,header[role='banner'],header.site-header,.site-header,.site-header-wrap,.main-header,.header-main,.header-area,.header-wrapper,.header-container,.header-top,.top-header,.top-bar,.topbar,.topbar-area,.mobile-header,.mobile-header-wrapper,.mobile-menu-header,.elementor-location-header,.elementor-header,[class*='karauos-header'],[id*='karauos-header'],[class*='themento-header'],.sticky-header,.sticky-header-wrap,.site-footer,#colophon,#site-footer,footer[role='contentinfo'],.elementor-location-footer,.page-header,.entry-header,.page-title-area,.page-title-wrap,.page-title-bar,.page-title,.breadcrumb,.breadcrumbs,#breadcrumb,.breadcrumb-wrapper,.scroll-to-top,.back-to-top{display:none!important;height:0!important;max-height:0!important;min-height:0!important;margin:0!important;padding:0!important;visibility:hidden!important;}html,body{padding-top:0!important;margin-top:0!important;}#page,.site,#content,.site-content,#primary,.content-area,main.site-main,.site-main{padding-top:0!important;margin-top:0!important;}#content,.site-content,#primary,.content-area,main.site-main,.site-main{max-width:100%!important;width:100%!important;}\";function apply(){if(!document.head)return;var style=document.getElementById(id);if(!style){style=document.createElement('style');style.id=id;style.textContent=css;document.head.appendChild(style);}if(!document.body)return;document.querySelectorAll(\"#wpadminbar,#masthead,#site-header,#site-header-wrap,#header,#header-wrap,body > header,header[role='banner'],header.site-header,.site-header,.main-header,.header-main,.header-area,.header-wrapper,.header-container,.top-header,.top-bar,.topbar,.mobile-header,.elementor-location-header,.elementor-header,[class*='karauos-header'],[id*='karauos-header'],[class*='themento-header'],.sticky-header,.site-footer,#colophon,#site-footer,footer[role='contentinfo'],.elementor-location-footer,.page-header,.entry-header,.page-title-area,.page-title-wrap,.page-title-bar,.breadcrumb,.breadcrumbs\").forEach(function(node){if(node.closest&&node.closest('#imaps-coordinate-map,#imaps-map,.leaflet-container'))return;node.style.setProperty('display','none','important');});}apply();if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});if(!window.__imapsAppHeaderWatcher&&document.documentElement){window.__imapsAppHeaderWatcher=new MutationObserver(function(changes){if(changes.some(function(change){return change.addedNodes.length>0;}))apply();});window.__imapsAppHeaderWatcher.observe(document.documentElement,{childList:true,subtree:true});}}catch(e){}})();";
        web.evaluateJavascript(script, null);
        // Fallback for a Goftino launcher inserted by the theme or another script.
        String widgetScript = "(function(){try{" +
            "var selector='iframe[src*=\\\"goftino\\\" i],[id*=\\\"goftino\\\" i],[class*=\\\"goftino\\\" i],[data-goftino]';" +
            "function clean(){" +
            "document.querySelectorAll(selector).forEach(function(el){el.remove();});" +
            "if(!document.getElementById('imaps-contact-filter')){" +
            "var style=document.createElement('style');style.id='imaps-contact-filter';" +
            "style.textContent=selector+'{display:none!important;visibility:hidden!important}';" +
            "document.head.appendChild(style);}" +
            "}" +
            "clean();" +
            "if(!window.__imapsContactCleaner&&document.documentElement){" +
            "window.__imapsContactCleaner=new MutationObserver(function(m){" +
            "if(m.some(function(a){return a.addedNodes.length>0;}))clean();" +
            "});window.__imapsContactCleaner.observe(document.documentElement,{" +
            "childList:true,subtree:true});}" +
            "}catch(e){}})();";
        web.evaluateJavascript(widgetScript, null);

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
