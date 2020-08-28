package com.codecubers.opensource.giffiledecoder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

	private String[] sampleNames;
	GifImageView imageView;
	TextView textView;
	File file;
	long fileSize;
	

	/*
	 * You have to manually stop playback in Activity events, otherwise it will continue 
	 * running thread in background. I'm doing stop, not pause, to release all held resources,
	 * even though it makes views lose playback position.
	 */
	
	@Override
	protected void onPause() {
		super.onPause();
		GifImageView.stopAll();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (file != null)
			imageView.play();
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		int[] sampleIds = new int[]{R.raw.test, R.raw.dispose_last, R.raw.dispose_bg,
				R.raw.dispose_prev, R.raw.interlaced};
		
		sampleNames = new String[] { "test", "dispose_last", "dispose_bg",
				"dispose_prev", "interlaced" };
	
		// Copy all the files to disk (badly, but it's a sample isn't it)
		
		File cacheDir = Utils.getDir(Utils.getCacheDir(this), "gif-cache");
		for (int i = 0; i < sampleIds.length; i++) {
			file = new File(cacheDir, sampleNames[i] + ".gif");
			
			try {
				InputStream input = getResources().openRawResource(sampleIds[i]);
				OutputStream output = new FileOutputStream(file);
				
				byte[] buffer = new byte[1024];
				int len;
				while ((len = input.read(buffer)) != -1) {
					output.write(buffer, 0, len);
				}
				
				input.close();
				output.close();
				
			} catch (Exception ex) {
				Utils.logError("test", ex, null);
			}
		}
		
		// Performance test
		/*
		try {
			
			android.util.Log.i("test", "started");
			long time1 = System.currentTimeMillis();

			GifFileDecoder decoder = new GifFileDecoder(new File(cacheDir, "anim5.gif"));
			decoder.start();
			
			for (int i = 0; i < 20; i++)
				decoder.readFrame();
			
			decoder.stop();
			
			long time2 = System.currentTimeMillis();
			android.util.Log.i("test", "finished in " + (time2-time1));
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		*/
		
		// Prepare UI
		
		textView = findViewById(R.id.textView);
		
		imageView = findViewById(R.id.imageView);
		
		GifImageView.diagnosticsCallback = value -> runOnUiThread(
				() -> textView.setText("file size: " + (fileSize / 1024) + " k\n" + value));
		
		Button play = findViewById(R.id.play);
		play.setOnClickListener(v -> imageView.play());
		
		Button stop = findViewById(R.id.stop);
		stop.setOnClickListener(v -> imageView.stop());
		
		Button pause = findViewById(R.id.pause);
		pause.setOnClickListener(v -> imageView.pause());
		
		Button browser = findViewById(R.id.browser);
		browser.setOnClickListener(v -> {
			if (file != null) {
				Intent intent = new Intent(MainActivity.this, BrowserActivity.class);
				intent.setData(Uri.fromFile(file));
				startActivity(intent);
			}
		});
		
		// Start with this sample

		openSample(sampleNames[0]);
//		openSample(sampleNames[1]);
//		openSample(sampleNames[2]);
//		openSample(sampleNames[3]);
//		openSample(sampleNames[4]);
	}

	private void openSample(String name) {
		File cacheDir = Utils.getDir(Utils.getCacheDir(this), "gif-cache");
		file = new File(cacheDir, name + ".gif");
		fileSize = file.length();

		imageView.setFile(file);
		imageView.play();

		textView.setText("collecting data");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		for (String name : sampleNames)
			menu.add(name);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		openSample(item.getTitle().toString());
		return super.onOptionsItemSelected(item);
	}
}
