package com.ksm.youtubetomp3;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    String sharedText;
    TextView updateUi;
    WebView browser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        browser = (WebView) findViewById(R.id.activity_main_webview);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.setWebChromeClient(new WebChromeClient());

        updateUi = (TextView) findViewById(R.id.activity_main_textview);
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            }
        }

    }

    public void onPageFinished(WebView view, String url) {

        view.loadUrl("javascript:"
                + "document.getElementById('input').value = '" + url + "';");
    }

    void handleSendText(Intent intent) {
        sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            if (sharedText.contains("youtube") || sharedText.contains("youtu.be")) {
                // Update UI to reflect text being shared
                fetchMP3();
            }
            //updateUi.setText(sharedText);


        }
    }

    private void fetchMP3() {

        updateUi.setText(sharedText);

        browser.loadUrl("https://ytmp3.cc");
        onPageFinished(browser, sharedText);
    }
}
