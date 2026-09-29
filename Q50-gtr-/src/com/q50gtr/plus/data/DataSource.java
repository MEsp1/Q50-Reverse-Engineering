package com.q50gtr.plus.data;

/**
 * Anything that can fill in parts of {@link VehicleData}.
 *
 * The dashboard never calls a source directly — it only reads VehicleData.
 * That is the seam that lets DemoDataProvider be replaced by a real EcuTek or
 * AirLift transport without touching a single gauge.
 */
public interface DataSource {

    /** Short name for the status strip, e.g. "DEMO", "ECUTEK", "AIRLIFT". */
    String getName();

    /** Called once when the dashboard becomes visible. */
    void start();

    /** Called when the dashboard is no longer visible. */
    void stop();

    /**
     * Pump whatever the source currently knows into {@code data}.
     * Called from the UI thread on the refresh tick; must not block.
     */
    void poll(VehicleData data, long nowMs);

    /** True once a real link to the vehicle / controller is established. */
    boolean isConnected();
}
