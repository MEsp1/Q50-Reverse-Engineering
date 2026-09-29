package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.VehicleData;

/**
 * Вкладка 2: давления топлива на циферблатах, коррекции и наддув — плитками.
 *
 * Прибор LPFP стоит на своём месте по макету, но значения у него нет и не
 * будет, пока низкое давление не логируется: рисовать туда демо-цифру значит
 * показывать водителю то, чего машина не измеряет.
 */
public final class FuelPage implements Page {

    public String title() {
        return "ТОПЛИВО";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float r = 128f;
        float cy = 150f;
        Gauges.dial(c, t, w / 2f - 168f, cy, r, d.lpfp,
                0f, 10f, 1, 5, Float.NaN, "LPFP", "bar", 1f, 0);
        Gauges.dial(c, t, w / 2f + 168f, cy, r, d.hpfpActual,
                0f, 250f, 0, 5, Float.NaN, "HPFP", "bar", 1f, 0);

        if (!d.lpfp.hasValue()) {
            c.drawText("не логируется", w / 2f - 168f, cy + r * 0.72f,
                    t.text(Theme.VALUE_DIM, 12f, Paint.Align.CENTER, false));
        }

        float tw = (w - 28f - 30f) / 4f;
        float ty = h - 92f;
        float th = 78f;
        Gauges.tile(c, t, EnginePage.col(0, tw), ty, tw, th, Icons.FUEL, "КОРРЕКЦИЯ ТОПЛИВА",
                d.stftB1, 1, "%", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(1, tw), ty, tw, th, Icons.LAMBDA, "СМЕСЬ (AFR)",
                d.afrB1, 1, "", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(2, tw), ty, tw, th, Icons.TURBO, "НАДДУВ (ЦЕЛЬ)",
                d.boostTarget, 1, "bar", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(3, tw), ty, tw, th, Icons.TURBO, "НАДДУВ (ФАКТ)",
                d.boostActual, 1, "bar", Float.NaN, Float.NaN);
    }
}
