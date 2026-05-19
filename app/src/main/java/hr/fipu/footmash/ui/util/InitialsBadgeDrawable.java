package hr.fipu.footmash.ui.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A self-contained circular badge that draws 1–2 initials over a colour seeded
 * deterministically from the source name. Replaces network-loaded crests so the
 * UI has a distinct visual per team / player / league without bundling licensed
 * artwork or hitting any image CDN.
 */
public class InitialsBadgeDrawable extends Drawable {

    private static final int[] PALETTE = {
            0xFF1E88E5, 0xFFD32F2F, 0xFF388E3C, 0xFFF57C00,
            0xFF7B1FA2, 0xFF00897B, 0xFFC2185B, 0xFF455A64,
            0xFF5E35B1, 0xFFE53935, 0xFF6D4C41, 0xFF0277BD
    };

    private final String initials;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public InitialsBadgeDrawable(String name) {
        this.initials = extractInitials(name);
        bgPaint.setColor(colorFromName(name));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    private static String extractInitials(String name) {
        if (name == null) return "?";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "?";
        String[] parts = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    private static int colorFromName(String name) {
        int hash = name == null ? 0 : name.hashCode();
        return PALETTE[Math.abs(hash % PALETTE.length)];
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        float cx = b.exactCenterX();
        float cy = b.exactCenterY();
        float radius = Math.min(b.width(), b.height()) / 2f;
        canvas.drawCircle(cx, cy, radius, bgPaint);

        textPaint.setTextSize(radius * 0.85f);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(initials, cx, baseline, textPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        bgPaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter cf) {
        bgPaint.setColorFilter(cf);
        textPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
