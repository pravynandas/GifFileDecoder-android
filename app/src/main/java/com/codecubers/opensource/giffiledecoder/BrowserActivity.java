package com.codecubers.opensource.giffiledecoder;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;


public class BrowserActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browser);
		
		Uri uri = getIntent().getData();
		
		WebView webView = findViewById(R.id.webView);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setUseWideViewPort(true);
		if (uri != null) {
			webView.loadUrl(uri.toString());
		} else {
			Toast.makeText(this, "Uri cannot be null", Toast.LENGTH_SHORT).show();
		}
	}

}
