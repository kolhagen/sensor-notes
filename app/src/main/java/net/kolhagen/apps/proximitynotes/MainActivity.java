package net.kolhagen.apps.proximitynotes;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private SensorManager mSensorManager;
    private Sensor mProximity;

    private TextView txText = null;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        // TODO: Dynamically ask for permissions
        // TODO: Check if came from notification (+ delete it)

        this.txText = (TextView) this.findViewById(R.id.text);
        this.txText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //ProximityService.startService(MainActivity.this);
                Intent serviceIntent = new Intent(MainActivity.this, SensorService.class);
                MainActivity.this.startService(serviceIntent);
            }
        });

        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        this.mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        this.mProximity = this.mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        Log.v(TAG, "getMaximumRange = " + this.mProximity.getMaximumRange());
        Log.v(TAG, "getMinDelay = " + this.mProximity.getMinDelay());
        Log.v(TAG, "getName = " + this.mProximity.getName());
        Log.v(TAG, "getPower = " + this.mProximity.getPower());
        Log.v(TAG, "getResolution = " + this.mProximity.getResolution());
        Log.v(TAG, "getType = " + this.mProximity.getType());
        Log.v(TAG, "getVendor = " + this.mProximity.getVendor());
        Log.v(TAG, "getVersion = " + this.mProximity.getVersion());
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

        this.txText.setText("Distance is " + distance);
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        // this.mSensorManager.registerListener(this, this.mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        this.mSensorManager.unregisterListener(this);
    }
}
