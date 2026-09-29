package com.q50gtr.plus;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.ui.DashboardView;

/**
 * Q50 GTR+ — a single full-screen instrument panel.
 *
 * No fragments, no support library, no layout XML: the activity owns a data hub
 * and one custom View, which is all a factory head unit should ever be asked
 * to run.
 */
public final class MainActivity extends Activity {

    private static final String STATE_PAGE = "page";

    private DataHub hub;
    private DashboardView dashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // The theme is already fullscreen; all the window needs from us is to
        // stay awake while the car is running.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        hub = new DataHub();
        dashboard = new DashboardView(this, hub);
        setContentView(dashboard);

        if (savedInstanceState != null) {
            dashboard.setPage(savedInstanceState.getInt(STATE_PAGE, 0));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hub.start();
        dashboard.start();
    }

    @Override
    protected void onPause() {
        dashboard.stop();
        hub.stop();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE, dashboard.getPage());
    }
}
