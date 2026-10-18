package ir.imaps.mapapp;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Lightweight native splash. Every logo fragment comes from the owner's own
 * R.drawable.imaps_full_logo; no invented substitute maps or lettering.
 * No unnecessary wait: it leaves as soon as both the short entrance animation
 * and the webpage's initial load complete, with a failsafe for offline phones.
 */
public final class SplashOverlay {
    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FrameLayout view;
    private boolean ready;
    private boolean dismissed;
    private final long started = android.os.SystemClock.uptimeMillis();
    private static final long MIN_MS = 2450L;

    private int dp(float v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
    private TextView label(String value, int sizeSp, int color, boolean bold) {
        TextView t = new TextView(activity);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(sizeSp);
        t.setGravity(Gravity.CENTER);
        t.setLetterSpacing(0.055f);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }

    public SplashOverlay(Activity activity, FrameLayout shell) {
        this.activity = activity;
        view = new FrameLayout(activity);
        view.setClickable(true);
        view.setFocusable(true);
        GradientDrawable background = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(4, 16, 37), Color.rgb(9, 40, 70), Color.rgb(3, 20, 42)});
        view.setBackground(background);
        shell.addView(view, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout center = new LinearLayout(activity);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(
            -1, -2, Gravity.CENTER);
        centerParams.leftMargin = dp(16);
        centerParams.rightMargin = dp(16);
        view.addView(center, centerParams);

        FrameLayout logoStage = new FrameLayout(activity);
        center.addView(logoStage, new LinearLayout.LayoutParams(dp(310), dp(310)));

        // Extract country outline, globe, and original two letter arcs directly
        // from the transparent brand image; colorize only the initial relief.
        Bitmap[] layers = splitOriginalLogo();
        ImageView topo = addLayer(logoStage, layers[0]);
        ImageView globe = addLayer(logoStage, layers[1]);
        ImageView upper = addLayer(logoStage, layers[2]);
        ImageView lower = addLayer(logoStage, layers[3]);

        topo.setAlpha(0f);
        topo.setRotation(-19f);
        topo.setScaleX(0.78f);
        topo.setScaleY(0.78f);
        topo.animate().alpha(1f).rotation(0f).scaleX(1.02f).scaleY(1.02f)
            .setDuration(870).setStartDelay(80).start();
        handler.postDelayed(() -> topo.animate().alpha(0f).setDuration(510).start(), 1320);

        enter(globe, 740, 1160, 0f, -9f, .88f);
        enter(upper, 1280, 700, -dp(16), -11f, 1.04f);
        enter(lower, 1530, 720, dp(21), 12f, 1.04f);

        TextView name = label("IMAPS WebGIS", 23, Color.rgb(251, 196, 93), true);
        name.setAlpha(0f);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, dp(40));
        nameParams.topMargin = -dp(9);
        center.addView(name, nameParams);
        name.setTranslationY(dp(10));
        name.animate().alpha(1f).translationY(0).setStartDelay(1750).setDuration(570).start();

        TextView caption = label("سامانه هوشمند استعلام سال‌های عکسبرداری هوایی",
            12, Color.rgb(210, 228, 237), false);
        caption.setAlpha(0f);
        center.addView(caption, new LinearLayout.LayoutParams(-1, dp(38)));
        caption.animate().alpha(1f).setStartDelay(1940).setDuration(450).start();

        TextView brand = label("© iMaps 2026", 11, Color.rgb(165, 188, 203), false);
        FrameLayout.LayoutParams brandParams = new FrameLayout.LayoutParams(-1, dp(32), Gravity.BOTTOM);
        brandParams.bottomMargin = dp(18);
        view.addView(brand, brandParams);

