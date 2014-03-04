package com.vng.bookreader.view;

import com.vng.bookreader.epub.PageTurnerSpine;

import android.graphics.Canvas;
import android.text.Layout.Alignment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.TextView;

public class SinglePageStrategy implements PageChangeStrategy {
	private Spanned text = new SpannableString("");
	private int storedPosition = 0;
	private double storedPercentage = -1d;
	
	private BookView bookView;
	private TextView childView;
	
	//FIXME: This should really be dynamically calculated based on screen size.
	private static final int MAX_PAGE_SIZE = 5000;
	
	public SinglePageStrategy(BookView bookView) {
		this.bookView = bookView;
		this.childView = bookView.getInnerView();
	}
	
	@Override
	public int getPosition() {
		if (this.storedPosition == Integer.MAX_VALUE) {
			return 0;
		}
		
		return this.storedPosition;
	}
	
	@Override
	public void setRelativePosition(double position) {
		this.storedPercentage = position;
		updatePosition();
	}

	@Override
	public void loadText(Spanned text) {
		this.text = text;
		updatePosition();
	}

	@Override
	public boolean isAtEnd() {
		return this.getPosition() + this.childView.getText().length() >= this.text.length();
	}

	@Override
	public boolean isAtStart() {
		return this.getPosition() == 0;
	}

	@Override
	public void pageDown() {
		int oldPos = this.storedPosition;
		int totalLength = this.text.length();
		
		int textEnd = this.storedPosition + this.childView.getText().length();
		
		if (textEnd >= this.text.length() - 1) {
			PageTurnerSpine spine = bookView.getSpine();
			
			if (spine == null || !spine.navigateForward()) {
				return;
			}
			
			this.storedPosition = 0;
			this.childView.setText("");
			this.clearText();
			
			bookView.loadText();
			return;
		}
		
		if (textEnd == oldPos) {
			textEnd++;
		}
		
		this.storedPosition = Math.min(textEnd, totalLength - 1);
		
		this.updatePosition();
	}
	
	/**
	 * Get the text offset of the beginning character in a page.
	 * @param endOfPageOffset The offset of the last character in a page.
	 * @return
	 */
	private int findStartOfPage(int endOfPageOffset) {
		int endOffset = endOfPageOffset;
		
		endOffset = Math.max(0, Math.min(endOffset, this.text.length() - 1));
		
		int start = Math.max(0, endOffset - MAX_PAGE_SIZE);
		
		CharSequence cutOff = this.text.subSequence(start, endOffset);
		
		TextPaint textPaint = this.childView.getPaint();
		int boundedWidth = this.childView.getWidth();
		StaticLayout layout = new StaticLayout(cutOff, textPaint, boundedWidth, Alignment.ALIGN_NORMAL, 1.0f, this.bookView.getLineSpacing(), false);
		
		layout.draw(new Canvas());
		
		if (layout.getHeight() < bookView.getHeight()) {
			return start;
		} else {
			int topLine = layout.getLineForVertical(layout.getHeight() - (bookView.getHeight() - 2 * bookView.getVerticalMargin()));
			int offset = layout.getLineStart(topLine + 2);
			
			return start + offset;
		}
	}
	
	@Override
	public Spanned getText() {
		return this.text;
	}
	
	@Override
	public void pageUp() {
		int pageStart = this.findStartOfPage(this.storedPosition);
		
		if (pageStart == this.storedPosition) {
			if (this.bookView.getSpine() == null || !this.bookView.getSpine().navigateBack()) {
				return;
			}
			
			this.childView.setText("");
			this.clearText();
			
			this.storedPosition = Integer.MAX_VALUE;
			this.bookView.loadText();
			
			return;
		} else {
			this.storedPosition = pageStart;
		}
		
		this.updatePosition();
	}
	
	@Override
	public void clearText() {
		this.text = new SpannedString("");
		
	}
	
	@Override
	public void clearStoredPosition() {
		// Do nothing
	}
	
	@Override
	public void reset() {
		this.storedPosition = 0;
		this.text = new SpannedString("");
	}
	
	@Override
	public void updatePosition() {
		if (this.text.length() == 0) {
			return;
		}
		
		if (this.storedPercentage != -1d) {
			this.storedPosition = (int)(this.storedPercentage * this.text.length());
			this.storedPercentage = -1d;
		}
		
		if (this.storedPosition >= this.text.length()) {
			this.storedPosition = this.findStartOfPage(this.text.length() - 1);
		}
		
		this.storedPosition = Math.max(0, Math.min(this.storedPosition, this.text.length() - 1));
		
		int totalLength = this.text.length();
		int end = Math.min(this.storedPosition + MAX_PAGE_SIZE, totalLength);
		
		CharSequence cutOff = this.text.subSequence(this.storedPosition, end);
		
		TextPaint textPaint = this.childView.getPaint();
		int boundedWidth = this.childView.getWidth();
		StaticLayout layout = new StaticLayout(cutOff, textPaint, boundedWidth, Alignment.ALIGN_NORMAL, 1.0f, this.bookView.getLineSpacing(), false);
		
		layout.draw(new Canvas());
		
		int bottomLine = layout.getLineForVertical(this.bookView.getHeight() - 2 * this.bookView.getVerticalMargin());
		bottomLine = Math.max(1, bottomLine);
		
		if (layout.getHeight() >= this.bookView.getHeight() && this.text.length() > 10) {
			int offset = layout.getLineStart(bottomLine - 1);
			CharSequence section = cutOff.subSequence(0, offset);
			
			/*
			 * Special case, happens with big pictures
			 * We increase the length of the text we display until it becomes to big for
			 * the screen, then cut off 1 before that.			
			 */
			if (section.length() == 0) {
				for (int i = 1; i < cutOff.length(); i++) {
					section = cutOff.subSequence(0, i);
					layout = new StaticLayout(section, textPaint, boundedWidth, Alignment.ALIGN_NORMAL, 1.0f, this.bookView.getLineSpacing(), false);
					if (layout.getHeight() >= this.bookView.getHeight()) {
						section = cutOff.subSequence(0, i - 1);
						break;
					}
				}
			}
			
			this.childView.setText(section);
		} else {
			this.childView.setText(cutOff);
		}
	}

	@Override
	public void setPosition(int pos) {
		this.storedPosition = pos;
		
		this.updatePosition();
	}

	@Override
	public boolean isScrolling() {
		return false;
	}
}
