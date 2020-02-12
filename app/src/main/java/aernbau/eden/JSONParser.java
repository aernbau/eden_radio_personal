package aernbau.eden;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Arnas on 2015.07.11.
 * class copied from http://stackoverflow.com/questions/9605913/how-to-parse-json-in-android
 * Updated to API 28: 2020.02.12.
 */
public class JSONParser {

    static InputStream is = null;
    static JSONObject jObj = null;
    static String json = "";

    // constructor
    public JSONParser() {}

    public static JSONObject getJSONFromUrl(String endpoint) {
        // Making HTTP request
        try {
            URL url = new URL(endpoint);
            HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
            httpsConn.setConnectTimeout(0);
            httpsConn.setReadTimeout(0);
            httpsConn.setReadTimeout(0);
            httpsConn.setRequestMethod("POST");
            httpsConn.setUseCaches(false);
            httpsConn.setDoOutput(false);
            httpsConn.setDoInput(true);
            httpsConn.connect();

            final int responseCode = httpsConn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                try {
                    is = httpsConn.getInputStream();
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(is, StandardCharsets.ISO_8859_1), 8
                    );
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    is.close();
                    json = sb.toString();
                } catch (Exception e) {
                    Log.e("Buffer Error", "Error converting result " + e.toString());
                }

                // try parse the string to a JSON object
                try {
                    jObj = new JSONObject(json);
                } catch (JSONException e) {
                    Log.e("JSON Parser", "Error parsing data " + e.toString());
                }
            } else {
                // If responseCode is not HTTP_OK
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // return JSON String
        return jObj;
    }
}
