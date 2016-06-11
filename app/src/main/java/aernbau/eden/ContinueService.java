package aernbau.eden;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.IOException;

public class ContinueService extends Service {
    private MediaPlayer audio_stream;
    private SharedPreferences shared_prefs;
    private String current_song;
    private Handler notif_handler; private PendingIntent pi;
    private static final int notif_id=774;

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

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        notif_handler = new Handler();

        shared_prefs = this.getSharedPreferences("edenPreferences", Context.MODE_PRIVATE);

        // Running on Wifi
        WifiManager wMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        final WifiManager.WifiLock wLock = wMan.createWifiLock(WifiManager.WIFI_MODE_FULL, "Locker");

        // The audio player for audio stream.
        audio_stream = new MediaPlayer();
        audio_stream.setAudioStreamType(AudioManager.STREAM_MUSIC);
        audio_stream.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        final String eden_url = "http://edenofthewest.com:8080/eden.mp3"; // EDEN stream

        pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), Eden.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // If there's no audio playing, play it
        // this is in case someone presses the button too fast.
        if(!audio_stream.isPlaying())
            try {
                audio_stream.setDataSource(eden_url);
                audio_stream.prepareAsync(); // might take long! (for buffering, etc)
                wLock.acquire();
                notif_handler.postDelayed(updateNotification, 0);

            } catch (IOException e) {
                e.printStackTrace();
            }
        else{
            audio_stream.stop();
            audio_stream.reset();
            wLock.release();
            stopForeground(true);
        }

        audio_stream.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                audio_stream.start();
            }
        });
    }

    @Override
    public void onDestroy() {
        audio_stream.stop();
        audio_stream.reset();
        notif_handler.removeCallbacks(updateNotification);
    }

    // Creates/replaces the notification for the player.
    private Runnable updateNotification = new Runnable()
    {
        public void run()
        {
            if(!shared_prefs.getString("current_json", "null").equals(current_song)) {
                current_song = shared_prefs.getString("current_json", "null");
                Notification notification = new Notification();
                notification.tickerText = "eden";
                notification.icon = R.drawable.main_icon;
                notification.flags |= Notification.FLAG_ONGOING_EVENT;
                notification.setLatestEventInfo(getApplicationContext(), "Eden",
                        shared_prefs.getString("current_json", "Offline"), pi);
                // "Playing: " + current_string, pi);
                startForeground(notif_id, notification);
            }
            notif_handler.postDelayed(this, 8000);
        }
    };
}
