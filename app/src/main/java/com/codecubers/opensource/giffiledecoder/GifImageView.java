/*
 * File downloaded from:
 *   https://code.google.com/p/giffiledecoder/
 */

package com.codecubers.opensource.giffiledecoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.IntBuffer;
import java.util.concurrent.Semaphore;

public class GifImageView extends androidx.appcompat.widget.AppCompatImageView {
	
	private static final String TAG = "GifImageView";
	private static final boolean INFO = true;
	private static final boolean DEBUG = true;
	private static final boolean VERBOSE = true;

	public interface DiagnosticsCallback {
		void onDiagnostics(String value);
	}
	
	public static DiagnosticsCallback diagnosticsCallback;
	
	
	// Class methods
	
	public static final int STATE_STOPPED = 0;
	public static final int STATE_PLAYING = 1;
	public static final int STATE_PAUSED  = 2;
	
	private File file;
	
 	public GifImageView(Context context) {
 		super(context);
 	}

 	public GifImageView(Context context, AttributeSet attrs) {
 		super(context, attrs);
 	}
 	
 	public GifImageView(Context context, AttributeSet attrs, int defStyleAttr) {
 		super(context, attrs, defStyleAttr);
 	}
 	
 	public int getState() {
 		return GifImageView.getState(this);
 	}
 	
 	public void setFile(File file) {
 		if (this.file != null && getState(this) != STATE_STOPPED)
 			GifImageView.stop(this);
 		this.file = file;
 	}
 	
	public void play() {
		if (file == null) {
			Log.w(TAG, "no file");
			return;
		}
		switch (getState()) {
		case STATE_PLAYING:
			if (INFO) Log.i(TAG, "already playing");
			break;
		case STATE_STOPPED:
			GifImageView.start(this, file);
			break;
		case STATE_PAUSED:
			GifImageView.resume(this);
		}
	}

	public void pause() {
		if (getState() == STATE_PLAYING)
			GifImageView.pause(this);
		else
			Log.w(TAG, "can't pause");
	}

	public void stop() {
		if (getState() == STATE_PLAYING || getState() == STATE_PAUSED)
			GifImageView.stop(this);
		else
			Log.w(TAG, "can't stop");
	}
	
	@Override
	protected void onDetachedFromWindow() {
		stop();
		super.onDetachedFromWindow();
	}
	
	
	// Static dispatcher methods

	private static final int MSG_SETBITMAP = 0;
	private static final int MSG_REDRAW    = 1;
	private static final int MSG_FINALIZE  = 2;
	
	private static LongSparseArray<ThreadInfo> threads = new LongSparseArray<>();
	private static Handler mainHandler;

	private static class ThreadInfo {
		public WeakReference<GifImageView> view;
		public File file;
		public Semaphore pause = new Semaphore(1);
		public boolean paused = false;
	}

	private static class ThreadParam {
		public long threadId;
		public Bitmap bitmap;
	}

