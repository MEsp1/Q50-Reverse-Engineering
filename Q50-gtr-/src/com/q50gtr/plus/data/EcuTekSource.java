package com.q50gtr.plus.data;

/**
 * Marker for a source of EcuTek / ECU-side channels: RPM, boost actual and
 * target, ignition timing, knock index per cylinder, knock retard, AFR B1/B2,
 * STFT/LTFT B1/B2, HPFP actual and target, throttle and pedal, speed,
 * coolant / oil / intake / transmission temperature, and IVT cam angles.
 *
 * Turbo speed is not part of this contract: the vehicle does not log it.
 */
public interface EcuTekSource extends DataSource {
}
