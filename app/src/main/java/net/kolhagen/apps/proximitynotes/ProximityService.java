package net.kolhagen.apps.proximitynotes;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class ProximityService extends IntentService implements SensorEventListener {
    private static final String TAG = ProximityService.class.getSimpleName();
    private static final String ACTION_START = "net.kolhagen.apps.proximitynotes.action.START";

    private SensorManager mSensorManager;
    private Sensor mProximity;

    public ProximityService() {
        super("ProximityService");
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, ProximityService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        final String action = intent.getAction();
        if (!ACTION_START.equals(action))
            return;

        Log.v(TAG, "onHandleIntent");

        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        this.mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        this.mProximity = this.mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        this.mSensorManager.registerListener(this, this.mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");

        this.mSensorManager.unregisterListener(this);

        super.onDestroy();
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.

        Log.v(TAG, "onAccuracyChanged: Accuracy = " + accuracy);
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];
        // Do something with this sensor data.

        Log.v(TAG, "onSensorChanged: Distance = " + distance);
    }
}
