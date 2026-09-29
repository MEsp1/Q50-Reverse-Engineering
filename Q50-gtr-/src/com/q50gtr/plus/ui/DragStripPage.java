package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.VehicleData;

/**
 * Drag Strip (Acceleration).
 */
public final class DragStripPage implements Page {

    public String title() {
        return "DRAG STRIP";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float r = 128f;
        float cy = 150f;
        
        // Speed and RPM
        Gauges.dial(c, t, w / 2f - 168f, cy, r, d.speed,
                0f, 240f, 0, 12, Float.NaN, "SPEED", "KM/H", 1f, 0);
        Gauges.dial(c, t, w / 2f + 168f, cy, r, d.rpm,
                0f, 8000f, 0, 8, 6800f, "RPM", "x1000", 1000f, 0);

        float tw = (w - 28f - 30f) / 4f;
        float ty = h - 92f;
        float th = 78f;
        
        Gauges.tile(c, t, EnginePage.col(0, tw), ty, tw, th, Icons.BATTERY, "HEV SOC",
                d.hybridBatterySoc, 0, "%", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(1, tw), ty, tw, th, Icons.THROTTLE, "0-100 KM/H",
                d.latG, 2, "SEC", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(2, tw), ty, tw, th, Icons.ROUTE, "1/4 MILLA",
                d.latG, 2, "SEC", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(3, tw), ty, tw, th, Icons.GEAR, "PEAK LONG G",
                d.longG, 2, "G", Float.NaN, Float.NaN);
    }
}
