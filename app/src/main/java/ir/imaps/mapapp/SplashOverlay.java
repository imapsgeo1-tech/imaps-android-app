package ir.imaps.mapapp;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

/**
 * Native-only animated brand entrance. No bitmap pixel-processing on the
 * main thread; the logo is a genuine bundled high-resolution PNG.
 * Page navigation continues behind this view, without an artificial delay.
 */
public final class SplashOverlay {
    private static final long MIN_VISIBLE_MS = 1650L;
    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FrameLayout view;
    private final long started = SystemClock.uptimeMillis();
    private boolean leaving = false;
    private boolean initialPageReady = false;
    private final Runnable fallback = this::dismiss;
    private final Runnable pendingDismiss = this::dismiss;

    private int dp(float v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(activity);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }
    private void entrance(View v, long delay, long duration, float scale, float y, float startRotate) {
        v.setAlpha(0f);
        v.setScaleX(scale);
        v.setScaleY(scale);
        v.setTranslationY(y);
        v.setRotation(startRotate);
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
            .rotation(0f).setStartDelay(delay).setDuration(duration).start();
    }

    public SplashOverlay(Activity activity, FrameLayout shell) {
        this.activity = activity;
        view = new FrameLayout(activity);
        view.setClickable(true);
        view.setFocusable(true);
        view.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(3, 12, 28), Color.rgb(11, 38, 66), Color.rgb(4, 19, 40)}));
        shell.addView(view, new FrameLayout.LayoutParams(-1, -1));

        // Thin cartographic contours give the map a premium topographic mood.
        view.addView(new ContourField(activity), new FrameLayout.LayoutParams(-1, -1));

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams position =
            new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        position.leftMargin = dp(14);
        position.rightMargin = dp(14);
        view.addView(content, position);

        FrameLayout stage = new FrameLayout(activity);
        content.addView(stage, new LinearLayout.LayoutParams(dp(310), dp(310)));

        // High-resolution official globe extracted from the same original
        // iMaps identity; then it gives way to the full exact IMAPS mark.
        ImageView globe = new ImageView(activity);
        globe.setImageResource(R.drawable.ic_launcher);
        globe.setScaleType(ImageView.ScaleType.FIT_CENTER);
        globe.setContentDescription("نقشه ایران");
        stage.addView(globe, new FrameLayout.LayoutParams(-1, -1));
        entrance(globe, 60, 1050, .78f, dp(8), -17f);
        globe.animate().alpha(0f).setStartDelay(1080).setDuration(480).start();

        ImageView complete = new ImageView(activity);
        complete.setImageResource(R.drawable.imaps_full_logo);
        complete.setScaleType(ImageView.ScaleType.FIT_CENTER);
        complete.setContentDescription("لوگوی اصلی IMAPS ایران مپس");
        stage.addView(complete, new FrameLayout.LayoutParams(-1, -1));
        entrance(complete, 1060, 680, .86f, dp(6), 0f);

        TextView name = text("IMAPS WebGIS", 24, Color.rgb(239, 182, 71), true);
        LinearLayout.LayoutParams nameLayout = new LinearLayout.LayoutParams(-1, dp(40));
        nameLayout.topMargin = -dp(4);
        content.addView(name, nameLayout);
        entrance(name, 1150, 560, .96f, dp(11), 0f);

        TextView subtitle = text("سامانه هوشمند استعلام سال‌های عکسبرداری هوایی",
            12, Color.rgb(207, 223, 234), false);
        content.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(34)));
        entrance(subtitle, 1390, 400, 1f, dp(6), 0f);

        TextView copyright = text("© iMaps 2026", 11, Color.rgb(163, 183, 201), false);
        FrameLayout.LayoutParams cp =
            new FrameLayout.LayoutParams(-1, dp(34), Gravity.BOTTOM);
        cp.bottomMargin = dp(14);
        view.addView(copyright, cp);

        // Never leave the user stuck on a decorative splash if the connection is slow.
        handler.postDelayed(fallback, 5500);
    }

    /** Static lightweight topographic lines. No bitmap manipulation or WebView changes. */
    private static final class ContourField extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path line = new Path();
        ContourField(Activity context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            if (w<=0 || h<=0) return;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f, getResources().getDisplayMetrics().density*.72f));
            for (int j=0;j<15;j++) {
                float y=h*(.18f + j*.048f);
                float offset=j*1.47f;
                p.setColor(Color.argb(j%4==0?49:23, 232, 175, 83));
                line.reset();
                for (int i=0;i<=60;i++) {
                    float x=w*i/60f;
                    float yy=y+(float)Math.sin(i*.21f+offset)*h*.018f
                        +(float)Math.sin(i*.075f+offset*2)*h*.015f;
                    if(i==0)line.moveTo(x,yy);else line.lineTo(x,yy);
                }
                c.drawPath(line,p);
            }
        }
    }

    public void onPageReady() {
        if(initialPageReady) return;
        initialPageReady = true;
        handler.removeCallbacks(fallback);
        long elapsed = SystemClock.uptimeMillis() - started;
        handler.postDelayed(pendingDismiss, Math.max(0, MIN_VISIBLE_MS - elapsed));
    }
    private void dismiss() {
        if (leaving) return;
        leaving=true;
        handler.removeCallbacksAndMessages(null);
        view.animate().alpha(0f).setDuration(230).withEndAction(() -> {
            if(view.getParent() instanceof ViewGroup)
                ((ViewGroup)view.getParent()).removeView(view);
        }).start();
    }
    public void destroy() {
        handler.removeCallbacksAndMessages(null);
        view.animate().cancel();
        if(view.getParent() instanceof ViewGroup)
            ((ViewGroup)view.getParent()).removeView(view);
    }
}
