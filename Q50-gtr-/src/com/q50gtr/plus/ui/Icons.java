package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Пиктограммы плиток. Рисуются линиями, а не картинками: на ГУ нет ни места
 * под растр под каждую плотность, ни желания его декодировать на каждом кадре.
 *
 * Каждая рисуется в квадрате size x size с левым верхним углом в (x, y).
 */
public final class Icons {

    public static final int COOLANT = 0;
    public static final int OIL_TEMP = 1;
    public static final int OIL_PRESS = 2;
    public static final int INTAKE = 3;
    public static final int FUEL = 4;
    public static final int LAMBDA = 5;
    public static final int TURBO = 6;
    public static final int GEARBOX = 7;
    public static final int BATTERY = 8;
    public static final int AMBIENT = 9;
    public static final int SPARK = 10;
    public static final int KNOCK = 11;
    public static final int THROTTLE = 12;
    public static final int SPEED = 13;

    private Icons() {
    }

    public static void draw(Canvas c, Theme t, int icon, float x, float y, float size, int color) {
        float cx = x + size / 2f;
        float cy = y + size / 2f;
        float w = size * 0.08f;
        switch (icon) {
            case COOLANT:   thermometer(c, t, cx, cy, size, color, w, true);  break;
            case OIL_TEMP:  drop(c, t, cx, cy, size, color, w, true);         break;
            case OIL_PRESS: drop(c, t, cx, cy, size, color, w, false);        break;
            case INTAKE:    thermometer(c, t, cx, cy, size, color, w, false); break;
            case FUEL:      pump(c, t, cx, cy, size, color, w);               break;
            case LAMBDA:    lambda(c, t, cx, cy, size, color);                break;
            case TURBO:     turbo(c, t, cx, cy, size, color, w);              break;
            case GEARBOX:   gear(c, t, cx, cy, size, color, w);               break;
            case BATTERY:   battery(c, t, cx, cy, size, color, w);            break;
            case AMBIENT:   thermometer(c, t, cx, cy, size, color, w, false); break;
            case SPARK:     spark(c, t, cx, cy, size, color, w);              break;
            case KNOCK:     engine(c, t, cx, cy, size, color, w);             break;
            case THROTTLE:  throttle(c, t, cx, cy, size, color, w);           break;
            case SPEED:     speedo(c, t, cx, cy, size, color, w);             break;
            default: break;
        }
    }

    private static void thermometer(Canvas c, Theme t, float cx, float cy, float s,
                                    int color, float w, boolean waves) {
        float h = s * 0.46f;
        float bulb = s * 0.15f;
        float stemX = waves ? cx + s * 0.12f : cx;
        c.drawLine(stemX, cy - h, stemX, cy + h * 0.35f, t.stroke(color, w * 1.6f));
        c.drawCircle(stemX, cy + h * 0.55f, bulb, t.fill(color));
        for (int i = 0; i < 3; i++) {
            float ty = cy - h + s * 0.12f * (i + 1);
            c.drawLine(stemX - s * 0.16f, ty, stemX - s * 0.06f, ty, t.stroke(color, w));
        }
        if (waves) {
            // Волны воды — отличают охлаждающую жидкость от воздуха.
            float bx = cx - s * 0.30f;
            for (int i = 0; i < 2; i++) {
                float wy = cy - s * 0.02f + i * s * 0.18f;
                Path p = t.path;
                p.reset();
                p.moveTo(bx - s * 0.12f, wy);
                p.rQuadTo(s * 0.06f, -s * 0.07f, s * 0.12f, 0f);
                p.rQuadTo(s * 0.06f, s * 0.07f, s * 0.12f, 0f);
                c.drawPath(p, t.stroke(color, w));
            }
        }
    }

    private static void drop(Canvas c, Theme t, float cx, float cy, float s,
                             int color, float w, boolean temp) {
        Path p = t.path;
        p.reset();
        float r = s * 0.22f;
        float top = cy - s * 0.30f;
        p.moveTo(cx, top);
        p.quadTo(cx + r * 1.5f, cy - r * 0.1f, cx + r, cy + r * 0.5f);
        p.quadTo(cx, cy + r * 1.6f, cx - r, cy + r * 0.5f);
        p.quadTo(cx - r * 1.5f, cy - r * 0.1f, cx, top);
        p.close();
        c.drawPath(p, t.stroke(color, w * 1.4f));
        if (temp) {
            c.drawLine(cx - s * 0.06f, cy + s * 0.05f, cx + s * 0.06f, cy + s * 0.05f,
                    t.stroke(color, w));
        } else {
            c.drawCircle(cx, cy + s * 0.05f, s * 0.05f, t.fill(color));
        }
    }

    private static void pump(Canvas c, Theme t, float cx, float cy, float s, int color, float w) {
        float bw = s * 0.30f, bh = s * 0.52f;
        t.rect.set(cx - bw * 0.9f, cy - bh / 2f, cx + bw * 0.2f, cy + bh / 2f);
        c.drawRoundRect(t.rect, s * 0.05f, s * 0.05f, t.stroke(color, w * 1.4f));
        c.drawLine(cx - bw * 0.75f, cy - bh * 0.18f, cx + bw * 0.05f, cy - bh * 0.18f,
                t.stroke(color, w));
        c.drawLine(cx + bw * 0.2f, cy + bh * 0.4f, cx + bw * 0.6f, cy + bh * 0.4f,
                t.stroke(color, w));
        c.drawLine(cx + bw * 0.6f, cy + bh * 0.4f, cx + bw * 0.6f, cy - bh * 0.2f,
                t.stroke(color, w));
    }

