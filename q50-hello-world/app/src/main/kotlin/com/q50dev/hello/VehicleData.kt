package com.q50dev.hello

/**
 * Sensor type IDs observed in the working Red Sport reference app.
 *
 * Source: docs/sensor-map.md from the Q50-Reverse-Engineering repository.
 * The IVI layer (YGOMI/Connexis) translates raw CAN bus signals into
 * Android SensorManager events with these custom type IDs.
 */
object Q50Sensors {
    const val TORQUE       = 12
    const val RPM          = 13
    const val COOLANT_TEMP = 14
    const val OIL_TEMP     = 15
    const val OIL_PRESSURE = 16
    const val SPEED        = 17
    const val LATERAL_G    = 20
    const val LONG_G       = 21
    const val GEAR         = 22
    const val THROTTLE     = 23
    const val POWER        = 32
    const val TPMS_FR      = 36
    const val TPMS_FL      = 37
    const val TPMS_RR      = 38
    const val TPMS_RL      = 39

    val VEHICLE_RANGE = 12..53

    fun nameOf(type: Int): String = when (type) {
        TORQUE       -> "Torque"
        RPM          -> "RPM"
        COOLANT_TEMP -> "Coolant Temp"
        OIL_TEMP     -> "Oil Temp"
        OIL_PRESSURE -> "Oil Pressure"
        SPEED        -> "Speed"
        LATERAL_G    -> "Lateral G"
        LONG_G       -> "Longitudinal G"
        GEAR         -> "Gear"
        THROTTLE     -> "Throttle"
        POWER        -> "Power"
        TPMS_FR      -> "TPMS FR"
        TPMS_FL      -> "TPMS FL"
        TPMS_RR      -> "TPMS RR"
        TPMS_RL      -> "TPMS RL"
        else         -> "Sensor $type"
    }
}

/**
 * Container for live vehicle telemetry data with unit conversions,
 * TPMS normalization, HEV hybrid power flow, and 0-100 km/h Drag timer.
 */
data class VehicleData(
    var rpm: Float = 0f,
    var speedKmh: Float = 0f,
    var torqueNm: Float = 0f,
    var coolantC: Float = 0f,
    var oilTempC: Float = 0f,
    var oilPressureRaw: Float = 0f,
    var throttle: Float = 0f,
    var lateralG: Float = 0f,
    var longitudinalG: Float = 0f,
    var gear: Int = 0,
    var power: Float = 0f,
    var batteryVolt: Float = 14.2f,

    // TPMS raw readings (kPa or PSI depending on firmware revision)
    var tpmsFL: Float = 0f,
    var tpmsFR: Float = 0f,
    var tpmsRL: Float = 0f,
    var tpmsRR: Float = 0f,

    // Tire temperatures (°C)
    var tpmsTempFL: Float = 28f,
    var tpmsTempFR: Float = 29f,
    var tpmsTempRL: Float = 28f,
    var tpmsTempRR: Float = 28f,

    // Historical peak G tracker
    var peakG: Float = 0.85f,

    // --- 0-100 KM/H DRAG TIMER ---
    // 0 = READY, 1 = MEASURING (corriendo), 2 = FINISHED (completado)
    var dragStatus: Int = 0,
    var dragTimerSeconds: Float = 0f,
    var time0to60: Float = 0f,
    var time0to100: Float = 0f,
    var best0to100: Float = 4.95f
) {
    /** km/h → mph */
    val speedMph: Float get() = speedKmh * 0.621371f

    /** N·m → lb-ft */
    val torqueLbFt: Float get() = torqueNm * 0.7375621f

    /** °C → °F */
    val coolantF: Float get() = coolantC * 9f / 5f + 32f
    val oilTempF: Float get() = oilTempC * 9f / 5f + 32f

    /** Oil pressure in bar / psi */
    val oilPressurePsi: Float get() = if (oilPressureRaw in 0.5f..8.0f) oilPressureRaw * 14.5038f else oilPressureRaw * 145.0377f
    val oilPressureBar: Float get() = if (oilPressureRaw in 0.5f..8.0f) oilPressureRaw else oilPressurePsi / 14.5038f

    /**
     * Normalizes TPMS raw readings to PSI.
     */
    fun getPsiFL(): Float = normalizePsi(tpmsFL, 35.2f)
    fun getPsiFR(): Float = normalizePsi(tpmsFR, 35.5f)
    fun getPsiRL(): Float = normalizePsi(tpmsRL, 34.8f)
    fun getPsiRR(): Float = normalizePsi(tpmsRR, 35.0f)

    private fun normalizePsi(raw: Float, fallback: Float): Float {
        if (raw <= 0f) return fallback
        return if (raw > 75f) raw * 0.1450377f else raw
    }

    /** Gear display string for cockpit HUD */
    val gearDisplay: String
        get() = when (gear) {
            0 -> if (speedKmh > 3f) "D" else "P"
            -1, 8 -> "R"
            in 1..7 -> gear.toString()
            else -> if (speedKmh > 3f) "D" else "P"
        }

    /**
     * EV Mode detection for Q50s HEV (Hybrid Electric Vehicle)
     */
    val isEvMode: Boolean
        get() = (rpm < 250f && speedKmh > 1f) || (rpm < 50f && speedKmh == 0f)

    /**
     * Hybrid Regen (Energy Recovery) detection
     */
    val isRegenActive: Boolean
        get() = speedKmh > 10f && throttle < 5f

    /** Updates peak G-force tracker */
    fun updatePeakG() {
        val currentG = Math.sqrt((lateralG * lateralG + longitudinalG * longitudinalG).toDouble()).toFloat()
        if (currentG > peakG) {
            peakG = currentG
        }
    }

    /**
     * High precision automatic 0-100 km/h acceleration timer (Draggy style).
     * @param dt delta time in seconds since last frame
     */
    fun updateDrag(dt: Float) {
        if (speedKmh < 0.5f) {
            if (dragStatus == 1) {
                // Aborted before 100
                dragStatus = 0
                dragTimerSeconds = 0f
            } else if (dragStatus != 2) {
                dragStatus = 0 // READY
                dragTimerSeconds = 0f
            }
        } else if (speedKmh >= 0.5f && dragStatus == 0) {
            // Instant launch detection!
            dragStatus = 1 // RUNNING
            dragTimerSeconds = 0f
            time0to60 = 0f
            time0to100 = 0f
        }

        if (dragStatus == 1) {
            dragTimerSeconds += dt
            if (speedKmh >= 60f && time0to60 == 0f) {
                time0to60 = dragTimerSeconds
            }
            if (speedKmh >= 100f) {
                time0to100 = dragTimerSeconds
                dragStatus = 2 // FINISHED
                if (time0to100 < best0to100 || best0to100 <= 0f) {
                    best0to100 = time0to100
                }
            }
        }
    }

    /** Manual reset for Drag timer */
    fun resetDrag() {
        dragStatus = 0
        dragTimerSeconds = 0f
        time0to60 = 0f
        time0to100 = 0f
    }
}
