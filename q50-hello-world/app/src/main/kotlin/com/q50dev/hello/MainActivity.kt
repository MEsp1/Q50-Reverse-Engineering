package com.q50dev.hello

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager

/**
 * Main Activity for Q50s HEV Telemetry Dashboard.
 *
 * Runs fullscreen landscape on the lower 7-inch InTouch display (800×480 WVGA).
 * Extends pure [Activity] for 100% Dalvik 2.3 (API 10) compatibility.
 */
class MainActivity : Activity() {

    companion object {
        private const val TAG = "Q50Main"
    }

    private lateinit var sensorMgr: Q50SensorManager
    private lateinit var dashboardView: DashboardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: Initializing Q50 GT-R Telemetry Dashboard")

        // Fullscreen without title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        // Keep screen on while driving
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize custom high-performance dashboard view
        dashboardView = DashboardView(this)
        setContentView(dashboardView)

        // Initialize YGOMI CAN sensor manager
        sensorMgr = Q50SensorManager(this)
        sensorMgr.onDataChanged = { liveData ->
            // Pass live sensor data and flag live CAN mode
            dashboardView.data = liveData
            dashboardView.isLiveMode = true
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: Starting sensors and display refresh")
        dashboardView.start()
        sensorMgr.start()
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause: Stopping sensors and pause refresh")
        sensorMgr.stop()
        dashboardView.stop()
    }
}
