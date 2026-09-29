package com.q50gtr.plus.data;

/**
 * A single measured value plus the honest story of where it came from.
 *
 * The dashboard never renders a bare float: it renders a Channel, so a demo
 * value can never be mistaken for a real one, and a value the car simply does
 * not log (LPFP in the current EcuTek configuration) is drawn as "N/A" rather
 * than as a plausible-looking number.
 */
public final class Channel {

    /** Not logged by the vehicle / not wired up yet. Never draw a number. */
    public static final int UNAVAILABLE = 0;
    /** Synthetic value produced for UI bring-up. Must be badged DEMO. */
    public static final int DEMO = 1;
    /** Real value from the vehicle. */
    public static final int LIVE = 2;
    /** Was live, but no fresh frame arrived recently. */
    public static final int STALE = 3;

    public final String id;
    public final String label;
    public final String unit;

    private float value;
    private int state = UNAVAILABLE;
    private long updatedAtMs;

    public Channel(String id, String label, String unit) {
        this.id = id;
        this.label = label;
        this.unit = unit;
    }

    public void set(float value, int state, long nowMs) {
        this.value = value;
        this.state = state;
        this.updatedAtMs = nowMs;
    }

    public void setLive(float value, long nowMs) {
        set(value, LIVE, nowMs);
    }

    public void setDemo(float value, long nowMs) {
        set(value, DEMO, nowMs);
    }

    public void setUnavailable() {
        this.state = UNAVAILABLE;
    }

    /** Drops LIVE to STALE once no frame has arrived for timeoutMs. */
    public void ageOut(long nowMs, long timeoutMs) {
        if (state == LIVE && nowMs - updatedAtMs > timeoutMs) {
            state = STALE;
        }
    }

    public float getValue() {
        return value;
    }

    public int getState() {
        return state;
    }

    /** True when there is a number worth drawing. */
    public boolean hasValue() {
        return state != UNAVAILABLE;
    }

    public boolean isDemo() {
        return state == DEMO;
    }

    public long getUpdatedAtMs() {
        return updatedAtMs;
    }

    /** Formatted value, or an em dash when the channel carries nothing. */
    public String text(int decimals) {
        if (!hasValue()) {
            return "—";
        }
        return format(value, decimals);
    }

    public static String format(float v, int decimals) {
        if (decimals <= 0) {
            return Integer.toString(Math.round(v));
        }
        int scale = 1;
        for (int i = 0; i < decimals; i++) {
            scale *= 10;
        }
        int scaled = Math.round(v * scale);
        boolean negative = scaled < 0;
        if (negative) {
            scaled = -scaled;
        }
        int whole = scaled / scale;
        int frac = scaled % scale;
        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append('-');
        }
        sb.append(whole).append('.');
        String fracText = Integer.toString(frac);
        for (int i = fracText.length(); i < decimals; i++) {
            sb.append('0');
        }
        sb.append(fracText);
        return sb.toString();
    }
}
