package aernbau.eden;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Eden extends Activity {

    private TextView title, listeners, current, previous, listn, discjockey;
    private ImageView play;
    private Handler mHandler;
    private SharedPreferences shared_prefs; private SharedPreferences.Editor editor;
    private String previous_json, latest_json;
    private Typeface font;
    private Intent serviceIntent;
    private Boolean stop_it, allow_playing;
    private ConnectivityManager conMgr;
    private NetworkInfo activeNetwork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_eden);

        allow_playing = false;

        mHandler = new Handler();
        serviceIntent = new Intent(this, ContinueService.class);

        //Text fields
        title = findViewById(R.id.title_textview);
        listeners = findViewById(R.id.listeners_json_textview);
        current = findViewById(R.id.current_json_textview);
        previous = findViewById(R.id.previous_played_json_textview);
        listn = findViewById(R.id.listeners_textview);
        discjockey = findViewById(R.id.dj_json_textview);

        // Custom font and setting it
        font = Typeface.createFromAsset(getAssets(), "Lato-Regular.ttf");
        title.setTypeface(font); listeners.setTypeface(font);
        current.setTypeface(font); previous.setTypeface(font);
        listn.setTypeface(font); discjockey.setTypeface(font);

        // Setting up the image to be a button
        play = findViewById(R.id.toggle_player);
        stop_it = isMyServiceRunning(ContinueService.class);
        if(stop_it) play.setImageResource(R.drawable.stop);

        // Checking for any internet connection points
        conMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = conMgr.getActiveNetworkInfo();

        // Setting image button
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(allow_playing)
                    if(stop_it){
                        play.setImageResource(R.drawable.play);
                        stop_it = !stop_it;
                        stopService(view);
                    } else {
                        play.setImageResource(R.drawable.stop);
                        stop_it = !stop_it;
                        startService(view);
                    }
            }
        });

        shared_prefs = this.getSharedPreferences("edenPreferences",Context.MODE_PRIVATE);

        // Starts JSON refresher
        startRepeatingTask();
    }

    // Checks if the service is running for button status
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    // Start the service
    public void startService(View view){
        startService(serviceIntent);
    }

    // Stop the service
    public void stopService(View view){
        stopService(serviceIntent);
    }

    // onDestroy stops the repeating JSON/UI refresher and application
    @Override
    protected void onDestroy() {
        stopRepeatingTask();
        super.onDestroy();
    }

    private class StatusChecker extends AsyncTask<String, Void, Void> {
        private JSONObject json;

        protected Void doInBackground(String ...url) {
            if (activeNetwork != null && activeNetwork.isConnected()) {
                allow_playing = true;
                // ajax function for the actual retrieval
                // and UI modifications
                this.json = JSONParser.getJSONFromUrl(url[0]);
            } else {
                allow_playing = false;
            }
            return null;
        }

        protected void onPostExecute(Void none) {
            if (this.json != null) {
                try {
                    latest_json = this.json.toString();
                    if (!latest_json.equals(previous_json)) {
                        listeners.setText(this.json.getString("listeners"));
                        if (!current.getText().equals(this.json.getString("current"))) {
                            editor = shared_prefs.edit();
                            editor.putString("current_json", this.json.getString("current"));
                            editor.apply();
                        }
                        current.setText(this.json.getString("current"));
                        //If current song unequal to json current song, that's a new song

                        discjockey.setText(this.json.getString("dj"));
                        StringBuilder prev = new StringBuilder();
                        prev.append("Previously played:\n");
                        JSONArray jsonArray = this.json.getJSONArray("lastplayed");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            prev.append(jsonArray.getString(i) + "\n");
                        }
                        previous.setText(prev);
                    }
                    previous_json = this.json.toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Network error", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    // Runnable object that checks JSON every 8 seconds
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            new StatusChecker().execute("https://www.edenofthewest.com/ajax/status.php");
            mHandler.postDelayed(mStatusChecker, 8000);
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }
}

