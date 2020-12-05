package aernbau.eden;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;

import static android.content.ContentValues.TAG;

public class ContinueService extends Service {
    private MediaPlayer audio_stream;
    private SharedPreferences shared_prefs;
    private String current_song;
    private Handler notif_handler; private PendingIntent pi;
    private static final int notif_id=774;
    private static String channelID;

    public ContinueService(){
    //to fix. updating of notification text.
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
    }

    public int onStartCommand(Intent intent, int flag, int startId) {
        super.onStartCommand(intent, flag, startId);

        notif_handler = new Handler();

        shared_prefs = this.getSharedPreferences("edenPreferences", Context.MODE_PRIVATE);

        // Running on Wifi
        WifiManager wMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        final WifiManager.WifiLock wLock = wMan.createWifiLock(WifiManager.WIFI_MODE_FULL, "EdenLocker");

        // The audio player for audio stream.
        audio_stream = new MediaPlayer();
        audio_stream.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                audio_stream.start();
            }
        });
        audio_stream.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });

        audio_stream.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        long currentTimeSec = System.currentTimeMillis() / 1000;
        final String eden_url = "https://www.edenofthewest.com/radio/8000/radio.mp3?"+currentTimeSec; // EDEN stream

        pi = PendingIntent.getActivity(
                getApplicationContext(), 0,
                new Intent(getApplicationContext(), Eden.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        channelID = ContinueService.createNotificationChannel(this);

        // If there's no audio playing, play it
        // this is in case someone presses the button too fast.
        if (!audio_stream.isPlaying()) {
            try {
                audio_stream.setDataSource(eden_url);
                wLock.acquire();
                audio_stream.prepareAsync();
                notif_handler.postDelayed(updateNotification, 0);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "onStart: ", e);
            } catch (IOException e) {
                Log.e(TAG, "onStart: ", e);
                e.printStackTrace();
            }
        } else {
            audio_stream.stop();
            audio_stream.reset();
            wLock.release();
            stopForeground(true);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        audio_stream.stop();
        audio_stream.reset();
        audio_stream.release();
        audio_stream = null;
        notif_handler.removeCallbacks(updateNotification);
    }

    public static String createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelID = "EdenOfTheWestChannel";
            CharSequence name = "Eden";
            String description = "Eden persist radio";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            return channelID;
        } else {
            return null;
        }
    }

    // Creates/replaces the notification for the player.
    private Runnable updateNotification = new Runnable()
    {
        public void run()
        {
            if(!shared_prefs.getString("current_json", "null").equals(current_song)) {
                current_song = shared_prefs.getString("current_json", "null");
                Notification notification = new NotificationCompat.Builder(getApplicationContext(), channelID)
                        .setSmallIcon(R.drawable.main_icon)
                        .setContentIntent(pi)
                        .setContentTitle("eden of the west")
                        .setContentText(shared_prefs.getString("current_json", "Offline"))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();
                notification.flags |= Notification.FLAG_ONGOING_EVENT;
                startForeground(notif_id, notification);
            }
            notif_handler.postDelayed(this, 8000);
        }
    };
}
