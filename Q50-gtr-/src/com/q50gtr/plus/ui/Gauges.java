package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;

/**
 * Круглые приборы и плитки в стиле штатной панели Q50: металлический обод,
 * тёмно-синий циферблат, белые риски с красной зоной, тонкая стрелка.
 */
public final class Gauges {

    /** Шкала идёт снизу-слева до снизу-справа, как в кластере. */
    private static final float ARC_START = 150f;
    private static final float ARC_SWEEP = 240f;

    private static final String NO_DATA = "НЕТ ДАННЫХ";

    private Gauges() {
    }

    /* ------------------------------------------------------------------ */
    /* круглый прибор                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * @param tickScale делитель подписей шкалы (1000 для «RPM x1000»)
     * @param redlineFrom начало красной зоны в единицах канала, NaN — нет зоны
     */
    public static void dial(Canvas c, Theme t, float cx, float cy, float r,
                            Channel ch, float min, float max, int decimals,
                            int majorTicks, float redlineFrom,
                            String caption, String subCaption, float tickScale,
                            int tickDecimals) {

        int save = c.save();
        c.translate(cx, cy);

        // Обод и циферблат рисуются в локальных координатах: так один комплект
        // шейдеров обслуживает все приборы панели.
        c.drawCircle(0f, 0f, r, t.bezel(r));
        c.drawCircle(0f, 0f, r * 0.94f, t.fill(Theme.DIAL_OUT));
        c.drawCircle(0f, 0f, r * 0.925f, t.dial(r));
        c.drawCircle(0f, 0f, r * 0.925f, t.stroke(0xFF223148, 1f));

        float trackR = r * 0.80f;
        t.rect.set(-trackR, -trackR, trackR, trackR);

        // Красная зона — дугой под рисками.
        if (!Float.isNaN(redlineFrom) && redlineFrom > min && redlineFrom < max) {
            float f = (redlineFrom - min) / (max - min);
            float from = ARC_START + ARC_SWEEP * f;
            c.drawArc(t.rect, from, ARC_START + ARC_SWEEP - from, false,
                    t.stroke(Theme.RED, r * 0.055f));
        }

        // Мелкие риски.
        float tickOuter = trackR + r * 0.03f;
        int minorSteps = majorTicks * 2;
        for (int i = 0; i <= minorSteps; i++) {
            if (i % 2 == 0) {
                continue;
            }
            float f = (float) i / minorSteps;
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cos * (tickOuter - r * 0.055f), sin * (tickOuter - r * 0.055f),
                    cos * tickOuter, sin * tickOuter, t.stroke(Theme.TICK_MINOR, 1.6f));
        }

