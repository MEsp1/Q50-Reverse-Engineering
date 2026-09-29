package com.q50gtr.plus.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.q50gtr.plus.data.Channel;
import com.q50gtr.plus.data.VehicleData;

/**
 * Вкладка 3: пневмоподвеска вокруг машины, температуры трансмиссии и бортовая
 * сеть — колонкой справа, контроль зажигания и движения — строкой снизу.
 */
public final class ChassisPage implements Page {

    public String title() {
        return "ШАССИ";
    }

    public void draw(Canvas c, Theme t, VehicleData d, float w, float h) {
        float colW = 238f;
        float colX = w - 14f - colW;

        c.drawText("ДАВЛЕНИЕ ПНЕВМОПОДВЕСКИ (bar)", (colX - 14f) / 2f + 14f, 26f,
                t.text(Theme.LABEL, 12.5f, Paint.Align.CENTER, false));

        float carW = 168f;
        float carH = 132f;
        float carX = (colX - 14f) / 2f + 14f - carW / 2f;
        float carY = 48f;
        Q50Rear.car(c, t, carX, carY, carW, carH);

        // Стойки по углам машины: передние выше, задние ниже.
        float strutH = 62f;
        float leftX = carX - 52f;
        float rightX = carX + carW + 52f;
        float topY = carY + 20f;
        float bottomY = carY + carH - 4f;
        Q50Rear.strut(c, t, leftX, topY, strutH, d.airFrontLeft, true);
        Q50Rear.strut(c, t, rightX, topY, strutH, d.airFrontRight, false);
        Q50Rear.strut(c, t, leftX, bottomY, strutH, d.airRearLeft, true);
        Q50Rear.strut(c, t, rightX, bottomY, strutH, d.airRearRight, false);

        // Колонка справа.
        float rh = 58f;
        float gap = 8f;
        float ry = 22f;
        Gauges.tileRow(c, t, colX, ry, colW, rh, Icons.GEARBOX, "ТЕМП. АКПП",
                d.transmissionTemp, 0, "°C", 110f, 125f);
        Gauges.tileRow(c, t, colX, ry + (rh + gap), colW, rh, Icons.GEARBOX, "ТЕМП. РАЗДАТКИ",
                d.transferCaseTemp, 0, "°C", 110f, 125f);
        Gauges.tileRow(c, t, colX, ry + 2 * (rh + gap), colW, rh, Icons.BATTERY, "НАПРЯЖЕНИЕ",
                d.batteryVoltage, 1, "V", Float.NaN, Float.NaN);
        Gauges.tileRow(c, t, colX, ry + 3 * (rh + gap), colW, rh, Icons.AMBIENT, "ТЕМП. ОКР. ВОЗДУХА",
                d.ambientTemp, 0, "°C", Float.NaN, Float.NaN);

        // Нижняя строка на всю ширину.
        float tw = (w - 28f - 30f) / 4f;
        float ty = h - 92f;
        float th = 78f;
        Channel knock = d.maxKnockIndexChannel();
        Gauges.tile(c, t, EnginePage.col(0, tw), ty, tw, th, Icons.SPARK, "УГОЛ ЗАЖИГАНИЯ",
                d.ignitionTiming, 0, "°", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(1, tw), ty, tw, th, Icons.KNOCK, "KNOCK INDEX (MAX)",
                knock != null ? knock : d.knockRetard, 1, "", 25f, 40f);
        Gauges.tile(c, t, EnginePage.col(2, tw), ty, tw, th, Icons.THROTTLE, "ДРОССЕЛЬ",
                d.throttle, 0, "%", Float.NaN, Float.NaN);
        Gauges.tile(c, t, EnginePage.col(3, tw), ty, tw, th, Icons.SPEED, "СКОРОСТЬ",
                d.speed, 0, "км/ч", Float.NaN, Float.NaN);
    }
}
