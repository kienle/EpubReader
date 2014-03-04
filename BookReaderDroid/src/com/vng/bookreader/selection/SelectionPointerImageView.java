package com.vng.bookreader.selection;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SelectionPointerImageView extends ImageView {

	public SelectionPointerImageView(Context context) {
		super(context);
	}

	public SelectionPointerImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public SelectionPointerImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public int getPosX() {
		FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)this.getLayoutParams();
		
		return layoutParams.leftMargin + layoutParams.width / 2;
	}
	
	public int getPosY() {
		FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)this.getLayoutParams();
		
		return layoutParams.topMargin;
	}
	
	public void setPosX(int xPos) {
		FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)this.getLayoutParams();
		layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
		layoutParams.leftMargin = xPos - layoutParams.width / 2;
        
		this.setLayoutParams(layoutParams);
	}
	
	public void setPosY(int yPos) {
		FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)this.getLayoutParams();
		layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.topMargin = yPos;
        
        this.setLayoutParams(layoutParams);
	}
	
	public void setPosition(int xPos, int yPos) {
		FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)this.getLayoutParams();
		layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
		layoutParams.leftMargin = xPos - layoutParams.width / 2;
        layoutParams.topMargin = yPos;
        
        this.setLayoutParams(layoutParams);
	}
	
}
