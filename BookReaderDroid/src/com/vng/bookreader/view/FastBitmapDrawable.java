package com.vng.bookreader.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class FastBitmapDrawable extends Drawable {
	private final Bitmap bitmap;

	public FastBitmapDrawable(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.drawBitmap(this.bitmap, 0.0f, 0.0f, null);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int alpha) {
		
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		
	}

	@Override
	public int getIntrinsicWidth() {
		return this.bitmap.getWidth();
	}
	
	@Override
	public int getIntrinsicHeight() {
		return this.bitmap.getHeight();
	}
	
	@Override
	public int getMinimumWidth() {
		return this.bitmap.getWidth();
	}
	
	@Override
	public int getMinimumHeight() {
		return this.bitmap.getHeight();
	}
	
	public Bitmap getBitmap() {
		return this.bitmap;
	}
}
