package com.ksm.youtubetomp3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Khurram Saeed Malik on 02/03/2018.
 */

public class FetchActivity extends AppCompatActivity {
    private static boolean check = false;
    private String download_url, songTitle;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.e("Permission error", "You have permission");
            } else {
                Log.e("Permission error", "You have asked for permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else { //you dont need to worry about these stuff below api level 23
            Log.e("Permission error", "You already have the permission");
        }

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                try {
                    handleSendText(intent); // Handle text being sent
                } catch (ExecutionException | InterruptedException | JSONException | IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }

    }

    void downloadLogic(String mUrl) {
        InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, mUrl,
                new Response.Listener<byte[]>() {
                    @Override
                    public void onResponse(byte[] response) {
                        // TODO handle the response
                        try {
                            if (response!=null) {

                                FileOutputStream outputStream;
                                String name=songTitle+".mp3";
                                outputStream = openFileOutput(name, Context.MODE_PRIVATE);
                                outputStream.write(response);
                                outputStream.close();
                                Toast.makeText(getApplicationContext(), "Download complete.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            Log.d("KEY_ERROR", "UNABLE TO DOWNLOAD FILE");
                            e.printStackTrace();
                        }
                    }
                } ,new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO handle the error
                error.printStackTrace();
            }
        }, null);
        RequestQueue mRequestQueue = Volley.newRequestQueue(getApplicationContext(), new HurlStack());
        mRequestQueue.add(request);

    }

    void handleSendText(Intent intent) throws ExecutionException, InterruptedException, JSONException, IOException {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            fetchMP3(sharedText);
        }
    }

    private void fetchMP3(String sharedText) throws ExecutionException, InterruptedException, JSONException, IOException {
        System.err.println("SHARED TEXT: " + sharedText);
        String videoId = youtubeVidId(sharedText);
        String api_key = null;
        // Instantiate the RequestQueue.
        String htmlData = new FetchActivity.GetUrlContentTask().execute("https://ytmp3.cc").get();
        api_key = htmlData.substring(htmlData.indexOf("js/converter-1.0.js?") + 22, htmlData.indexOf("&=_"));
        System.err.println(api_key);

        // Get Hash
        String hash = "https://d.ymcdn.cc/check.php?v=" + videoId + "&f=mp3&k=" + api_key + "&_=1";
        check = true;
        String hashResult = new FetchActivity.GetUrlContentTask().execute(hash).get();
        check = false;
        System.out.println(hashResult);
        JSONObject json = new JSONObject(hashResult);
        String songHash = json.getString("hash");
        songTitle = json.getString("title");

        // Download url
        download_url = "https://yyd.ymcdn.cc/" + songHash + "/" + videoId;

        //Download logic here
        downloadLogic(download_url);
    }

    public static String youtubeVidId(String ytUrl) {
        String vidId = null;
        Pattern pattern = Pattern.compile(
                "^https?://.*(?:youtu.be/|v/|u/\\w/|embed/|watch?v=)([^#&?]*).*$",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ytUrl);
        if (matcher.matches()) {
            vidId = matcher.group(1);
        }
        return vidId;
    }

    @SuppressLint("StaticFieldLeak")
    private class GetUrlContentTask extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... urls) {
            URL url = null;
            String content = "", line;
            try {
                url = new URL(urls[0]);
                HttpURLConnection connection = null;
                connection = (HttpURLConnection) url.openConnection();
                assert connection != null;
                connection.setRequestMethod("GET");
                if (check) {
                    connection.setRequestProperty("Referer", "https://ytmp3.cc");
                }
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();
                BufferedReader rd = null;
                rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                assert rd != null;
                while ((line = rd.readLine()) != null) {
                    content += line + "\n";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return content;
        }
    }

}
