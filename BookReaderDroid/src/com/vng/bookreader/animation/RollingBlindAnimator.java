package com.vng.bookreader.animation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Rolling-blind autoscroll Animator.
 * 
 * Slowly unveils the new page on top of the old one.
 * 
 * @author Alex Kuiper
 *
 */
public class RollingBlindAnimator implements Animator {

	private Bitmap backgroundBitmap;
	private Bitmap foregroundBitmap;
	
	private int count;	
		
	private int animationSpeed;
	
	private static final int MAX_STEPS = 500;
		
	@Override
	public void advanceOneFrame() {
		count++;		
	}
	
	@Override
	public void draw(Canvas canvas) {
		if ( backgroundBitmap != null ) {
			
			float percentage = (float) count / (float) MAX_STEPS;
			
			int pixelsToDraw = (int) (backgroundBitmap.getHeight() * percentage); 	
			
			Rect top = new Rect( 0, 0, backgroundBitmap.getWidth(), pixelsToDraw );
						
			canvas.drawBitmap(foregroundBitmap, top, top, null);
			
			Rect bottom = new Rect( 0, pixelsToDraw, backgroundBitmap.getWidth(), backgroundBitmap.getHeight() );
			
			canvas.drawBitmap(backgroundBitmap, bottom, bottom, null);
			
			Paint paint = new Paint();
			paint.setColor(Color.GRAY);
			paint.setStyle(Paint.Style.STROKE);
			
			canvas.drawLine(0, pixelsToDraw, backgroundBitmap.getWidth(), pixelsToDraw, paint);
		}		
	}
	
	@Override
	public boolean isFinished() {
		return backgroundBitmap == null
			|| count >= MAX_STEPS; 
	}
	
	@Override
	public void stop() {
		this.backgroundBitmap = null;		
	}
	
	@Override
	public int getAnimationSpeed() {
		return this.animationSpeed;
	}	
	
	public void setBackgroundBitmap(Bitmap backgroundBitmap) {
		this.backgroundBitmap = backgroundBitmap;
	}
	
	public void setForegroundBitmap(Bitmap foregroundBitmap) {
		this.foregroundBitmap = foregroundBitmap;
	}	
	
	public void setAnimationSpeed(int animationSpeed) {
		this.animationSpeed = animationSpeed;
	}
	
}