        // A slow server should not trap visitors behind the splash.
        handler.postDelayed(this::dismiss, 10500);
    }

    private ImageView addLayer(FrameLayout stage, Bitmap bitmap) {
        ImageView v = new ImageView(activity);
        v.setImageDrawable(new BitmapDrawable(activity.getResources(), bitmap));
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        stage.addView(v, new FrameLayout.LayoutParams(-1, -1));
        return v;
    }

    private void enter(View v, long delay, long duration, float y, float rotation, float scale) {
        v.setAlpha(0f);
        v.setRotation(rotation);
        v.setScaleX(scale);
        v.setScaleY(scale);
        v.setTranslationY(y);
        v.animate().alpha(1f).rotation(0f).translationY(0).scaleX(1f).scaleY(1f)
            .setStartDelay(delay).setDuration(duration).start();
    }

    /**
     * Image is already a carefully cropped/transparent copy of the real logo.
     * Golden letter arcs are outside the center globe, Iran is the dark-blue
     * country silhouette. The terrain hatching is illustrative topography.
     */
    private Bitmap[] splitOriginalLogo() {
        Bitmap source = BitmapFactory.decodeResource(activity.getResources(), R.drawable.imaps_full_logo);
        int w = source.getWidth(), h = source.getHeight();
        Bitmap[] result = new Bitmap[4];
        for (int i = 0; i < 4; i++) result[i] = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] pix = new int[w*h];
        int[][] layers = new int[4][w*h];
        source.getPixels(pix, 0, w, 0, 0, w, h);
        float cx = w * .51f, cy = h * .505f;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y*w + x;
                int c = pix[i], a = Color.alpha(c);
                if (a < 12) continue;
                int r = Color.red(c), g = Color.green(c), b = Color.blue(c);
                boolean gold = r > 145 && r > b + 37 && g > b + 12;
                boolean navy = r < 125 && g < 150 && b < 175 && b >= r;
                float dx = x - cx, dy = y - cy;
                boolean center = (dx*dx + dy*dy) < w*w*.111f;
                if (center && (gold || navy)) layers[1][i] = c;
                if (!center && gold) layers[y < h*.55f ? 2 : 3][i] = c;
                if (navy && center) {
                    float u = x/(float)w, v = y/(float)h;
                    double wave = Math.sin(u*30 + Math.sin(v*17)*1.9)
                        + Math.cos(v*29+u*7)*.8 + Math.sin((u+v)*48)*.28;
                    float t = (float)((wave+2.2)/4.4);
                    // a relief gradient and repeating contour strokes,
                    // fully clipped by the exact outline in the original logo.
                    int rr = Math.max(20, Math.min(104, (int)(32 + 56*t)));
                    int gg = Math.max(74, Math.min(171, (int)(87 + 66*t)));
                    int bb = Math.max(92, Math.min(192, (int)(111 + 63*t)));
                    double contour = Math.abs(Math.sin((v*20 + Math.sin(u*15)*.25)*Math.PI));
                    if (contour > .972) { rr=193; gg=170; bb=101; }
                    layers[0][i] = Color.argb(a, rr,gg,bb);
                }
            }
        }
        for (int i=0;i<4;i++) result[i].setPixels(layers[i], 0, w, 0, 0, w, h);
        source.recycle();
        return result;
    }

    public void onPageReady() {
        ready = true;
        long elapsed = android.os.SystemClock.uptimeMillis() - started;
        handler.postDelayed(this::dismiss, Math.max(0L, MIN_MS - elapsed));
    }
    private void dismiss() {
        if (dismissed) return;
        dismissed = true;
        view.animate().alpha(0f).setDuration(290).withEndAction(() -> {
            ViewGroup parent = (ViewGroup)view.getParent();
            if (parent != null) parent.removeView(view);
        }).start();
    }
    public void destroy() {
        handler.removeCallbacksAndMessages(null);
        if (!dismissed) {
            view.animate().cancel();
            ViewGroup p=(ViewGroup)view.getParent();
            if(p!=null)p.removeView(view);
            dismissed=true;
        }
    }
}
