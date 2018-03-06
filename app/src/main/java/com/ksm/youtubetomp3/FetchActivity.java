package com.ksm.youtubetomp3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchActivity extends AppCompatActivity {
    private static boolean check = false;
    private String download_url, songTitle, sid;

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

    public void downloadLogic(String URL){
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri Download_Uri = Uri.parse(URL);
        DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(true);
        request.setTitle(songTitle);
        request.setDescription("Downloading song from youtube");
        request.setVisibleInDownloadsUi(true);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC,  songTitle + ".mp3");
        downloadManager.enqueue(request);
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

        String[] n = new String[]{"odg","ado","jld","tzg","uuj","bkl",
                "fnw","eeq","ebr","asx","ghn","eal",
                "hrh","quq","zki","tff","aol","eeu",
                "kkr","yui","yyd","hdi","ddb","iir",
                "ihi","heh","xaa","nim","omp","eez",
                "rpx","cxq","typ","amv","rlv","xnx",
                "vro","pfg"};

        String sid_request = new FetchActivity.GetUrlContentTask().execute("https://d.ymcdn.cc/progress.php?id=" + songHash).get();
        JSONObject json2 = new JSONObject(sid_request);
        sid = json2.getString("sid");
        String prefix = n[Integer.parseInt(sid)-1];
        // Download url
        download_url = "https://" + prefix + ".ymcdn.cc/" + songHash + "/" + videoId;

        //Download logic here
        downloadLogic(download_url);
        finish();
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
                HttpURLConnection connection;
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
