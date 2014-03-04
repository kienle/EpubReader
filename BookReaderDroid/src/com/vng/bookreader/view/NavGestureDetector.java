package com.vng.bookreader.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vng.bookreader.selection.SelectionWord;

import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Translates low-level touch and gesture events into more high-level
 * navigation events.
 * 
 * @author Alex Kuiper
 *
 */
public class NavGestureDetector extends GestureDetector.SimpleOnGestureListener {
	private static Logger LOG = LoggerFactory.getLogger(NavGestureDetector.class);
	
	//Distance to scroll 1 unit on edge slide.
	private static final int SCROLL_FACTOR = 50;
	
	private BookViewListener bookViewListener;
	private BookView bookView;
	private DisplayMetrics metrics;
	
	

	public NavGestureDetector(BookView bookView, BookViewListener navListener, DisplayMetrics metrics) {
		this.bookView = bookView;
		this.bookViewListener = navListener;
		this.metrics = metrics;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (bookView.hasLinkAt(e.getX(), e.getY())) {
			return false;
		}
		
		final int TAP_RANGE_H = this.bookView.getWidth() / 5;
		final int TAP_RANGE_V = this.bookView.getHeight() / 5;
		
		if (e.getX() < TAP_RANGE_H) {
			return this.bookViewListener.onTapLeftEdge();
		} else if (e.getX() > this.bookView.getWidth() - TAP_RANGE_H) {
			return this.bookViewListener.onTapRightEdge();
		}
		
		int yBase = this.bookView.getScrollY();	
		
		if (e.getY() < TAP_RANGE_V + yBase) {
			this.bookViewListener.onTapTopEdge();
		} else if (e.getY() > (yBase + this.bookView.getHeight()) - TAP_RANGE_V ) {
			this.bookViewListener.onTapBottomEdge();
		}
		
		this.bookViewListener.onScreenTap();
		
		return false;
	}
	
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		
		float scrollUnitSize = SCROLL_FACTOR * this.metrics.density;
		
		final int TAP_RANGE_H = bookView.getWidth() / 5;
		float delta = (e1.getY() - e2.getY()) / scrollUnitSize;
		int level = (int)delta;
		
		if (e1.getX() < TAP_RANGE_H) {			
			return this.bookViewListener.onLeftEdgeSlide(level);
		} else if (e1.getX() > bookView.getWidth() - TAP_RANGE_H) {
			return this.bookViewListener.onRightEdgeSlide(level);
		}
		
		return super.onScroll(e1, e2, distanceX, distanceY);
	}
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		
		
		float distanceX = e2.getX() - e1.getX();
		float distanceY = e2.getY() - e1.getY();
		
		if (Math.abs(distanceX) > Math.abs(distanceY)) {
			if (distanceX > 0) {
				return this.bookViewListener.onSwipeRight();
			} else {
				return this.bookViewListener.onSwipeLeft();
			}
		} else if (Math.abs(distanceX) < Math.abs(distanceY)) {
			if (distanceY > 0) {
				this.bookViewListener.onSwipeUp();
			} else {
				this.bookViewListener.onSwipeDown();
			}
		}
		
		return false;
	}
	
	@Override
	public void onLongPress(MotionEvent e) {
		LOG.info(String.format("onLongPress(x, y): %f %f", e.getX(), e.getY()));
		
		SelectionWord selectionWord = this.bookView.getWordAt(e.getX(), e.getY());
		
		if (selectionWord != null) {
			this.bookViewListener.onWordLongPressed(selectionWord);
		}
	}
}
