package com.q50gtr.plus.ui;

import android.graphics.Canvas;

import com.q50gtr.plus.data.VehicleData;

/**
 * One tab's worth of instruments. A page only reads VehicleData and draws —
 * it knows nothing about where the numbers came from.
 */
public interface Page {

    /** Tab caption. */
    String title();

    /** Draw into a {@code w} x {@code h} box whose origin is already at 0,0. */
    void draw(Canvas c, Theme t, VehicleData d, float w, float h);
}
