package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.VehicleData;

/**
 * Viaje / Eco (Trip & HEV Efficiency).
 */
public final class TripEcoPage implements Page {

    public String title() {
        return "VIAJE / ECO";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float r = 120f;
        float cy = 150f;
        
        // Medidor central: Consumo Total
        Gauges.dial(c, t, w / 2f, cy, r, d.fuelConsumed,
                0f, 20f, 1, 4, Float.NaN, "CONSUMO VIAJE", "LITROS", 1f, 1);

        float tw = (w - 28f - 30f) / 4f;
        float ty = h - 92f;
        float th = 78f;
        
        // Tiles inferiores
        Gauges.tile(c, t, EnginePage.col(0, tw), ty, tw, th, Icons.BATTERY, "BATERÍA HV",
                d.hybridBatterySoc, 0, "%", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(1, tw), ty, tw, th, Icons.ROUTE, "TRAYECTO A",
                d.tripDist, 1, "KM", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(2, tw), ty, tw, th, Icons.EV, "EV MODE",
                d.evModeRatio, 0, "%", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(3, tw), ty, tw, th, Icons.COOLANT, "HEV TEMP",
                d.hevBattTemp, 0, "°C", 40f, 50f);
    }
}
