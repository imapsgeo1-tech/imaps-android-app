package ir.imaps.mapapp;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native inquiry spinner. Does not interfere with location, map zoom or KMZ upload. */
public final class InquiryOverlay {
    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final FrameLayout cover;
    private final ImageView logo;
    private ObjectAnimator spin;
    private boolean shown;
    private final Runnable deadline = this::hide;

    private int dp(float v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }

    public InquiryOverlay(Activity activity, FrameLayout shell) {
        this.activity = activity;
        cover = new FrameLayout(activity);
        cover.setBackgroundColor(Color.argb(224, 5, 19, 40));
        cover.setVisibility(View.GONE);
        cover.setClickable(true);
        shell.addView(cover, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout center = new LinearLayout(activity);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        cover.addView(center, cp);

        FrameLayout circle = new FrameLayout(activity);
        GradientDrawable back = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(13, 57, 80), Color.rgb(8, 33, 64)});
        back.setShape(GradientDrawable.OVAL);
        back.setStroke(dp(1), Color.rgb(173, 137, 65));
        circle.setBackground(back);
        circle.setElevation(dp(9));
        center.addView(circle, new LinearLayout.LayoutParams(dp(152), dp(152)));

        logo = new ImageView(activity);
        logo.setImageResource(R.drawable.imaps_full_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("لوگوی آیمپس در حال استعلام");
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(140), dp(140), Gravity.CENTER);
        circle.addView(logo, lp);

        TextView title = new TextView(activity);
        title.setText("در حال استعلام...");
        title.setTextColor(Color.rgb(244, 211, 137));
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setTextSize(17);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(43));
        tp.topMargin = dp(16);
        center.addView(title, tp);

        TextView subtitle = new TextView(activity);
        subtitle.setText("در حال بررسی اطلاعات عکس‌های هوایی");
        subtitle.setTextColor(Color.rgb(207, 224, 235));
        subtitle.setTextSize(12);
        subtitle.setGravity(Gravity.CENTER);
        center.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(32)));
    }

    private final class Bridge {
        // The bridge exposes UI-only actions. There are no file/device privileges.
        @JavascriptInterface
        public void show() { main.post(InquiryOverlay.this::show); }
        @JavascriptInterface
        public void hide() { main.post(InquiryOverlay.this::hide); }
    }

    public void attach(WebView web) {
        web.addJavascriptInterface(new Bridge(), "IMAPS_LOAD_UI");
    }

    public void show() {
        if (shown) {
            main.removeCallbacks(deadline);
            main.postDelayed(deadline, 22000);
            return;
        }
        shown = true;
        cover.animate().cancel();
        cover.setAlpha(0f);
        cover.setVisibility(View.VISIBLE);
        cover.animate().alpha(1f).setDuration(220).start();

        spin = ObjectAnimator.ofFloat(logo, View.ROTATION, 0f, 360f);
        spin.setDuration(3900);
        spin.setRepeatCount(ObjectAnimator.INFINITE);
        spin.setInterpolator(new LinearInterpolator());
        spin.start();
        main.postDelayed(deadline, 22000);
    }
    public void hide() {
        main.removeCallbacks(deadline);
        if (!shown) return;
        shown = false;
        if (spin != null) { spin.cancel(); spin = null; }
        cover.animate().alpha(0f).setDuration(190)
            .withEndAction(() -> { if (!shown) cover.setVisibility(View.GONE); }).start();
    }

    public void destroy() {
        main.removeCallbacksAndMessages(null);
        if (spin != null) spin.cancel();
        cover.animate().cancel();
        ViewGroup p = (ViewGroup)cover.getParent();
        if (p != null) p.removeView(cover);
    }

    /**
     * Watch the actual inquiry action and requests it causes. The overlay closes
     * after fetch/XMLHttpRequest settles or a short no-request timeout. This
     * never alters the original query, API response or displayed results.
     */
    public void inject(WebView web) {
        String js = "(function(){try{"
            + "if(window.__imapsNativeInquiryV1)return;"
            + "window.__imapsNativeInquiryV1=true;"
            + "var armed=0,pending=0,shownAt=0,seq=0;"
            + "function show(){shownAt=Date.now();seq++;IMAPS_LOAD_UI.show();"
            + "var id=seq;setTimeout(function(){if(id===seq){IMAPS_LOAD_UI.hide();armed=0;}},21000);}"
            + "function hideLater(){var id=seq;setTimeout(function(){"
            + "if(pending===0&&id===seq&&Date.now()-shownAt>=650){IMAPS_LOAD_UI.hide();armed=0;}"
            + "},800);}"
            + "function start(){pending++;}"
            + "function end(){pending=Math.max(0,pending-1);if(pending===0)hideLater();}"
            + "document.addEventListener('click',function(ev){"
            + "var el=ev.target&&ev.target.closest&&ev.target.closest('button,input[type=submit],a,[role=button]');"
            + "if(!el)return;"
            + "var s=((el.value||'')+' '+(el.innerText||el.textContent||'')).replace(/[\\u200c\\u200e\\u200f]/g,' ').trim();"
            + "if(/استعلام/.test(s)&&!/استعلام جدید|پاک کردن|بازگشت|استعلام‌های قبلی/.test(s)){"
            + "armed=Date.now();show();setTimeout(function(){if(pending===0)hideLater();},2000);}"
            + "},true);"
            + "if(window.fetch){var f=window.fetch;window.fetch=function(){"
            + "var watch=armed&&Date.now()-armed<2300;if(watch)start();"
            + "try{var p=f.apply(this,arguments);"
            + "if(!watch)return p;"
            + "return Promise.resolve(p).then(function(v){end();return v;},function(e){end();throw e;});"
            + "}catch(e){if(watch)end();throw e;}"
            + "};}"
            + "var oldSend=XMLHttpRequest.prototype.send;"
            + "XMLHttpRequest.prototype.send=function(){"
            + "var watch=armed&&Date.now()-armed<2300;"
            + "if(watch){start();this.addEventListener('loadend',end,{once:true});}"
            + "return oldSend.apply(this,arguments);"
            + "};"
            + "}catch(e){}})();";
        web.evaluateJavascript(js, null);
    }
}
