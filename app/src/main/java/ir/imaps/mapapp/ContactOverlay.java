package ir.imaps.mapapp;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Native floating dial-only button, bottom-right.
 * The website's existing WhatsApp action on the left is not changed.
 */
public final class ContactOverlay {
    private static final String PHONE = "09213234340";
    private ContactOverlay() {}

    private static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
    public static void attach(Activity activity, FrameLayout content) {
        TextView call = new TextView(activity);
        call.setText("☎");
        call.setTextSize(29);
        call.setTextColor(Color.WHITE);
        call.setGravity(Gravity.CENTER);
        call.setContentDescription("تماس با پشتیبانی آیمپس");
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.rgb(11, 82, 101));
        bg.setStroke(dp(activity, 2), Color.rgb(193, 171, 99));
        call.setBackground(bg);
        call.setElevation(dp(activity, 5));
        FrameLayout.LayoutParams position = new FrameLayout.LayoutParams(
            dp(activity, 52), dp(activity, 52), Gravity.RIGHT | Gravity.BOTTOM);
        position.rightMargin = dp(activity, 14);
        position.bottomMargin = dp(activity, 36);
        content.addView(call, position);
        call.setOnClickListener(v -> {
            try {
                activity.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + PHONE)));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(activity, "شماره‌گیر گوشی در دسترس نیست", Toast.LENGTH_LONG).show();
            }
        });
    }
}
