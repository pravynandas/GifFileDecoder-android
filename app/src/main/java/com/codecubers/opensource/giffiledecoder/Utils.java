package com.codecubers.opensource.giffiledecoder;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class Utils {

	private static final String TAG = "Utils";

	public static String getExceptionMessage(Throwable th) {
		String msg = th.getMessage();
		if (msg == null)
			msg = th.getClass().getSimpleName();
		return msg;
	}
	
	public static void logWarning(String tag, Throwable th) {
		Log.w(tag, getExceptionMessage(th));
	}
	
	public static void logError(String tag, Throwable th, String info) {
		Log.e(tag, getExceptionMessage(th));
		th.printStackTrace();
	}
	
	// Tries to return SD cache dir, failing that returns internal cache. No nulls.
	public static File getCacheDir(Context context) {
		String storageState = null;
		try {
			storageState = android.os.Environment.getExternalStorageState();
		} catch (NullPointerException ex) { // undocumented, but happens
			Utils.logError(TAG, ex, null);
		}

		if (storageState != null && storageState.equals(Environment.MEDIA_MOUNTED)) {
			File result = null;
			try {
				result = context.getExternalCacheDir();
			} catch (NullPointerException ex) { // undocumented, but happens
				Utils.logError(TAG, ex, null);
			}
			if (result != null)
				return result;
		}

		return context.getCacheDir();
	}

	// Returns existing dir, creates ".nomedia" file in dir. Can return null.
	public static File getDir(File parent, String name) {
		File dir = new File(parent, name);
		if (!dir.exists()) {
			if (!dir.mkdir()) {
				Log.w(TAG, "Failed to create dir " + name);
				return null;
			}
			try {
				new File(dir, ".nomedia").createNewFile();
			} catch (IOException ex) {
				Log.w(TAG, "Failed to create .nomedia in " + name);
			}
		}
		return dir;
	}
	
}
