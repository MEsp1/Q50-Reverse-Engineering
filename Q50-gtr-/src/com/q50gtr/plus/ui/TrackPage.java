package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.VehicleData;

/**
 * Track / G-Force.
 */
public final class TrackPage implements Page {

    public String title() {
        return "TRACK / G-FORCE";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float r = 128f;
        float cy = 150f;
        
        // G-Force and RPM/Throttle dials
        Gauges.dial(c, t, w / 2f - 168f, cy, r, d.latG,
                -1.5f, 1.5f, 2, 6, Float.NaN, "LATERAL G", "G", 1f, 2);
        Gauges.dial(c, t, w / 2f + 168f, cy, r, d.longG,
                -1.5f, 1.5f, 2, 6, Float.NaN, "LONG G", "G", 1f, 2);

        float tw = (w - 28f - 30f) / 4f;
        float ty = h - 92f;
        float th = 78f;
        
        Gauges.tile(c, t, EnginePage.col(0, tw), ty, tw, th, Icons.THROTTLE, "THROTTLE",
                d.throttle, 0, "%", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(1, tw), ty, tw, th, Icons.THROTTLE, "BRAKE REGEN",
                d.pedal, 0, "%", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(2, tw), ty, tw, th, Icons.OIL_TEMP, "OIL TEMP",
                d.oilTemp, 0, "°C", 125f, 138f);
        Gauges.tile(c, t, EnginePage.col(3, tw), ty, tw, th, Icons.COOLANT, "COOLANT",
                d.coolantTemp, 0, "°C", 105f, 112f);
    }
}
