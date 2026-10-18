package ir.imaps.mapapp;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
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

/**
 * Only the genuine transparent iMaps logo rotates at the center.
 * No circle, white box, dimmer, subtitle, or artificial progress stripe.
 */
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
        cover.setBackgroundColor(Color.TRANSPARENT);
        cover.setVisibility(View.GONE);
        cover.setClickable(false);
        shell.addView(cover, new FrameLayout.LayoutParams(-1, -1));

        logo = new ImageView(activity);
        logo.setImageResource(R.drawable.imaps_full_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("لوگوی آیمپس در حال استعلام");
        FrameLayout.LayoutParams lp =
            new FrameLayout.LayoutParams(dp(155), dp(155), Gravity.CENTER);
        cover.addView(logo, lp);
    }

    private final class Bridge {
        @JavascriptInterface
        public void show() { main.post(InquiryOverlay.this::show); }
        @JavascriptInterface
        public void hide() { main.post(InquiryOverlay.this::hide); }
    }

    public void attach(WebView web) {
        web.addJavascriptInterface(new Bridge(), "IMAPS_LOAD_UI");
    }

    public void show() {
        main.removeCallbacks(deadline);
        if (shown) {
            main.postDelayed(deadline, 18000);
            return;
        }
        shown = true;
        cover.animate().cancel();
        cover.setAlpha(0f);
        cover.setVisibility(View.VISIBLE);
        cover.animate().alpha(1f).setDuration(140).start();

        logo.setRotation(0f);
        spin = ObjectAnimator.ofFloat(logo, View.ROTATION, 0f, 360f);
        spin.setDuration(2600);
        spin.setRepeatCount(ObjectAnimator.INFINITE);
        spin.setInterpolator(new LinearInterpolator());
        spin.start();
        main.postDelayed(deadline, 18000);
    }

    public void hide() {
        main.removeCallbacks(deadline);
        if (!shown) return;
        shown = false;
        if (spin != null) { spin.cancel(); spin = null; }
        cover.animate().alpha(0f).setDuration(140)
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
     * Observe inquiry requests without changing the data returned by the WebGIS
     * server. The existing UI and AJAX handlers are left intact.
     */
    public void inject(WebView web) {
        String js = "(function(){try{"
            + "if(window.__imapsNativeInquiryV2)return;"
            + "window.__imapsNativeInquiryV2=true;"
            + "var armed=0,pending=0,seq=0;"
            + "function show(){seq++;window.IMAPS_LOAD_UI.show();"
            + "var id=seq;setTimeout(function(){if(id===seq){IMAPS_LOAD_UI.hide();armed=0;}},17000);}"
            + "function end(){pending=Math.max(0,pending-1);"
            + "if(pending===0){var id=seq;setTimeout(function(){"
            + "if(pending===0&&id===seq){IMAPS_LOAD_UI.hide();armed=0;}"
            + "},650);}}"
            + "document.addEventListener('click',function(ev){"
            + "var el=ev.target&&ev.target.closest&&ev.target.closest('button,input[type=submit],a,[role=button]');"
            + "if(!el)return;"
            + "var s=((el.value||'')+' '+(el.innerText||el.textContent||'')).replace(/[\\u200c\\u200e\\u200f]/g,' ').trim();"
            + "if(/استعلام/.test(s)&&!/استعلام جدید|پاک کردن|بازگشت|استعلام‌های قبلی/.test(s)){"
            + "armed=Date.now();show();setTimeout(function(){if(pending===0)end();},2300);}"
            + "},true);"
            + "if(window.fetch){var f=window.fetch;window.fetch=function(){"
            + "var watch=armed&&Date.now()-armed<5000;if(watch)pending++;"
            + "try{var p=f.apply(this,arguments);if(!watch)return p;"
            + "return Promise.resolve(p).then(function(v){end();return v;},function(e){end();throw e;});"
            + "}catch(e){if(watch)end();throw e;}"
            + "};}"
            + "var oldSend=XMLHttpRequest.prototype.send;"
            + "XMLHttpRequest.prototype.send=function(){"
            + "var watch=armed&&Date.now()-armed<5000;"
            + "if(watch){pending++;this.addEventListener('loadend',end,{once:true});}"
            + "return oldSend.apply(this,arguments);"
            + "};"
            + "}catch(e){}})();";
        web.evaluateJavascript(js, null);
    }
}
