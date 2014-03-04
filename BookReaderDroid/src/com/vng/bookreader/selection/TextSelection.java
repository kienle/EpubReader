package com.vng.bookreader.selection;

import com.vng.bookreader.R;
import com.vng.bookreader.view.BookView;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class TextSelection {
	private Context context;
	private BookView bookView;
	private TextView textView;
	private SelectionPointerImageView startTextSelectView;
	private SelectionPointerImageView endTextSelectView;
	
	private BackgroundColorSpan bgColorSpan;
	

	public TextSelection(BookView bookView, SelectionPointerImageView startTextSelectView, SelectionPointerImageView endTextSelectView) {
		this.bookView = bookView;
		this.textView = bookView.getChildView();
		this.startTextSelectView = startTextSelectView;
		this.endTextSelectView = endTextSelectView;
		this.context = bookView.getContext();
		
		this.startTextSelectView.setOnTouchListener(new OnPointerTouchListener());
		this.endTextSelectView.setOnTouchListener(new OnPointerTouchListener());
		
		this.startTextSelectView.setPosition(0, 0);
		this.endTextSelectView.setPosition(0, 0);
		
//		highlightParagraph(20, 200);
		this.updateSelection(true);
	}
	
	public void show() {
		if (this.startTextSelectView != null) this.startTextSelectView.setVisibility(View.VISIBLE);
		if (this.endTextSelectView != null) this.endTextSelectView.setVisibility(View.VISIBLE);
	}
	
	public void hide() {
		this.clearHighlight();
		
		if (this.startTextSelectView != null) this.startTextSelectView.setVisibility(View.GONE);
		if (this.endTextSelectView != null) this.endTextSelectView.setVisibility(View.GONE);
	}
	
	public boolean isShowing() {
		return this.startTextSelectView.getVisibility() == View.VISIBLE &&
			   this.endTextSelectView.getVisibility() == View.VISIBLE;
	}
	
	private Integer findOffsetForPosition(float x, float y) {
		if (this.textView == null || this.textView.getLayout() == null) {
			return null;
		}
		
		Layout layout = this.textView.getLayout();
		int line = layout.getLineForVertical((int)y);
		
		return layout.getOffsetForHorizontal(line, x);
	}
	
	private void clearHighlight() {
		Spannable span = new SpannableString(this.textView.getText());
		
		if (this.bgColorSpan != null) {
			span.removeSpan(this.bgColorSpan);
			
			this.textView.setText(span);
		}
	}
	
	private void highlightParagraph(int start, int end) {
		this.clearHighlight();
		
		if (this.textView.length() == 0) return;
		
		start = Math.max(0, Math.min(start, this.textView.length() - 1));
		end = Math.max(0, Math.min(end, this.textView.length() - 1));
//		Log.i("highlightParagraph", String.format("%d %d", start, end));
		if (start > end) {
			int temp = start;
			start = end;
			end = temp;
		}
		
		Spannable span = new SpannableString(this.textView.getText());
		
		if (this.bgColorSpan == null) {
			this.bgColorSpan = new BackgroundColorSpan(this.context.getResources().getColor(R.color.text_selection_highlight));
		}
		span.setSpan(this.bgColorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		
		this.textView.setText(span);
	}
	
	public void doSelectionParagraph(int start, int end) {
		this.highlightParagraph(start, end);
		
		this.alignPointerPositions(start, end);
	}
	
	private Point getPoinerPositionForOffset(int pos) {
		if (this.textView.getLayout() != null) {
			Layout layout = textView.getLayout();
			int line = layout.getLineForOffset(pos);
			int baseline = layout.getLineBaseline(line);
//			int ascent = layout.getLineAscent(line);
			int x = (int)layout.getPrimaryHorizontal(pos) + this.bookView.getPaddingLeft();
//			int y = baseline + ascent;
			int y = baseline + this.bookView.getPaddingTop();
			
			return new Point(x, y);
		}
		
		return new Point(0, 0);
	}
	
	private int getValidCharOffset(int offset) {
		CharSequence text = this.textView.getText();
		int index = offset;
		
		while (index >= 0 && com.vng.bookreader.utils.TextUtils.isBoundaryCharacter(text.charAt(index))) {
			index--;
		}
		
		return index;
	}
	
	private void alignPointerPositions(int start, int end) {
		Layout layout = this.textView.getLayout();
		if (layout != null) {
			Point startPt = getPoinerPositionForOffset(start);
			Point endPt = getPoinerPositionForOffset(end);
			
			this.startTextSelectView.setPosition(startPt.x, startPt.y);
			this.endTextSelectView.setPosition(endPt.x, endPt.y);
		}
	}
	
	private void updateSelection(boolean alignPointer) {
		Integer start = this.findOffsetForPosition(this.startTextSelectView.getPosX() - this.bookView.getPaddingLeft(), 
												   this.startTextSelectView.getPosY() - this.bookView.getPaddingTop());
		Integer end = this.findOffsetForPosition(this.endTextSelectView.getPosX() - this.bookView.getPaddingLeft(), 
													this.endTextSelectView.getPosY() - this.bookView.getPaddingTop());
		
		if (start == null || end == null) return;
		
		if (alignPointer) {
//			start = getValidCharOffset(start);
//			end = getValidCharOffset(end);
			
			this.alignPointerPositions(start, end);
		}
		
		this.highlightParagraph(start, end);
	}

	private class OnPointerTouchListener implements View.OnTouchListener {
		private boolean isMoving = false;
		private int lastViewPosX;
		private int lastViewPosY;
		private int lastFingerPosX;
		private int lastFingerPosY;
		private int newPosX;
		private int newPosY;

		@Override
		public boolean onTouch(View view, MotionEvent event) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					lastFingerPosX = (int)event.getRawX();
					lastFingerPosY = (int)event.getRawY();
					
					lastViewPosX = ((SelectionPointerImageView)view).getPosX();
					lastViewPosY = ((SelectionPointerImageView)view).getPosY();
					
					isMoving = true;
					return true;
	
				case MotionEvent.ACTION_MOVE:
					if (isMoving) {
				        newPosX = lastViewPosX + ((int)event.getRawX() - lastFingerPosX);
				        newPosY = lastViewPosY + ((int)event.getRawY() - lastFingerPosY);
				        
				        newPosX = Math.max(0, newPosX);
				        newPosY = Math.max(0, newPosY);
				        ((SelectionPointerImageView)view).setPosition(newPosX, newPosY);
				        
				        //Log.i("=============", "(X, Y): (" + String.valueOf(newPosX) + ", " + String.valueOf(newPosY) + ")");
				        updateSelection(false);
					}
					
					return true;
				
				case MotionEvent.ACTION_UP:
					isMoving = false;
					updateSelection(true);
					return true;
			}
			
			return false;
		}
		
	}
}
