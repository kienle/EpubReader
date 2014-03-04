package com.vng.bookreader.view;

import com.vng.bookreader.R;
import com.vng.bookreader.library.LibraryBook;
import com.vng.bookreader.library.QueryResult;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.GridView;

public class BookCaseView extends GridView {
	private Bitmap background;
	
	private int shelfWidth;
	private int shelfHeight;
	
	private QueryResult<LibraryBook> result;
	
	private LibraryBook selectedBook;

	public BookCaseView(Context context, AttributeSet attributes) {
		super(context, attributes);
		
		this.setFocusableInTouchMode(true);
		this.setClickable(false);
		
		final Bitmap shelfBackground = BitmapFactory.decodeResource(context.getResources(), R.drawable.shelf_single);
		
		this.setBackground(shelfBackground);
		
		this.setFocusable(true);
	}

	public void setBackground(Bitmap background) {
		this.background = background;
		
		shelfWidth = this.background.getWidth();
		shelfHeight = this.background.getHeight();
	}
	
	protected void onClick(int bookIndex) {
		LibraryBook book = this.result.getItemAt(bookIndex);
		
		this.selectedBook = book;
		this.invalidate();
	}
	
	public void scrollToChild(int index) {
		System.out.println("Scrolling to child " + index);
		
		int y = this.getChildAt(index).getTop();
		int delta = y - this.getScrollY();
		
		this.scrollBy(0, delta);
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		final int count = this.getChildCount();
		final int top = count > 0 ? this.getChildAt(0).getTop() : 0;
		final int width = this.getWidth();
		final int height = this.getHeight();
		
		for (int x = 0; x < width; x += this.shelfWidth) {
			for (int y = top; y < height; y += this.shelfHeight) {
				canvas.drawBitmap(this.background, x, y, null);
			}
			
			//This draws the top pixels of the shelf above the current one
			
			Rect source = new Rect(0, this.shelfHeight - top, this.shelfWidth, this.shelfHeight);
			Rect dest = new Rect(x, 0, x + this.shelfWidth, top);
			
			canvas.drawBitmap(this.background, source, dest, null);
		}
		
		super.dispatchDraw(canvas);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && this.selectedBook != null) {
			this.selectedBook = null;
			this.invalidate();
			
			return true;
		}
		
		return false;
	}
}
