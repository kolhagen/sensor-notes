package net.kolhagen.apps.proximitynotes;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = SensorService.class.getSimpleName();

    private SensorManager sensorManager = null;
    private Sensor sensor = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");

        this.sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        this.sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        this.sensorManager.registerListener(this, this.sensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        /*
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent bIntent = new Intent(MyService.this, MainActivity.class);
        PendingIntent pbIntent = PendingIntent.getActivity(MyService.this, 0 , bIntent, 0);
        NotificationCompat.Builder bBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Title")
                        .setContentText("Subtitle")
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setContentIntent(pbIntent);
        barNotif = bBuilder.build();
        this.startForeground(1, barNotif);
        // ------
        //Notification.Builder notification = new Notification.Builder(this);

        Notification notification = new Notification(R.drawable.icon, getText(R.string.ticker_text),
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getText(R.string.notification_title),
                getText(R.string.notification_message), pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification);
*/
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");

        return null;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");

        if (this.sensorManager != null)
            this.sensorManager.unregisterListener(this);

        super.onDestroy();
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.

        Log.v(TAG, "onAccuracyChanged: Accuracy = " + accuracy);
    }

    private boolean lastProximity = false; // false: long, true: close
    private long lastChange = System.currentTimeMillis();
    private int step = 0;

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // Do something with this sensor data.

        float distance = event.values[0];
        long interval = System.currentTimeMillis() - this.lastChange;
        boolean proximity = (distance < 0.5);

        Log.v(TAG, "onSensorChanged: Distance = " + distance + ", Interval = " + interval);
        //Log.v(TAG, "- LPRX = " + lastProximity);
        //Log.v(TAG, "- PROX = " + proximity);
        //Log.v(TAG, "- STEP = " + step);

        // TODO: Fix gesture!
        if (step == 3 && this.lastProximity && !proximity) {
            Log.v(TAG, "RESET / STOP RECORDING");
            step = 0;

            // stop recording
            RecordService.stop(this);
        } else if (step == 0 && !this.lastProximity && proximity) {
            Log.v(TAG, "STEP 0 -> 1");
            step++;
        } else if (step == 1 && this.lastProximity && !proximity && interval > 2000) {
            Log.v(TAG, "STEP 1 -> 2");
            step++;
        } else if (step == 2 && !this.lastProximity && proximity && interval < 2000) {
            Log.v(TAG, "STEP 2 -> 3 / START RECORDING");
            step++;
            RecordService.start(this);
        } else {
            Log.v(TAG, "STEP = 0");
            step = 0;
        }

        this.lastProximity = (distance < 0.5);
        this.lastChange = System.currentTimeMillis();
    }
}
