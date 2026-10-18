package ir.imaps.mapapp;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * iMaps introduction, deliberately implemented as a transparent, GPU-animated
 * Android sequence rather than a network-dependent video/WebView animation.
 *
 * Phase 1: translucent Google-style search interface types "iMaps".
 * Phase 2: exact Iran shape from the company logo rises into view.
 * Phase 3: remaining globe and genuine IMAPS / IRAN MAPS letters assemble.
 *
 * All three authentic logo layers are pre-rendered ONCE at build time from
 * the high resolution original PNG. No per-pixel processing at startup.
 */
public final class SplashOverlay {
    private static final long INTRO_MS = 3950L;
    private static final long SAFETY_MS = 7800L;
    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final FrameLayout overlay;
    private final long started = SystemClock.uptimeMillis();
    private boolean pageReady = false;
    private boolean dismissed = false;

    private int dp(float n) {
        return Math.round(n * activity.getResources().getDisplayMetrics().density);
    }
    private TextView text(String label, int size, int color, boolean bold) {
        TextView v = new TextView(activity);
        v.setText(label);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        v.setGravity(Gravity.CENTER);
        if (bold) v.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return v;
    }
    private GradientDrawable rounded(int fill, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private void appear(View v, long delay, long duration, float initialScale,
                        float translateY, float angle) {
        v.setAlpha(0f);
        v.setScaleX(initialScale);
        v.setScaleY(initialScale);
        v.setTranslationY(translateY);
        v.setRotation(angle);
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
            .rotation(0f).setStartDelay(delay).setDuration(duration)
            .setInterpolator(new DecelerateInterpolator(1.2f)).start();
    }

    public SplashOverlay(Activity activity, FrameLayout appShell) {
        this.activity = activity;
        overlay = new FrameLayout(activity);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(5, 15, 32), Color.rgb(10, 38, 60), Color.rgb(3, 13, 29)}));
        appShell.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        // Very faint contour-lines backdrop; static rendering, no busy movement.
        overlay.addView(new ContourLines(activity), new FrameLayout.LayoutParams(-1, -1));

        FrameLayout stage = new FrameLayout(activity);
        FrameLayout.LayoutParams stageLayout =
            new FrameLayout.LayoutParams(-1, dp(400), Gravity.CENTER);
        stageLayout.leftMargin = dp(20);
        stageLayout.rightMargin = dp(20);
        overlay.addView(stage, stageLayout);

        // --- First act: a cinematic glass-like browser and search bar.
        LinearLayout search = new LinearLayout(activity);
        search.setOrientation(LinearLayout.VERTICAL);
        search.setGravity(Gravity.CENTER_HORIZONTAL);
        search.setPadding(dp(18), dp(17), dp(18), dp(24));
        search.setBackground(rounded(Color.argb(226, 14, 35, 58), 22,
            Color.argb(65, 211, 224, 240)));
        search.setElevation(dp(11));
        FrameLayout.LayoutParams searchLayout =
            new FrameLayout.LayoutParams(-1, dp(256), Gravity.CENTER);
        stage.addView(search, searchLayout);

        TextView browser = text("●  ●  ●          google.com", 12,
            Color.rgb(158, 174, 195), false);
        browser.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        browser.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        search.addView(browser, new LinearLayout.LayoutParams(-1, dp(30)));

        View hairline = new View(activity);
        hairline.setBackgroundColor(Color.argb(55, 255, 255, 255));
        LinearLayout.LayoutParams hairlineLP =
            new LinearLayout.LayoutParams(-1, dp(1));
        hairlineLP.topMargin = dp(4);
        search.addView(hairline, hairlineLP);

        TextView google = text("Google", 40, Color.WHITE, true);
        google.setLetterSpacing(-0.06f);
        google.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        SpannableString colored = new SpannableString("Google");
        int[] palette = {
            Color.rgb(66,133,244), Color.rgb(234,67,53),
            Color.rgb(251,188,5), Color.rgb(66,133,244),
            Color.rgb(52,168,83), Color.rgb(234,67,53)
        };
        for (int i=0;i<6;i++) {
            colored.setSpan(new ForegroundColorSpan(palette[i]), i, i+1,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        google.setText(colored);
        LinearLayout.LayoutParams googleLP =
            new LinearLayout.LayoutParams(-1, dp(72));
        googleLP.topMargin = dp(13);
        search.addView(google, googleLP);

        LinearLayout field = new LinearLayout(activity);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setOrientation(LinearLayout.HORIZONTAL);
        field.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        field.setPadding(dp(14), 0, dp(11), 0);
        field.setBackground(rounded(Color.argb(210, 247, 250, 255), 28,
            Color.argb(170, 227, 233, 240)));
        search.addView(field, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView magnifier = text("⌕", 30, Color.rgb(71, 87, 113), false);
        field.addView(magnifier, new LinearLayout.LayoutParams(dp(33), dp(45)));
        TextView typing = text("│", 19, Color.rgb(23, 55, 83), true);
        typing.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        typing.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        field.addView(typing, new LinearLayout.LayoutParams(0, dp(45), 1f));
        TextView enter = text("↵", 24, Color.rgb(30, 114, 130), true);
        field.addView(enter, new LinearLayout.LayoutParams(dp(32), dp(44)));

        TextView searchNote = text("SEARCH  •  IMAPS", 11,
            Color.rgb(168, 185, 204), false);
        searchNote.setLetterSpacing(.16f);
        LinearLayout.LayoutParams noteLP =
            new LinearLayout.LayoutParams(-1, dp(26));
        noteLP.topMargin = dp(9);
        search.addView(searchNote, noteLP);

        // Only five UI text updates; no loop in the rendering thread.
        final String query = "iMaps";
        for (int n=1;n<=query.length();n++) {
            final int len = n;
            main.postDelayed(() -> typing.setText(query.substring(0,len) + "│"),
                390L + n*160L);
        }
        main.postDelayed(() -> typing.setText(query + "  ↵"), 1440);
        appear(search, 80, 510, .94f, dp(12), 0f);
        main.postDelayed(() -> search.animate().alpha(0f).scaleX(.94f)
            .scaleY(.94f).translationY(-dp(16)).setDuration(380)
            .withEndAction(() -> search.setVisibility(View.GONE)).start(), 1670);

        // --- Second act: the exact map, then the golden globe, then typography.
        LinearLayout logoGroup = new LinearLayout(activity);
        logoGroup.setOrientation(LinearLayout.VERTICAL);
        logoGroup.setGravity(Gravity.CENTER);
        logoGroup.setAlpha(1f);
        stage.addView(logoGroup, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        FrameLayout logoStage = new FrameLayout(activity);
        logoGroup.addView(logoStage, new LinearLayout.LayoutParams(dp(286), dp(286)));

        ImageView iran = logoLayer(logoStage, R.drawable.imaps_intro_iran,
            "نقشه ایران از لوگوی اصلی");
        ImageView globe = logoLayer(logoStage, R.drawable.imaps_intro_globe,
            "کره طلایی اصلی آیمپس");
        ImageView letters = logoLayer(logoStage, R.drawable.imaps_intro_letters,
            "حروف اصلی IMAPS IRAN MAPS");

        appear(iran, 1920, 680, .70f, dp(18), -13f);
        appear(globe, 2360, 760, .90f, dp(12), 6f);
        appear(letters, 2920, 700, .90f, dp(10), 0f);

        TextView name = text("IMAPS WebGIS", 23, Color.rgb(242, 194, 105), true);
        name.setLetterSpacing(.035f);
        logoGroup.addView(name, new LinearLayout.LayoutParams(-1, dp(42)));
        appear(name, 3060, 590, .95f, dp(9), 0f);

        TextView subtitle = text("سامانه هوشمند استعلام عکس‌های هوایی",
            12, Color.rgb(214, 226, 235), false);
        logoGroup.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(32)));
        appear(subtitle, 3270, 490, 1f, dp(7), 0f);

        TextView footer = text("© iMaps 2026", 11, Color.rgb(165, 184, 200), false);
        FrameLayout.LayoutParams footerLayout =
            new FrameLayout.LayoutParams(-1, dp(40), Gravity.BOTTOM);
        footerLayout.bottomMargin = dp(15);
        overlay.addView(footer, footerLayout);

        // Intro is finite even if website/network is unavailable.
        main.postDelayed(this::dismiss, SAFETY_MS);
    }

    private ImageView logoLayer(FrameLayout group, int id, String description) {
        ImageView image = new ImageView(activity);
        image.setImageResource(id);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(description);
        group.addView(image, new FrameLayout.LayoutParams(-1, -1));
        return image;
    }

    private static final class ContourLines extends View {
        private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path curve = new Path();
        ContourLines(Activity a) { super(a); }
        @Override public void onDraw(Canvas canvas) {
            int w=getWidth(), h=getHeight();
            if (w==0||h==0) return;
            pen.setStyle(Paint.Style.STROKE);
            pen.setStrokeWidth(getResources().getDisplayMetrics().density*.65f);
            for (int line=0;line<14;line++) {
                float y=h*(.14f+line*.054f);
                pen.setColor(Color.argb(line%3==0?27:14, 224, 183, 105));
                curve.reset();
                for (int x=0;x<=46;x++) {
                    float xx=x*w/46f;
                    float yy=y+(float)Math.sin(x*.24f+line*.93f)*h*.017f
                        +(float)Math.sin(x*.1f+line*1.32f)*h*.012f;
                    if (x==0)curve.moveTo(xx,yy);else curve.lineTo(xx,yy);
                }
                canvas.drawPath(curve,pen);
            }
        }
    }

    public void onPageReady() {
        if (pageReady || dismissed) return;
        pageReady = true;
        long elapsed = SystemClock.uptimeMillis()-started;
        main.postDelayed(this::dismiss, Math.max(0, INTRO_MS-elapsed));
    }

    private void dismiss() {
        if (dismissed) return;
        dismissed = true;
        main.removeCallbacksAndMessages(null);
        overlay.animate().alpha(0f).setDuration(260).withEndAction(() -> {
            ViewGroup p = (ViewGroup) overlay.getParent();
            if(p!=null)p.removeView(overlay);
        }).start();
    }

    public void destroy() {
        main.removeCallbacksAndMessages(null);
        overlay.animate().cancel();
        ViewGroup p=(ViewGroup)overlay.getParent();
        if(p!=null)p.removeView(overlay);
    }
}
