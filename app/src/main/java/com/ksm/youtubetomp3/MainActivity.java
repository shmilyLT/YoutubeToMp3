package com.ksm.youtubetomp3;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    static boolean check = false;
    static File outputFile;
    String download_url, songTitle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                try {
                    handleSendText(intent); // Handle text being sent
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }
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
        String htmlData = new GetUrlContentTask().execute("https://ytmp3.cc").get();
        api_key = htmlData.substring(htmlData.indexOf("js/converter-1.0.js?") + 22, htmlData.indexOf("&=_"));
        System.err.println(api_key);

        // Get Hash
        String hash = "https://d.ymcdn.cc/check.php?v=" + videoId + "&f=mp3&k=" + api_key + "&_=1";
        check = true;
        String hashResult = new GetUrlContentTask().execute(hash).get();
        check = false;
        System.out.println(hashResult);
        JSONObject json = new JSONObject(hashResult);
        String songHash = json.getString("hash");
        songTitle = json.getString("title");

        // Download url
         download_url = "https://yyd.ymcdn.cc/" + songHash + "/" + videoId;

        // Logic for downloading to storage
//        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//        ClipData clip = ClipData.newPlainText("", download_url);
//        clipboard.setPrimaryClip(clip);
//        startActivity(new Intent(Intent., Uri.parse(download_url)));
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.e("Permission error","You have permission");
                download_song();
            } else {
                Log.e("Permission error","You have asked for permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                download_song();
            }
        }
        else { //you dont need to worry about these stuff below api level 23
            Log.e("Permission error","You already have the permission");
            download_song();
        }
    }

    public void download_song() {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(download_url));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, songTitle + ".mp3");
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    private class GetUrlContentTask extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... urls) {
            URL url = null;
            try {
                url = new URL(urls[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                connection.setRequestMethod("GET");
                if (check) {
                connection.setRequestProperty("Referer", "https://ytmp3.cc");
                }
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            try {
                connection.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String content = "", line;
            try {
                while ((line = rd.readLine()) != null) {
                    content += line + "\n";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return content;
        }
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
}