	static {
		mainHandler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(Message msg) {
				mainThread(msg.what, (ThreadParam) msg.obj);
			}
		};
	}

	private synchronized static void start(GifImageView view, File file) {
		if (INFO) Log.i(TAG, "start");
		
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				backgroundThread();
			}
		});

		ThreadInfo info = new ThreadInfo();
		info.view = new WeakReference<>(view);
		info.file = file;
		threads.put(thread.getId(), info);
		
		thread.start();
	}

	private synchronized static void stop(GifImageView view) {
		if (INFO) Log.i(TAG, "stop");
		ThreadInfo info = getThreadInfo(view);
		if (info != null) 
			stopThread(info);
	}
	
	public synchronized static void stopAll() {
		for (int i = 0; i < threads.size(); i++) 
			stopThread(threads.valueAt(i));
	}
	
	private static void stopThread(ThreadInfo info) {
		info.view.clear();
		if (info.paused) {
			info.pause.release();
			info.paused = false;
		}
	}
	
	private synchronized static void pause(GifImageView view) {
		if (INFO) Log.i(TAG, "pause");
		ThreadInfo info = getThreadInfo(view);
		if (info != null && !info.paused) {
			try { info.pause.acquire(); } catch (InterruptedException ignored) {}
			info.paused = true;
		}
	}
	
	private synchronized static void resume(GifImageView view) {
		if (INFO) Log.i(TAG, "resume");
		ThreadInfo info = getThreadInfo(view);
		if (info != null && info.paused) {
			info.pause.release();
			info.paused = false;
		}
	}
	
	private synchronized static int getState(GifImageView view) {
		ThreadInfo info = getThreadInfo(view);
		if (info == null)
			return STATE_STOPPED;
		if (info.paused)
			return STATE_PAUSED;
		return STATE_PLAYING;
	}

	private synchronized static ThreadInfo getThreadInfo(GifImageView view) {
		for (int i = 0; i < threads.size(); i++) {
			ThreadInfo info = threads.valueAt(i);
			GifImageView threadView = info.view.get();
			if (view.equals(threadView))
				return info;
		}
		return null;
	}

	private synchronized static ThreadInfo getThreadInfo(long threadId) {
		return threads.get(threadId);
	}

	private synchronized static void removeThread(long threadId) {
		threads.remove(threadId);
	}

	private static void backgroundThread() {
		long threadId = Thread.currentThread().getId();
		ThreadInfo info = getThreadInfo(threadId);
		if (info == null)
			return;
		
		if (DEBUG) Log.d(TAG, "started thread " + threadId);
		
		long startTime = System.currentTimeMillis();
		long infoTime = startTime + 10 * 1000;
		int delay = 0;
		long frameIndex = 0;
		long decodeTimes = 0;
		long delays = 0;
		boolean diagDone = false;
		
		GifFileDecoder decoder = new GifFileDecoder(info.file);
		try {
			decoder.start();
			
			Bitmap bitmap = Bitmap.createBitmap(decoder.getWidth(), decoder.getHeight(),
					Bitmap.Config.ARGB_8888);

			sendToMain(threadId, MSG_SETBITMAP, bitmap);
			
			while (decoder.hasFrame()) {				
				// decode frame
				long frameStart = System.currentTimeMillis();
				
				int[] pixels = decoder.readFrame();
				if (pixels == null) {
					if (DEBUG) Log.d(TAG, "null frame, stopping");
					break;
				}
				
				long decodeTime = System.currentTimeMillis() - frameStart;
				
				if (VERBOSE) Log.v(TAG, "decoded frame in " + decodeTime + " delay " + delay);

				// wait until the end of delay set by previous frame
				Thread.sleep(Math.max(0, delay - decodeTime));

				// check for pause
				info.pause.acquire();
				info.pause.release();
				
				// check if view still exists
				if (info.view.get() == null)
					break;
				
				// send frame to view
				bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixels));
				sendToMain(threadId, MSG_REDRAW, null);
				
				delay = decoder.getDelay();
				
				// some logging
				if (diagnosticsCallback != null && !diagDone) {
					frameIndex++;
					decodeTimes += decodeTime;
					delays += delay;
					if (System.currentTimeMillis() > startTime + 5 * 1000) {
						long fpsa = frameIndex * 1000 / decodeTimes;
						long fpsb = frameIndex * 1000 / delays;
						String value = "size: " + bitmap.getWidth() + " x " + bitmap.getHeight() +
								"\nfps: " + fpsa + " / " + fpsb;
						diagnosticsCallback.onDiagnostics(value);
						diagDone = true;
					}
				}
				if (System.currentTimeMillis() > infoTime) {
					if (INFO) Log.i(TAG, "Gif thread still running");
					infoTime += 10 * 1000;
				}
				if (System.currentTimeMillis() > startTime + 4 * 60 * 60 * 1000)
					throw new RuntimeException("Gif thread leaked, fix your code");
			}
			
		} catch (IOException ex) {
			Utils.logWarning(TAG, ex);
		} catch (InterruptedException ex) {
			Utils.logError(TAG, ex, null);
		} finally {
			if (DEBUG) Log.d(TAG, "stopping decoder");
			decoder.stop();
		}

		sendToMain(threadId, MSG_FINALIZE, null);
		if (DEBUG) Log.d(TAG, "finished thread " + threadId);
	}
	
	private static void sendToMain(long threadId, int what, Bitmap bitmap) {
		ThreadParam param = new ThreadParam();
		param.threadId = threadId;
		param.bitmap = bitmap;
		mainHandler.obtainMessage(what, param).sendToTarget();
	}
	
	private static void mainThread(int what, ThreadParam obj) {
		if (what == MSG_FINALIZE) {
			if (DEBUG) Log.d(TAG, "removing thread " + obj.threadId);
			removeThread(obj.threadId);
			return;
		}

		ThreadInfo info = getThreadInfo(obj.threadId);
		if (info == null) {
			if (DEBUG) Log.d(TAG, "no thread info");
			return;
		}
		GifImageView view = info.view.get();
		if (view == null) {
			if (DEBUG) Log.d(TAG, "no view");
			return;
		}

		if (what == MSG_SETBITMAP)
			view.setImageBitmap(obj.bitmap);
		else if (what == MSG_REDRAW)
			view.invalidate();
	}
    
}
