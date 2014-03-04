package com.vng.bookreader.view;

import android.text.Spanned;

public interface PageChangeStrategy {

	/**
	 * Loads the given section of text.
	 * 
	 * This will be a whole "file" from an epub.
	 * 
	 * @param text
	 */
	public void loadText(Spanned text);
	
	/**
	 * Returns the text-offset of the top-left character on the screen.
	 * 
	 * @return
	 */
	public int getPosition();
	
	/**
	 * Returns if we're at the start of the current section
	 * @return
	 */
	public boolean isAtStart();

	/**
	 * Returns if we're at the end of the current section
	 * @return
	 */
	public boolean isAtEnd();
	
	/**
	 * Tells this strategy to move the window so the specified
	 * position ends up on the top line of the windows.
	 * 
	 * @param pos
	 */
	public void setPosition(int pos);
	
	/**
	 * Sets a position relative to the text length:
	 * 0 means the start of the text, 1 means the end of 
	 * the text.
	 * 
	 * @param position a value between 0 and 1
	 */
	public void setRelativePosition( double position );
	
	/**
	 * Move the view one page up.
	 */
	public void pageUp();
	
	/**
	 * Move the view one page down.
	 */
	public void pageDown();
	
	/** Simple way to differentiate without instanceof **/
	public boolean isScrolling();
	
	/**
	 * Clears all text held in this strategy's buffer.
	 */
	public void clearText();
	
	/**
	 * Clears the stored position in this strategy.
	 */
	public void clearStoredPosition();
	
	/**
	 * Updates all fields to reflect a new configuration.
	 */
	public void updatePosition();
	
	/**
	 * Clears both the buffer and stored position.
	 */
	public void reset();
	
	/**
	 * Gets the text held in this strategy's buffer.
	 * 
	 * @return the text
	 */
	public Spanned getText();
}
