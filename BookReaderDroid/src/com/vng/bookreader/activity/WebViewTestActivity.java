package com.vng.bookreader.activity;

import java.io.IOException;
import java.io.InputStream;

import com.vng.bookreader.R;
import com.vng.bookreader.R.layout;
import com.vng.bookreader.R.menu;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.webkit.WebView;

public class WebViewTestActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_web_view_test);
        
        WebView webView = new WebView(this);
        setContentView(webView);
        
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultTextEncodingName("UTF-8");
        
        //webView.loadUrl("file:///android_asset/index.html");
        
        String htmlText = "";
        try {
        	InputStream is = this.getAssets().open("index.html");
        
        	int size = is.available();
        	byte[] buffer = new byte[size];
        	
        	is.read(buffer);
        	is.close();
        	
        	htmlText = new String(buffer);
        } catch (IOException e) {
        	
        }
        
        webView.loadDataWithBaseURL("file:///", htmlText, "text/html", "UTF-8", null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_web_view_test, menu);
        return true;
    }
}