    private static void lambda(Canvas c, Theme t, float cx, float cy, float s, int color) {
        Paint p = t.text(color, s * 0.92f, Paint.Align.CENTER, false);
        c.drawText("λ", cx, cy + s * 0.33f, p);
    }

    private static void turbo(Canvas c, Theme t, float cx, float cy, float s, int color, float w) {
        float r = s * 0.30f;
        c.drawCircle(cx, cy, r, t.stroke(color, w * 1.4f));
        c.drawCircle(cx, cy, s * 0.07f, t.fill(color));
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6.0;
            float sx = cx + (float) Math.cos(a) * s * 0.10f;
            float sy = cy + (float) Math.sin(a) * s * 0.10f;
            float ex = cx + (float) Math.cos(a) * r * 0.92f;
            float ey = cy + (float) Math.sin(a) * r * 0.92f;
            c.drawLine(sx, sy, ex, ey, t.stroke(color, w));
        }
    }

    private static void gear(Canvas c, Theme t, float cx, float cy, float s, int color, float w) {
        float r = s * 0.28f;
        c.drawCircle(cx, cy, r, t.stroke(color, w * 1.4f));
        c.drawCircle(cx, cy, r * 0.42f, t.stroke(color, w));
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8.0;
            float sx = cx + (float) Math.cos(a) * r;
            float sy = cy + (float) Math.sin(a) * r;
            float ex = cx + (float) Math.cos(a) * (r + s * 0.09f);
            float ey = cy + (float) Math.sin(a) * (r + s * 0.09f);
            c.drawLine(sx, sy, ex, ey, t.stroke(color, w * 1.3f));
        }
    }

    private static void battery(Canvas c, Theme t, float cx, float cy, float s, int color, float w) {
        float bw = s * 0.62f, bh = s * 0.40f;
        t.rect.set(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f);
        c.drawRoundRect(t.rect, s * 0.05f, s * 0.05f, t.stroke(color, w * 1.4f));
        c.drawLine(cx - bw * 0.22f, cy - bh * 0.72f, cx - bw * 0.22f, cy - bh / 2f, t.stroke(color, w * 1.6f));
        c.drawLine(cx + bw * 0.22f, cy - bh * 0.72f, cx + bw * 0.22f, cy - bh / 2f, t.stroke(color, w * 1.6f));
        c.drawLine(cx - bw * 0.26f, cy, cx - bw * 0.06f, cy, t.stroke(color, w));
        c.drawLine(cx - bw * 0.16f, cy - bh * 0.22f, cx - bw * 0.16f, cy + bh * 0.22f, t.stroke(color, w));
        c.drawLine(cx + bw * 0.06f, cy, cx + bw * 0.26f, cy, t.stroke(color, w));
    }

    private static void spark(Canvas c, Theme t, float cx, float cy, float s, int color, float w) {
        Path p = t.path;
        p.reset();
        p.moveTo(cx + s * 0.10f, cy - s * 0.34f);
        p.lineTo(cx - s * 0.16f, cy + s * 0.04f);
        p.lineTo(cx + s * 0.02f, cy + s * 0.04f);
        p.lineTo(cx - s * 0.08f, cy + s * 0.34f);
        p.lineTo(cx + s * 0.18f, cy - s * 0.06f);
        p.lineTo(cx, cy - s * 0.06f);
        p.close();
        c.drawPath(p, t.stroke(color, w * 1.3f));
    }

    private static void engine(Canvas c, Theme t, float cx, float cy, float s, int color, float w) {
        t.rect.set(cx - s * 0.30f, cy - s * 0.16f, cx + s * 0.22f, cy + s * 0.26f);
        c.drawRoundRect(t.rect, s * 0.05f, s * 0.05f, t.stroke(color, w * 1.4f));
        c.drawLine(cx - s * 0.16f, cy - s * 0.16f, cx - s * 0.16f, cy - s * 0.30f, t.stroke(color, w));
        c.drawLine(cx - s * 0.02f, cy - s * 0.16f, cx - s * 0.02f, cy - s * 0.30f, t.stroke(color, w));
        c.drawLine(cx + s * 0.22f, cy - s * 0.02f, cx + s * 0.34f, cy - s * 0.02f, t.stroke(color, w));
        c.drawLine(cx + s * 0.34f, cy - s * 0.02f, cx + s * 0.34f, cy + s * 0.14f, t.stroke(color, w));
    }

    private static void throttle(Canvas c, Theme t, float cx, float cy, float s, int color, float w) {
        float r = s * 0.30f;
        c.drawCircle(cx, cy, r, t.stroke(color, w * 1.4f));
        c.drawLine(cx - r * 0.72f, cy + r * 0.52f, cx + r * 0.72f, cy - r * 0.52f,
                t.stroke(color, w * 1.5f));
        c.drawCircle(cx, cy, s * 0.05f, t.fill(color));
    }

    private static void speedo(Canvas c, Theme t, float cx, float cy, float s, int color, float w) {
        float r = s * 0.30f;
        t.rect.set(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(t.rect, 160f, 220f, false, t.stroke(color, w * 1.4f));
        c.drawLine(cx, cy, cx + r * 0.62f, cy - r * 0.48f, t.stroke(color, w * 1.4f));
        c.drawCircle(cx, cy, s * 0.05f, t.fill(color));
    }
}
