package aernbau.eden;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import self.philbrown.droidQuery.$;
import self.philbrown.droidQuery.AjaxOptions;
import self.philbrown.droidQuery.Function;

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
        title = (TextView)findViewById(R.id.title_textview);
        listeners = (TextView)findViewById(R.id.listeners_json_textview);
        current = (TextView)findViewById(R.id.current_json_textview);
        previous = (TextView)findViewById(R.id.previous_played_json_textview);
        listn = (TextView)findViewById(R.id.listeners_textview);
        discjockey = (TextView)findViewById(R.id.dj_json_textview);

        // Custom font and setting it
        font = Typeface.createFromAsset(getAssets(), "Lato-Regular.ttf");
        title.setTypeface(font); listeners.setTypeface(font);
        current.setTypeface(font); previous.setTypeface(font);
        listn.setTypeface(font); discjockey.setTypeface(font);

        // Setting up the image to be a button
        play = (ImageView)findViewById(R.id.toggle_player);
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

    // Runnable object that checks JSON every 8 seconds
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            if (activeNetwork != null && activeNetwork.isConnected()) {
                allow_playing = true;
                // ajax function for the actual retrieval
                // and UI modifications
                $.ajax(new AjaxOptions().url("http://www.edenofthewest.com/ajax/status.php")
                    .type("GET")
                    .dataType("JSON")
                    .context(Eden.this)
                    .success(new Function() {
                        @Override
                        public void invoke($ droid, Object... args) {
                            JSONObject json = (JSONObject) args[0];
                            try {
                                latest_json = json.toString();
                                if (!latest_json.equals(previous_json)) {
                                    listeners.setText(json.getString("listeners"));
                                    if (!current.getText().equals(json.getString("current"))) {
                                        editor = shared_prefs.edit();
                                        editor.putString("current_json", json.getString("current"));
                                        editor.commit();
                                    }
                                    current.setText(json.getString("current"));
                                    //If current song unequal to json current song, that's a new song

                                    discjockey.setText(json.getString("dj"));
                                    String prev = "Previously played:\n";
                                    JSONArray jsonArray = json.getJSONArray("lastplayed");
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        prev += jsonArray.getString(i) + "\n";
                                    }
                                    previous.setText(prev);
                                }
                                previous_json = json.toString();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .error(new Function() { //This was only used for testing.
                        @Override
                        public void invoke($ droid, Object... args) {
                            String reason = (String) args[2];
                            //droid.toast("Error - " + reason, Toast.LENGTH_LONG);
                        }
                    }));
            } else {
                allow_playing = false;
            }
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

