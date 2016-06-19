package net.kolhagen.apps.proximitynotes;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordService extends Service {
    private static final String TAG = RecordService.class.getSimpleName();

    private static final int NOTIFICATION_ONGOING_ID = 1234;
    private static final int NOTIFICATION_ID = 1235;
    private static final long MIN_DURATION = 1000;
    private static final String ACTION_START = "net.kolhagen.apps.proximitynotes.RecordService.action.START";
    private static final String ACTION_STOP = "net.kolhagen.apps.proximitynotes.RecordService.action.STOP";

    private static final String DIRECTORY_NAME = "Recordings";
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int OUTPUT_FORMAT = MediaRecorder.OutputFormat.THREE_GPP;
    private static final int AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC;
    // https://developer.android.com/guide/appendix/media-formats.html

    private MediaRecorder mediaRecorder = null;
    private long startedRecording = -1;
    private File lastRecordingFile = null;
    private Handler playSoundHandler = new Handler();

    public RecordService() {
        super(); // "RecordService"
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, RecordService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        this.onHandleIntent(intent);
        return result;
    }

    protected void onHandleIntent(Intent intent) {
        // TODO: Play sound
        // TODO: Start recording voice
        // TODO: Text-to-Speech (transcribe first word until break)
        // TODO: Foreground service (Notification), Sticky
        // TODO: Permissions

        if (intent == null)
            return;

        final String action = intent.getAction();

        if (ACTION_START.equals(action)) {
            this.startRecording();
        } else if (ACTION_STOP.equals(action)) {
            this.stopRecording();
        } else {
            Log.w(TAG, "Unknown action: " + action);
        }
    }

    private void startRecording() {
        Log.v(TAG, "startRecording - " + this);

        if (this.isRecording()) {
            Log.w(TAG, "There is already a recording going on!");
            return;
        }

        this.playSound(true);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("ProximityNotes");
        builder.setContentText("Recording... Click to stop!");
        builder.setSmallIcon(android.R.drawable.ic_btn_speak_now);
        builder.setWhen(System.currentTimeMillis());
        builder.setOngoing(true);

        Intent intent = new Intent(this, RecordService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);

        this.startForeground(NOTIFICATION_ONGOING_ID, builder.build());

        /*Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        MediaPlayer mediaPlayer = MediaPlayer.create(this, uri);
        mediaPlayer.start();*/

        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm");
            final String date = sdf.format(new Date());
            final File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DIRECTORY_NAME);

            if (!directory.exists())
                directory.mkdir();

            int number = 1;
            File audioFile;
            do {
                String numberStr = String.format("%03d", number++);
                audioFile = new File(directory.getAbsolutePath() + "/" + date + "-" + numberStr + ".3gp");
            } while (audioFile.exists());

            this.lastRecordingFile = audioFile;

            this.mediaRecorder = new MediaRecorder();
            this.mediaRecorder.setAudioSource(AUDIO_SOURCE);
            this.mediaRecorder.setOutputFormat(OUTPUT_FORMAT);
            this.mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            this.mediaRecorder.setAudioEncoder(AUDIO_ENCODER);

            this.mediaRecorder.prepare();
            this.mediaRecorder.start();

            this.startedRecording = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "Could not start recording!", e);
        }
        // TODO: Service is destroyed even tho it is foreground / https://groups.google.com/forum/#!topic/android-developers/UvolZ9g7ePw
        // TODO: Test when service is destroyed etc
    }

    private void stopRecording() {
        Log.v(TAG, "stopRecording - " + this);

        // TODO: discard recording if under 500ms or so

        if (!this.isRecording()) {
            Log.w(TAG, "There is no recording going on!");
            return;
        }

        this.stopForeground(true);

        try {
            this.mediaRecorder.stop();
            this.mediaRecorder.release();
            this.mediaRecorder = null;

            long duration = System.currentTimeMillis() - this.startedRecording;
            this.startedRecording = -1;

            File lastRecording = this.lastRecordingFile;
            this.lastRecordingFile = null;

            if (duration < MIN_DURATION) {
                Log.v(TAG, "Clip did not have the minimal duration length!");
                lastRecording.delete();
                return;
            }

            this.playSound(false);

            // TODO: Make counting notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setContentTitle("ProximityNotes");
            builder.setContentText("Recorded note success");
            builder.setSmallIcon(android.R.drawable.ic_btn_speak_now);
            builder.setWhen(System.currentTimeMillis());

            Intent intent = new Intent(this, MainActivity.class);
            intent.setAction("SHOW_RECORDING");
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            builder.setContentIntent(pendingIntent);

            NotificationManager noteManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            noteManager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Could not stop recording!", e);

            /**
             * -1007: Note that a RuntimeException is intentionally thrown to the application, if no valid audio/video data has been received when stop() is called. This happens if stop() is called immediately after start(). The failure lets the application take action accordingly to clean up the output file (delete the output file, for instance), since the output file is not properly constructed when this happens.
             */
            if (this.lastRecordingFile == null)
                return;

            this.lastRecordingFile.delete();
            this.lastRecordingFile = null;
        }
    }

    // https://github.com/GabeJacobs/Lil-Jon-App/tree/master/sounds
    private void playSound(boolean start) {
        final int resource = (start ? R.raw.record : R.raw.okay);

        this.playSoundHandler.post(new Runnable() {
            @Override
            public void run() {
                MediaPlayer mediaPlayer = MediaPlayer.create(RecordService.this, resource);
                mediaPlayer.start(); // no need to call prepare(); create() does that for you
                // mediaPlayer.release();
                // mediaPlayer = null;
            }
        });
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");

        this.stopRecording();

        super.onDestroy();
    }

    private boolean isRecording() {
        return (this.mediaRecorder != null && this.startedRecording > 0);
    }
}
