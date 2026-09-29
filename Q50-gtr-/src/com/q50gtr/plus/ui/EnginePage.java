package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/** Вкладка 1: обороты и наддув на циферблатах, температуры и давление — плитками. */
public final class EnginePage implements Page {

    public String title() {
        return "ДВИГАТЕЛЬ";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float r = 128f;
        float cy = 150f;
        Gauges.dial(c, t, w / 2f - 168f, cy, r, d.rpm,
                0f, 8000f, 0, 8, 6800f, "RPM", "x1000", 1000f, 0);
        Gauges.dial(c, t, w / 2f + 168f, cy, r, d.boostActual,
                -1f, 2f, 2, 6, 1.8f, "BOOST", "bar", 1f, 1);

        float tw = (w - 28f - 30f) / 4f;
        float ty = h - 92f;
        float th = 78f;
        Gauges.tile(c, t, col(0, tw), ty, tw, th, Icons.COOLANT, "ОХЛ. ЖИДКОСТЬ",
                d.coolantTemp, 0, "°C", 105f, 112f);
        Gauges.tile(c, t, col(1, tw), ty, tw, th, Icons.OIL_TEMP, "ТЕМП. МАСЛА",
                d.oilTemp, 0, "°C", 125f, 138f);
        Gauges.tile(c, t, col(2, tw), ty, tw, th, Icons.OIL_PRESS, "ДАВЛ. МАСЛА",
                d.oilPressure, 1, "bar", Float.NaN, Float.NaN);
        Gauges.tile(c, t, col(3, tw), ty, tw, th, Icons.INTAKE, "ТЕМП. ВПУСКА",
                d.intakeTemp, 0, "°C", 55f, 70f);
    }

    static float col(int i, float tw) {
        return 14f + i * (tw + 10f);
    }
}
