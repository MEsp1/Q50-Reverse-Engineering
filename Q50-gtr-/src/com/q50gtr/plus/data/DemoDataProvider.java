package com.q50gtr.plus.data;

/**
 * Synthetic data for bringing the UI up on a bench.
 *
 * Every value it writes is marked {@link Channel#DEMO}, so the dashboard badges
 * it and nobody can mistake it for telemetry. It fills both the EcuTek-shaped
 * and the AirLift-shaped channels so all three pages can be evaluated, and it
 * deliberately leaves LPFP untouched (not logged on this car).
 */
public final class DemoDataProvider implements EcuTekSource, AirLiftSource {

    private long startedAtMs;
    private boolean running;

    public String getName() {
        return "DEMO";
    }

    public void start() {
        startedAtMs = System.currentTimeMillis();
        running = true;
    }

    public void stop() {
        running = false;
    }

    public boolean isConnected() {
        // A demo generator is never a link to the car.
        return false;
    }

    public void poll(VehicleData d, long nowMs) {
        if (!running) {
            return;
        }
        float t = (nowMs - startedAtMs) / 1000f;

        // A lazy synthetic pull in third gear: rev, shift, settle, repeat.
        float cycle = t % 14f;
        float load = cycle < 8f ? (cycle / 8f) : Math.max(0f, 1f - (cycle - 8f) / 6f);
        load = smooth(load);

        float rpm = 800f + load * 5800f + wobble(t, 7.3f) * 60f;
        float boost = -0.45f + load * 1.55f + wobble(t, 3.1f) * 0.04f;
        float throttle = load * 100f;

        d.rpm.setDemo(clamp(rpm, 650f, 6800f), nowMs);
        d.boostActual.setDemo(boost, nowMs);
        d.boostTarget.setDemo(boost + 0.06f + wobble(t, 1.7f) * 0.03f, nowMs);
        d.coolantTemp.setDemo(88f + load * 9f + wobble(t, 0.4f) * 1.5f, nowMs);
        d.oilTemp.setDemo(96f + load * 22f + wobble(t, 0.3f) * 2f, nowMs);
        d.oilPressure.setDemo(1.6f + load * 3.4f + wobble(t, 2.9f) * 0.1f, nowMs);
        d.intakeTemp.setDemo(32f + load * 18f + wobble(t, 0.9f) * 2f, nowMs);

        d.hpfpActual.setDemo(4.2f + load * 15.5f + wobble(t, 5.1f) * 0.4f, nowMs);
        d.hpfpTarget.setDemo(4.5f + load * 15.6f, nowMs);
        d.afrB1.setDemo(14.7f - load * 3.3f + wobble(t, 4.3f) * 0.12f, nowMs);
        d.afrB2.setDemo(14.7f - load * 3.25f + wobble(t, 4.7f) * 0.12f, nowMs);
        d.stftB1.setDemo(wobble(t, 2.3f) * 4.5f, nowMs);
        d.stftB2.setDemo(wobble(t, 2.6f) * 4.5f, nowMs);
        d.ltftB1.setDemo(-1.6f + wobble(t, 0.2f) * 1.2f, nowMs);
        d.ltftB2.setDemo(-2.1f + wobble(t, 0.17f) * 1.2f, nowMs);

        d.ignitionTiming.setDemo(22f - load * 16f + wobble(t, 6.1f) * 1.4f, nowMs);
        d.knockRetard.setDemo(Math.max(0f, load * 2.4f - 1.4f + wobble(t, 8.3f) * 0.6f), nowMs);
        for (int i = 0; i < d.knockIndex.length; i++) {
            float k = load * 28f + wobble(t, 3.7f + i * 1.13f) * 14f;
            d.knockIndex[i].setDemo(Math.max(0f, k), nowMs);
        }

        d.throttle.setDemo(clamp(throttle, 0f, 100f), nowMs);
        d.pedal.setDemo(clamp(throttle * 1.05f, 0f, 100f), nowMs);
        d.speed.setDemo(clamp(18f + load * 145f, 0f, 260f), nowMs);

        d.ivtIntakeB1.setDemo(load * 38f, nowMs);
        d.ivtIntakeB2.setDemo(load * 37f, nowMs);
        d.ivtExhaustB1.setDemo(-load * 24f, nowMs);
        d.ivtExhaustB2.setDemo(-load * 23f, nowMs);

        d.transmissionTemp.setDemo(78f + load * 16f + wobble(t, 0.25f) * 1.5f, nowMs);
        d.transferCaseTemp.setDemo(71f + load * 12f + wobble(t, 0.21f) * 1.5f, nowMs);
        d.batteryVoltage.setDemo(14.2f - load * 0.45f + wobble(t, 1.3f) * 0.06f, nowMs);
        d.ambientTemp.setDemo(17f + wobble(t, 0.05f) * 0.6f, nowMs);

        d.airFrontLeft.setDemo(62f + wobble(t, 1.1f) * 2.5f, nowMs);
        d.airFrontRight.setDemo(61f + wobble(t, 1.4f) * 2.5f, nowMs);
        d.airRearLeft.setDemo(74f + wobble(t, 0.9f) * 2.5f, nowMs);
        d.airRearRight.setDemo(73f + wobble(t, 1.2f) * 2.5f, nowMs);
        d.airTank.setDemo(142f - wobble(t, 0.12f) * 8f, nowMs);

        // LPFP intentionally left UNAVAILABLE: it is not logged on this car and
        // a demo number here would be a lie the driver could act on.
    }

    private static float wobble(float t, float hz) {
        return (float) Math.sin(t * hz);
    }

    private static float smooth(float x) {
        return x * x * (3f - 2f * x);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
