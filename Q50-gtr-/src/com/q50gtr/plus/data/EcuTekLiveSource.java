package com.q50gtr.plus.data;

/**
 * Placeholder for the real EcuTek link.
 *
 * The transport (which adapter, which framing, which PID map) is not verified
 * yet, so nothing is invented here: the source simply reports itself as
 * disconnected and writes no values. Wiring it up later means implementing
 * {@link #start()} / {@link #poll} and nothing else in the app changes.
 */
public final class EcuTekLiveSource implements EcuTekSource {

    /** Channels stop being trusted this long after their last real frame. */
    private static final long STALE_AFTER_MS = 1500L;

    public String getName() {
        return "ECUTEK";
    }

    public void start() {
        // TODO: open the verified EcuTek transport here.
    }

    public void stop() {
    }

    public boolean isConnected() {
        return false;
    }

    public void poll(VehicleData d, long nowMs) {
        if (!isConnected()) {
            return;
        }
        // TODO: decode a frame and call setLive(...) per channel.
        ageOut(d, nowMs);
    }

    private static void ageOut(VehicleData d, long nowMs) {
        d.rpm.ageOut(nowMs, STALE_AFTER_MS);
        d.boostActual.ageOut(nowMs, STALE_AFTER_MS);
        d.boostTarget.ageOut(nowMs, STALE_AFTER_MS);
        d.coolantTemp.ageOut(nowMs, STALE_AFTER_MS);
        d.oilTemp.ageOut(nowMs, STALE_AFTER_MS);
        d.oilPressure.ageOut(nowMs, STALE_AFTER_MS);
        d.intakeTemp.ageOut(nowMs, STALE_AFTER_MS);
        d.hpfpActual.ageOut(nowMs, STALE_AFTER_MS);
        d.hpfpTarget.ageOut(nowMs, STALE_AFTER_MS);
        d.afrB1.ageOut(nowMs, STALE_AFTER_MS);
        d.afrB2.ageOut(nowMs, STALE_AFTER_MS);
        d.stftB1.ageOut(nowMs, STALE_AFTER_MS);
        d.stftB2.ageOut(nowMs, STALE_AFTER_MS);
        d.ltftB1.ageOut(nowMs, STALE_AFTER_MS);
        d.ltftB2.ageOut(nowMs, STALE_AFTER_MS);
        d.ignitionTiming.ageOut(nowMs, STALE_AFTER_MS);
        d.knockRetard.ageOut(nowMs, STALE_AFTER_MS);
        for (int i = 0; i < d.knockIndex.length; i++) {
            d.knockIndex[i].ageOut(nowMs, STALE_AFTER_MS);
        }
        d.throttle.ageOut(nowMs, STALE_AFTER_MS);
        d.pedal.ageOut(nowMs, STALE_AFTER_MS);
        d.speed.ageOut(nowMs, STALE_AFTER_MS);
        d.ivtIntakeB1.ageOut(nowMs, STALE_AFTER_MS);
        d.ivtIntakeB2.ageOut(nowMs, STALE_AFTER_MS);
        d.ivtExhaustB1.ageOut(nowMs, STALE_AFTER_MS);
        d.ivtExhaustB2.ageOut(nowMs, STALE_AFTER_MS);
        d.transmissionTemp.ageOut(nowMs, STALE_AFTER_MS);
        d.transferCaseTemp.ageOut(nowMs, STALE_AFTER_MS);
        d.batteryVoltage.ageOut(nowMs, STALE_AFTER_MS);
        d.ambientTemp.ageOut(nowMs, STALE_AFTER_MS);
    }
}
