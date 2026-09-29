package com.q50gtr.plus.data;

/**
 * Every channel the dashboard can draw, in one place.
 *
 * The field list is deliberately shaped after what the car actually logs
 * through EcuTek on the VR30DDTT, so that swapping DemoDataProvider for a real
 * EcuTek source later is a data-layer change only — no gauge is rewritten.
 *
 * Turbo speed is absent on purpose: it is not logged on this vehicle.
 */
public final class VehicleData {

    /* --- engine ------------------------------------------------------- */
    public final Channel rpm = new Channel("rpm", "RPM", "");
    public final Channel boostActual = new Channel("boost_actual", "BOOST ACT", "bar");
    public final Channel boostTarget = new Channel("boost_target", "BOOST TGT", "bar");
    public final Channel coolantTemp = new Channel("coolant_temp", "ОХЛ. ЖИДКОСТЬ", "°C");
    public final Channel oilTemp = new Channel("oil_temp", "МАСЛО T", "°C");
    public final Channel oilPressure = new Channel("oil_pressure", "МАСЛО P", "bar");
    public final Channel intakeTemp = new Channel("intake_temp", "ВПУСК T", "°C");

    /* --- fuel --------------------------------------------------------- */
    public final Channel hpfpActual = new Channel("hpfp_actual", "HPFP ACT", "MPa");
    public final Channel hpfpTarget = new Channel("hpfp_target", "HPFP TGT", "MPa");
    public final Channel afrB1 = new Channel("afr_b1", "AFR B1", "");
    public final Channel afrB2 = new Channel("afr_b2", "AFR B2", "");
    public final Channel stftB1 = new Channel("stft_b1", "STFT B1", "%");
    public final Channel stftB2 = new Channel("stft_b2", "STFT B2", "%");
    public final Channel ltftB1 = new Channel("ltft_b1", "LTFT B1", "%");
    public final Channel ltftB2 = new Channel("ltft_b2", "LTFT B2", "%");

    /**
     * Low pressure fuel pump. NOT logged in the current EcuTek configuration,
     * so it is permanently UNAVAILABLE and the fuel page renders it as such.
     * It exists as a field only so a future configuration can fill it in.
     */
    public final Channel lpfp = new Channel("lpfp", "LPFP", "kPa");

    /* --- ignition / knock --------------------------------------------- */
    public final Channel ignitionTiming = new Channel("ign_timing", "IGN TIMING", "°");
    public final Channel knockRetard = new Channel("knock_retard", "KNOCK RETARD", "°");
    /** Knock index, cylinders 1..6 (index 0 == cylinder 1). */
    public final Channel[] knockIndex = new Channel[6];

    /* --- driver inputs / vehicle -------------------------------------- */
    public final Channel throttle = new Channel("throttle", "THROTTLE", "%");
    public final Channel pedal = new Channel("pedal", "PEDAL", "%");
    public final Channel speed = new Channel("speed", "SPEED", "km/h");

    /* --- cam / valve timing ------------------------------------------- */
    public final Channel ivtIntakeB1 = new Channel("ivt_in_b1", "IVT IN B1", "°");
    public final Channel ivtExhaustB1 = new Channel("ivt_ex_b1", "IVT EX B1", "°");
    public final Channel ivtIntakeB2 = new Channel("ivt_in_b2", "IVT IN B2", "°");
    public final Channel ivtExhaustB2 = new Channel("ivt_ex_b2", "IVT EX B2", "°");

    /* --- chassis ------------------------------------------------------ */
    public final Channel transmissionTemp = new Channel("trans_temp", "АКПП", "°C");
    public final Channel transferCaseTemp = new Channel("transfer_temp", "РАЗДАТКА", "°C");
    public final Channel batteryVoltage = new Channel("voltage", "БОРТ. СЕТЬ", "V");
    public final Channel ambientTemp = new Channel("ambient_temp", "СНАРУЖИ", "°C");

    /* --- HEV & Telemetry ---------------------------------------------- */
    public final Channel hybridBatterySoc = new Channel("hev_soc", "HEV SOC", "%");
    public final Channel evModeRatio = new Channel("ev_ratio", "EV MODE", "%");
    public final Channel fuelConsumed = new Channel("fuel_cons", "CONSUMO", "L");
    public final Channel fuelInstant = new Channel("fuel_inst", "INST", "L/100");
    public final Channel regenEnergy = new Channel("regen", "REGEN", "kWh");
    public final Channel combinedHp = new Channel("combined_hp", "POTENCIA", "HP");
    public final Channel combinedTorque = new Channel("combined_trq", "PAR MOTOR", "Nm");
    public final Channel latG = new Channel("lat_g", "LAT G", "G");
    public final Channel longG = new Channel("long_g", "LONG G", "G");
    public final Channel tripDist = new Channel("trip_dist", "TRAYECTO A", "KM");
    public final Channel hevBattTemp = new Channel("hev_temp", "HEV BATT", "°C");

    /* --- AirLift Performance 3H (suspension) --------------------------- */
    public final Channel airFrontLeft = new Channel("air_fl", "FL", "psi");
    public final Channel airFrontRight = new Channel("air_fr", "FR", "psi");
    public final Channel airRearLeft = new Channel("air_rl", "RL", "psi");
    public final Channel airRearRight = new Channel("air_rr", "RR", "psi");
    public final Channel airTank = new Channel("air_tank", "РЕСИВЕР", "psi");

    public VehicleData() {
        for (int i = 0; i < knockIndex.length; i++) {
            knockIndex[i] = new Channel("knock_idx_" + (i + 1), "CYL " + (i + 1), "");
        }
        // LPFP is not logged; say so once and never touch it again.
        lpfp.setUnavailable();
    }

    /** Highest knock index across cylinders 1..6, or null when none is known. */
    public Channel maxKnockIndexChannel() {
        Channel best = null;
        for (int i = 0; i < knockIndex.length; i++) {
            Channel c = knockIndex[i];
            if (!c.hasValue()) {
                continue;
            }
            if (best == null || c.getValue() > best.getValue()) {
                best = c;
            }
        }
        return best;
    }

    /** True while any drawn channel is still synthetic. */
    public boolean anyDemo() {
        return rpm.isDemo() || boostActual.isDemo() || airFrontLeft.isDemo();
    }
}
