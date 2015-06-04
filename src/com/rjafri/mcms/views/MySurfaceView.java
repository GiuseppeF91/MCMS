package com.rjafri.mcms.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback {

	public MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public MySurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MySurfaceView(Context context) {
		super(context);
		init();
	}
	
	private void init() {
		getHolder().addCallback(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (callback != null)
			callback.surfaceChanged(this, holder, format, width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (callback != null)
			callback.surfaceCreated(this, holder);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (callback != null)
			callback.surfaceDestroyed(this, holder);
	}
	
	private Callback callback = null;
	
	public void setCallback(Callback callback) {
		this.callback = callback;
	}
	
	public abstract interface Callback {
		public abstract void surfaceChanged(MySurfaceView view, SurfaceHolder holder, int format, int width, int height);
		public abstract void surfaceCreated(MySurfaceView view, SurfaceHolder holder);
		public abstract void surfaceDestroyed(MySurfaceView view, SurfaceHolder holder);
	}

}
