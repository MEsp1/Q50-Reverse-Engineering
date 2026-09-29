package com.q50gtr.plus.data;

/**
 * Placeholder for the real AirLift Performance 3H link.
 *
 * The 3H protocol is not documented here and is not guessed at. The class
 * exists so the chassis page already reads suspension data through the same
 * seam it will use once a verified transport lands.
 */
public final class AirLiftLiveSource implements AirLiftSource {

    private static final long STALE_AFTER_MS = 3000L;

    public String getName() {
        return "AIRLIFT";
    }

    public void start() {
        // TODO: open the verified AirLift 3H transport here.
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
        // TODO: decode a frame and call setLive(...) per corner.
        d.airFrontLeft.ageOut(nowMs, STALE_AFTER_MS);
        d.airFrontRight.ageOut(nowMs, STALE_AFTER_MS);
        d.airRearLeft.ageOut(nowMs, STALE_AFTER_MS);
        d.airRearRight.ageOut(nowMs, STALE_AFTER_MS);
        d.airTank.ageOut(nowMs, STALE_AFTER_MS);
    }
}
