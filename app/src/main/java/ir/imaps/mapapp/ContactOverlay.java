package ir.imaps.mapapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Floating contact actions rendered by Android, not by WordPress.
 * Only the app shows these controls; the website remains unchanged.
 */
public final class ContactOverlay {
    private static final String PHONE_DIAL = "09213234340";
    private static final String PHONE_WHATSAPP = "989213234340";
    private static final int GREEN = Color.rgb(18, 151, 82);
    private static final int BLUE = Color.rgb(11, 82, 101);

    private ContactOverlay() {}

    private static int dp(Activity activity, float value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static GradientDrawable rounded(Activity a, int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(a, radius));
        return shape;
    }

    private static TextView button(Activity a, String title, int color) {
        TextView button = new TextView(a);
        button.setText(title);
        button.setTextColor(Color.WHITE);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(a, 13), dp(a, 7), dp(a, 13), dp(a, 7));
        button.setBackground(rounded(a, color, 24));
        button.setElevation(dp(a, 5));
        return button;
    }

    private static void openWhatsapp(Activity activity) {
        String deepLink = "whatsapp://send?phone=" + PHONE_WHATSAPP;
        for (String pkg : new String[]{"com.whatsapp", "com.whatsapp.w4b"}) {
            try {
                Intent in = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLink));
                in.setPackage(pkg);
                activity.startActivity(in);
                return;
            } catch (ActivityNotFoundException ignored) {
                // Try WhatsApp Business, then standard HTTPS link.
            } catch (SecurityException ignored) {
                // Continue with the public deep link fallback.
            }
        }
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/" + PHONE_WHATSAPP)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "مرورگری برای باز کردن واتساپ پیدا نشد", Toast.LENGTH_LONG).show();
        }
    }

    private static void openDialer(Activity activity) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + PHONE_DIAL)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "برنامه شماره گیر گوشی در دسترس نیست", Toast.LENGTH_LONG).show();
        }
    }

    public static void attach(Activity activity, FrameLayout content) {
        LinearLayout floating = new LinearLayout(activity);
        floating.setOrientation(LinearLayout.VERTICAL);
        floating.setGravity(Gravity.RIGHT);
        floating.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        FrameLayout.LayoutParams position = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM | Gravity.RIGHT);
        position.setMargins(dp(activity, 12), dp(activity, 12),
            dp(activity, 14), dp(activity, 36));
        content.addView(floating, position);

        LinearLayout options = new LinearLayout(activity);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setGravity(Gravity.RIGHT);
        options.setVisibility(View.GONE);
        floating.addView(options, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView whatsapp = button(activity, "پیام در واتساپ", GREEN);
        whatsapp.setContentDescription("ارسال پیام واتساپ به شماره پشتیبانی آیمپس");
        LinearLayout.LayoutParams waParams = new LinearLayout.LayoutParams(
            dp(activity, 145), dp(activity, 49));
        waParams.bottomMargin = dp(activity, 8);
        options.addView(whatsapp, waParams);
        whatsapp.setOnClickListener(v -> {
            options.setVisibility(View.GONE);
            openWhatsapp(activity);
        });

        TextView call = button(activity, "تماس تلفنی", BLUE);
        call.setContentDescription("تماس با شماره پشتیبانی آیمپس");
        LinearLayout.LayoutParams callParams = new LinearLayout.LayoutParams(
            dp(activity, 145), dp(activity, 49));
        callParams.bottomMargin = dp(activity, 8);
        options.addView(call, callParams);
        call.setOnClickListener(v -> {
            options.setVisibility(View.GONE);
            openDialer(activity);
        });

        TextView toggle = button(activity, "☎ ارتباط", BLUE);
        toggle.setTextSize(15);
        toggle.setContentDescription("باز کردن گزینه های واتساپ و تماس");
        toggle.setOnClickListener(v ->
            options.setVisibility(options.getVisibility() == View.VISIBLE
                ? View.GONE : View.VISIBLE));
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
            dp(activity, 108), dp(activity, 51));
        floating.addView(toggle, toggleParams);
    }
}
