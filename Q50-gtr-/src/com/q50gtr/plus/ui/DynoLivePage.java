package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.VehicleData;

/**
 * Dyno Live (HP and Torque combined).
 */
public final class DynoLivePage implements Page {

    public String title() {
        return "DYNO LIVE";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float r = 128f;
        float cy = 150f;
        
        // Medidor dual
        Gauges.dial(c, t, w / 2f - 168f, cy, r, d.combinedHp,
                0f, 400f, 0, 8, 364f, "POTENCIA COMBI", "HP", 1f, 0);
        Gauges.dial(c, t, w / 2f + 168f, cy, r, d.combinedTorque,
                0f, 600f, 0, 6, 500f, "PAR MOTOR TOTAL", "Nm", 1f, 0);

        float tw = (w - 28f - 30f) / 4f;
        float ty = h - 92f;
        float th = 78f;
        
        Gauges.tile(c, t, EnginePage.col(0, tw), ty, tw, th, Icons.KNOCK, "RPM ENGINE",
                d.rpm, 0, "RPM", 6800f, 7200f);
        Gauges.tile(c, t, EnginePage.col(1, tw), ty, tw, th, Icons.BATTERY, "HEV SOC",
                d.hybridBatterySoc, 0, "%", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(2, tw), ty, tw, th, Icons.GEARBOX, "GEAR",
                d.speed, 0, "KM/H", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(3, tw), ty, tw, th, Icons.SPARK, "REGEN",
                d.regenEnergy, 1, "kWh", Float.NaN, Float.NaN);
    }
}