        // Крупные риски и подписи шкалы.
        Paint tickText = t.text(Theme.TICK, r * 0.115f, Paint.Align.CENTER, false);
        for (int i = 0; i <= majorTicks; i++) {
            float f = (float) i / majorTicks;
            float value = min + (max - min) * f;
            boolean hot = !Float.isNaN(redlineFrom) && value >= redlineFrom - 1e-4f;
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cos * (tickOuter - r * 0.105f), sin * (tickOuter - r * 0.105f),
                    cos * tickOuter, sin * tickOuter,
                    t.stroke(hot ? Theme.RED : Theme.TICK, 3f));
            float lr = tickOuter - r * 0.185f;
            tickText.setColor(hot ? Theme.RED : Theme.TICK);
            c.drawText(Channel.format(value / tickScale, tickDecimals),
                    cos * lr, sin * lr + r * 0.05f, tickText);
        }

        // Название прибора и подпись единиц — по центру над осью.
        c.drawText(caption, 0f, -r * 0.34f,
                t.text(Theme.WHITE, r * 0.135f, Paint.Align.CENTER, false));
        if (subCaption != null) {
            c.drawText(subCaption, 0f, -r * 0.20f,
                    t.text(Theme.LABEL, r * 0.095f, Paint.Align.CENTER, false));
        }

        boolean has = ch.hasValue();
        float f = 0f;
        boolean hot = false;
        if (has) {
            f = (ch.getValue() - min) / (max - min);
            f = f < 0f ? 0f : (f > 1f ? 1f : f);
            hot = !Float.isNaN(redlineFrom) && ch.getValue() >= redlineFrom;
            
            // Активная дуга (Neon sweep)
            c.drawArc(t.rect, ARC_START, ARC_SWEEP * f, false,
                    t.stroke(hot ? Theme.RED : Theme.ACCENT, r * 0.045f));
        }

        // Значение — крупно в нижней части циферблата.
        if (has) {
            c.drawText(ch.text(decimals), 0f, r * 0.47f,
                    t.text(hot ? Theme.RED : Theme.ACCENT, r * 0.27f, Paint.Align.CENTER, true));
        } else {
            c.drawText(NO_DATA, 0f, r * 0.42f,
                    t.text(Theme.VALUE_DIM, r * 0.13f, Paint.Align.CENTER, false));
        }
        if (ch.isDemo()) {
            c.drawText("DEMO", 0f, r * 0.70f,
                    t.text(Theme.ACCENT, r * 0.095f, Paint.Align.CENTER, true));
        }

        // Стрелка поверх всего.
        if (has) {
            float a = (float) Math.toRadians(ARC_START + ARC_SWEEP * f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            // Стрелка не доходит до подписей шкалы: иначе она их перечёркивает.
            c.drawLine(-cos * r * 0.15f, -sin * r * 0.15f,
                    cos * r * 0.55f, sin * r * 0.55f,
                    t.stroke(hot ? Theme.RED : Theme.ACCENT, r * 0.026f));
            c.drawCircle(0f, 0f, r * 0.085f, t.fill(Theme.BEZEL_LO));
            c.drawCircle(0f, 0f, r * 0.055f, t.fill(Theme.DIAL_OUT));
        }

        c.restoreToCount(save);
    }

    /* ------------------------------------------------------------------ */
    /* плитки                                                              */
    /* ------------------------------------------------------------------ */

    private static void panel(Canvas c, Theme t, float x, float y, float w, float h) {
        t.rect.set(x, y, x + w, y + h);
        int save = c.save();
        c.translate(x, y);
        t.rect2.set(0f, 0f, w, h);
        c.drawRoundRect(t.rect2, 5f, 5f, t.tile(h));
        c.restoreToCount(save);
        c.drawRoundRect(t.rect, 5f, 5f, t.stroke(Theme.TILE_EDGE, 1f));
    }

    private static int colorFor(Channel ch, float warnFrom, float alertFrom) {
        if (!ch.hasValue()) {
            return Theme.VALUE_DIM;
        }
        float v = ch.getValue();
        if (!Float.isNaN(alertFrom) && v >= alertFrom) {
            return Theme.RED;
        }
        if (!Float.isNaN(warnFrom) && v >= warnFrom) {
            return Theme.WARN;
        }
        
        // Neon Racing style per channel type
        if (ch.id.startsWith("hev") || ch.id.startsWith("ev") || ch.id.startsWith("regen") || ch.id.startsWith("combined")) {
            return Theme.ACCENT;
        }
        if (ch.id.startsWith("trip") || ch.id.startsWith("fuel")) {
            return Theme.GREEN;
        }
        return Theme.WHITE;
    }

    /** Значение с единицей: значение крупно, единица мелко следом. */
    private static void value(Canvas c, Theme t, float x, float baseline,
                              Channel ch, int decimals, String unit, int color, float size) {
        if (!ch.hasValue()) {
            c.drawText("—", x, baseline,
                    t.text(Theme.VALUE_DIM, size, Paint.Align.LEFT, true));
            return;
        }
        String s = ch.text(decimals);
        Paint p = t.text(color, size, Paint.Align.LEFT, true);
        float vw = p.measureText(s);
        c.drawText(s, x, baseline, p);
        if (unit != null && unit.length() > 0) {
            c.drawText(unit, x + vw + size * 0.16f, baseline,
                    t.text(Theme.LABEL, size * 0.46f, Paint.Align.LEFT, false));
        }
    }

    /** Плитка нижнего ряда: подпись сверху, под ней иконка и значение. */
    public static void tile(Canvas c, Theme t, float x, float y, float w, float h,
                            int icon, String label, Channel ch, int decimals,
                            String unit, float warnFrom, float alertFrom) {
        panel(c, t, x, y, w, h);
        c.drawText(label, x + 11f, y + 19f,
                t.text(Theme.LABEL, 11.5f, Paint.Align.LEFT, false));
        if (ch.isDemo()) {
            c.drawText("DEMO", x + w - 10f, y + 19f,
                    t.text(Theme.ACCENT, 9.5f, Paint.Align.RIGHT, true));
        }
        float iconSize = Math.min(26f, h * 0.36f);
        float iconY = y + h - iconSize - 11f;
        Icons.draw(c, t, icon, x + 10f, iconY, iconSize, Theme.LABEL);
        value(c, t, x + 10f + iconSize + 9f, y + h - 14f, ch, decimals, unit,
                colorFor(ch, warnFrom, alertFrom), Math.min(27f, h * 0.36f));
    }

    /** Плитка колонки: иконка слева, справа подпись и значение. */
    public static void tileRow(Canvas c, Theme t, float x, float y, float w, float h,
                               int icon, String label, Channel ch, int decimals,
                               String unit, float warnFrom, float alertFrom) {
        panel(c, t, x, y, w, h);
        float iconSize = Math.min(24f, h * 0.48f);
        Icons.draw(c, t, icon, x + 10f, y + (h - iconSize) / 2f, iconSize, Theme.LABEL);
        float tx = x + 10f + iconSize + 10f;
        c.drawText(label, tx, y + h * 0.40f,
                t.text(Theme.LABEL, 11f, Paint.Align.LEFT, false));
        value(c, t, tx, y + h * 0.84f, ch, decimals, unit,
                colorFor(ch, warnFrom, alertFrom), 21f);
        if (ch.isDemo()) {
            c.drawText("DEMO", x + w - 8f, y + h * 0.40f,
                    t.text(Theme.ACCENT, 9f, Paint.Align.RIGHT, true));
        }
    }
}
