package com.vng.bookreader.view;

import com.vng.bookreader.animation.Animator;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AnimatedImageView extends ImageView {
	private Animator animator;

	public AnimatedImageView(Context context, AttributeSet attributes) {
		super(context, attributes);
	}

	public void setAnimator(Animator animator) {
		this.animator = animator;
	}
	
	public Animator getAnimator() {
		return animator;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (this.animator != null) {
			this.animator.draw(canvas);
		}
	}
}
