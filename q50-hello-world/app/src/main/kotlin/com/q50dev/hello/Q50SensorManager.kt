package com.q50dev.hello

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * Manages registration and reading of YGOMI vehicle sensors on the Q50 IVI.
 *
 * The YGOMI/Connexis IVI layer translates raw CAN bus frames into standard
 * Android SensorManager events. This class wraps that access pattern,
 * following the approach observed in the working Red Sport reference app:
 *
 * 1. Get SensorManager system service
 * 2. Enumerate all sensors with getSensorList(TYPE_ALL)
 * 3. Filter to vehicle sensors using heuristic detection
 * 4. Register listeners with SENSOR_DELAY_GAME
 * 5. Read values[0] from each SensorEvent
 *
 * Heuristic for identifying vehicle sensors (from Red Sport app):
 * - sensor.getVendor() contains "YGOMI"
 * - sensor.getName() contains "VS_ID"
 * - sensor.getType() is in range 12..53
 *
 * @see <a href="docs/sensor-map.md">Vehicle sensor map</a>
 */
class Q50SensorManager(context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "Q50Sensor"
    }

    private val sm: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    /** Live vehicle data — updated on every sensor event. */
    val data = VehicleData()

    /** Callback invoked on the sensor thread when any vehicle value changes. */
    var onDataChanged: ((VehicleData) -> Unit)? = null

    /** Number of vehicle sensors successfully registered. */
    var registeredCount: Int = 0
        private set

    /** List of all detected vehicle sensors (for diagnostics). */
    val detectedSensors: MutableList<String> = mutableListOf()

    /**
     * Register listeners for all detected vehicle sensors.
     * Call from Activity.onResume().
     */
    fun start() {
        val sensorManager = sm ?: run {
            Log.w(TAG, "SensorManager not available")
            return
        }

        registeredCount = 0
        detectedSensors.clear()

        for (sensor in sensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (isVehicleSensor(sensor)) {
                val success = sensorManager.registerListener(
                    this, sensor, SensorManager.SENSOR_DELAY_GAME
                )
                if (success) {
                    registeredCount++
                    val info = "type=${sensor.type} name=${sensor.name} vendor=${sensor.vendor}"
                    detectedSensors.add(info)
                    Log.d(TAG, "Registered: $info")
                }
            }
        }

        Log.i(TAG, "Registered $registeredCount vehicle sensors")
    }

    /**
     * Unregister all sensor listeners.
     * Call from Activity.onPause().
     */
    fun stop() {
        sm?.unregisterListener(this)
        Log.i(TAG, "Unregistered all sensors")
    }

    /**
     * Heuristic from the Red Sport reference app to identify YGOMI vehicle sensors.
     *
     * Returns true if any of these conditions match:
     * - sensor type is in the 12..53 range
     * - vendor string contains "YGOMI"
     * - sensor name contains "VS_ID"
     */
    private fun isVehicleSensor(sensor: Sensor): Boolean {
        if (sensor.type in Q50Sensors.VEHICLE_RANGE) return true
        if (sensor.vendor?.contains("YGOMI", ignoreCase = true) == true) return true
        if (sensor.name?.contains("VS_ID", ignoreCase = true) == true) return true
        return false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val value = event.values?.getOrNull(0) ?: return

        when (event.sensor.type) {
            Q50Sensors.RPM          -> data.rpm = value
            Q50Sensors.SPEED        -> data.speedKmh = value
            Q50Sensors.TORQUE       -> data.torqueNm = value
            Q50Sensors.COOLANT_TEMP -> data.coolantC = value
            Q50Sensors.OIL_TEMP     -> data.oilTempC = value
            Q50Sensors.OIL_PRESSURE -> data.oilPressureRaw = value
            Q50Sensors.THROTTLE     -> data.throttle = value
            Q50Sensors.LATERAL_G    -> data.lateralG = value
            Q50Sensors.LONG_G       -> data.longitudinalG = value
            Q50Sensors.GEAR         -> data.gear = value.toInt()
            Q50Sensors.POWER        -> data.power = value
            Q50Sensors.TPMS_FR      -> data.tpmsFR = value
            Q50Sensors.TPMS_FL      -> data.tpmsFL = value
            Q50Sensors.TPMS_RR      -> data.tpmsRR = value
            Q50Sensors.TPMS_RL      -> data.tpmsRL = value
            else -> return  // Unknown vehicle sensor, ignore
        }

        onDataChanged?.invoke(data)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not required for vehicle sensor readings.
    }
}
