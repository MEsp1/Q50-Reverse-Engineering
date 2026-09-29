package com.q50gtr.plus.data;

/**
 * Owns the vehicle data and decides which sources get to fill it.
 *
 * Today: the live sources report themselves disconnected, so the demo provider
 * fills in. The moment a live source connects, it takes over and the demo
 * provider is silenced — without the UI knowing anything happened.
 */
public final class DataHub {

    private final VehicleData data = new VehicleData();

    private final EcuTekSource ecuTek;
    private final AirLiftSource airLift;
    private final DemoDataProvider demo;

    private boolean demoAllowed = true;

    public DataHub() {
        this(new EcuTekLiveSource(), new AirLiftLiveSource(), new DemoDataProvider());
    }

    public DataHub(EcuTekSource ecuTek, AirLiftSource airLift, DemoDataProvider demo) {
        this.ecuTek = ecuTek;
        this.airLift = airLift;
        this.demo = demo;
    }

    public VehicleData getData() {
        return data;
    }

    public void setDemoAllowed(boolean allowed) {
        demoAllowed = allowed;
    }

    public void start() {
        ecuTek.start();
        airLift.start();
        demo.start();
    }

    public void stop() {
        ecuTek.stop();
        airLift.stop();
        demo.stop();
    }

    public void poll(long nowMs) {
        boolean live = false;
        if (ecuTek.isConnected()) {
            ecuTek.poll(data, nowMs);
            live = true;
        }
        if (airLift.isConnected()) {
            airLift.poll(data, nowMs);
            live = true;
        }
        if (!live && demoAllowed) {
            demo.poll(data, nowMs);
        }
    }

    /** What the status strip should call the current source. */
    public String getSourceLabel() {
        if (ecuTek.isConnected() && airLift.isConnected()) {
            return ecuTek.getName() + " + " + airLift.getName();
        }
        if (ecuTek.isConnected()) {
            return ecuTek.getName();
        }
        if (airLift.isConnected()) {
            return airLift.getName();
        }
        return demoAllowed ? demo.getName() : "NO LINK";
    }

    public boolean isDemoActive() {
        return !ecuTek.isConnected() && !airLift.isConnected() && demoAllowed;
    }
}
