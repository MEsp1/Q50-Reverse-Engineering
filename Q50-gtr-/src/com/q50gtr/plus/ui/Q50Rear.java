package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.q50gtr.plus.data.Channel;

/**
 * Q50 сзади и четыре пневмостойки вокруг — так, как это показано на
 * приборной панели: машина по центру, давления по углам.
 */
public final class Q50Rear {

    private Q50Rear() {
    }

    /** Кузов вписывается в прямоугольник (x, y, w, h). */
    public static void car(Canvas c, Theme t, float x, float y, float w, float h) {
        float cx = x + w / 2f;
        Path p = t.path;

        // Крыша и стекло.
        p.reset();
        float roofW = w * 0.52f;
        float glassW = w * 0.60f;
        p.moveTo(cx - roofW / 2f, y + h * 0.10f);
        p.quadTo(cx, y + h * 0.02f, cx + roofW / 2f, y + h * 0.10f);
        p.lineTo(cx + glassW / 2f, y + h * 0.40f);
        p.lineTo(cx - glassW / 2f, y + h * 0.40f);
        p.close();
        c.drawPath(p, t.fill(0xFF0E1826));
        c.drawPath(p, t.stroke(0xFF39485C, 1.4f));

        // Кузов.
        p.reset();
        float bodyW = w * 0.86f;
        p.moveTo(cx - glassW / 2f - w * 0.02f, y + h * 0.40f);
        p.lineTo(cx + glassW / 2f + w * 0.02f, y + h * 0.40f);
        p.quadTo(cx + bodyW / 2f, y + h * 0.46f, cx + bodyW / 2f, y + h * 0.62f);
        p.lineTo(cx + bodyW / 2f, y + h * 0.84f);
        p.quadTo(cx + bodyW / 2f, y + h * 0.92f, cx + bodyW / 2f - w * 0.06f, y + h * 0.92f);
        p.lineTo(cx - bodyW / 2f + w * 0.06f, y + h * 0.92f);
        p.quadTo(cx - bodyW / 2f, y + h * 0.92f, cx - bodyW / 2f, y + h * 0.84f);
        p.lineTo(cx - bodyW / 2f, y + h * 0.62f);
        p.quadTo(cx - bodyW / 2f, y + h * 0.46f, cx - glassW / 2f - w * 0.02f, y + h * 0.40f);
        p.close();
        c.drawPath(p, t.fill(0xFF141E2C));
        c.drawPath(p, t.stroke(0xFF4A5A70, 1.5f));

        // Фонари.
        float ly = y + h * 0.56f;
        float lh = h * 0.09f;
        t.rect.set(cx - bodyW / 2f + w * 0.03f, ly, cx - bodyW * 0.16f, ly + lh);
        c.drawRoundRect(t.rect, lh / 2f, lh / 2f, t.fill(0xFFB02A28));
        t.rect.set(cx + bodyW * 0.16f, ly, cx + bodyW / 2f - w * 0.03f, ly + lh);
        c.drawRoundRect(t.rect, lh / 2f, lh / 2f, t.fill(0xFFB02A28));

        // Номерной знак и выхлоп.
        t.rect.set(cx - w * 0.11f, y + h * 0.72f, cx + w * 0.11f, y + h * 0.82f);
        c.drawRoundRect(t.rect, 2f, 2f, t.stroke(0xFF5A6A80, 1.2f));
        c.drawLine(cx - bodyW * 0.30f, y + h * 0.95f, cx - bodyW * 0.20f, y + h * 0.95f,
                t.stroke(0xFF55637A, 3f));
        c.drawLine(cx + bodyW * 0.20f, y + h * 0.95f, cx + bodyW * 0.30f, y + h * 0.95f,
                t.stroke(0xFF55637A, 3f));

        // Эмблема.
        c.drawCircle(cx, y + h * 0.62f, w * 0.045f, t.stroke(0xFF8A94A4, 1.2f));
        c.drawLine(cx - w * 0.032f, y + h * 0.62f, cx + w * 0.032f, y + h * 0.62f,
                t.stroke(0xFF8A94A4, 1.2f));
    }

    /**
     * Пневмостойка с давлением. {@code labelLeft} разводит подпись по ту
     * сторону стойки, где для неё есть место.
     */
    public static void strut(Canvas c, Theme t, float cx, float cy, float h,
                             Channel ch, boolean labelLeft) {
        float top = cy - h / 2f;
        float bottom = cy + h / 2f;
        float w = h * 0.30f;

        // Шток и опоры.
        c.drawLine(cx, top, cx, top + h * 0.16f, t.stroke(0xFF98A2B2, 3f));
        t.rect.set(cx - w * 0.62f, top, cx + w * 0.62f, top + h * 0.06f);
        c.drawRoundRect(t.rect, 2f, 2f, t.fill(0xFF8A94A4));
        t.rect.set(cx - w * 0.62f, bottom - h * 0.06f, cx + w * 0.62f, bottom);
        c.drawRoundRect(t.rect, 2f, 2f, t.fill(0xFF8A94A4));

        // Витки пневмобаллона.
        int coils = 5;
        float cTop = top + h * 0.18f;
        float cBottom = bottom - h * 0.10f;
        float step = (cBottom - cTop) / coils;
        for (int i = 0; i < coils; i++) {
            float yy = cTop + step * i;
            t.rect.set(cx - w / 2f, yy, cx + w / 2f, yy + step * 1.35f);
            c.drawArc(t.rect, 0f, 180f, false, t.stroke(0xFF7E8A9B, 2.4f));
        }

        // Давление подписывается рядом со стойкой.
        String text = ch.hasValue() ? ch.text(1) : "—";
        Paint p = t.text(ch.hasValue() ? Theme.WHITE : Theme.VALUE_DIM, 22f,
                labelLeft ? Paint.Align.RIGHT : Paint.Align.LEFT, true);
        float tx = labelLeft ? cx - w * 0.95f : cx + w * 0.95f;
        c.drawText(text, tx, cy + 8f, p);
    }
}
