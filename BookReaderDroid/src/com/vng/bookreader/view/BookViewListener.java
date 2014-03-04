package com.vng.bookreader.view;

import com.vng.bookreader.selection.SelectionWord;

import nl.siegmann.epublib.domain.Book;

/**
 * Listener interface for updates from a BookView.
 * @author 
 *
 */
public interface BookViewListener {

	/**
	 * Called after the Bookview has successfully parsed the book.
	 * 
	 * @param book
	 */
	void bookOpened(Book book);

	/**
	 * Called if the book could not be opened for some reason.
	 * 
	 * @param errorMessage
	 */
	void errorOnBookOpening(String errorMessage);
	
	/**
	 * Called when the BookView starts parsing a new entry
	 * of the book. Usually after a pageUp or pageDown event.
	 * 	
	 * @param entry
	 * @param name
	 */
	void parseEntryStart(int entry);
	
	/**
	 * Called after parsing is complete.
	 * 
	 * @param entry
	 * @param name
	 */
	void parseEntryComplete(int entry, String name);
	
	/** Indicates how far we've progressed in the book **/
	void progressUpdate(int progressPercentage);
	
	/**
	 * Generated when the user long-presses on a word in the text
	 * 
	 * @param word the selected word.
	 */
	void onWordLongPressed(SelectionWord selectionWord);
	
	/**
	 * Generated when the user swipes upward.
	 * 
	 * @return true if the event was handled.
	 */
	boolean onSwipeUp();
	
	/**
	 * Generated when the user swipes downward.
	 * 
	 * @return true if the event was handled.
	 */
	boolean onSwipeDown();
	
	/**
	 * Generated when the user from right to left.
	 * 
	 * @return true if the event was handled.
	 */
	boolean onSwipeLeft();
	
	/**
	 * Generated when the user swipes from left to right.
	 * 
	 * @return true if the event was handled.
	 */
	boolean onSwipeRight();
	
	/**
	 * Generated when the user taps left edge of the screen.
	 * 
	 * @return true if the event was handled.
	 */
	boolean onTapLeftEdge();
	
	/**
	 * Generated when the user taps the right edge of the screen.
	 * 
	 * @return true if the event was handled.
	 */
	boolean onTapRightEdge();
	
	/**
	 * Generated when the user taps the top edge of the screen.
	 * 
	 * @return true if the event was handled.
	 */
	boolean onTapTopEdge();
	
	/**
	 * Generated when the user taps the bottom edge of the screen.
	 * 
	 * @return true if the event was handled.
	 */
	boolean onTapBottomEdge();
	
	/**
	 * Generated when the user slides a finger along the screen's left edge.
	 * 
	 * @param value how far the user has slid.
	 */
	boolean onLeftEdgeSlide( int value );
	
	/**
	 * Generated when the user slides a finger along the screen's right edge.
	 * 
	 * @param value how far the user has slid.
	 */
	boolean onRightEdgeSlide( int value );
	
	/**
	 * Called when the user touches the screen.
	 * 
	 * This will always be called when the user taps the screen, even
	 * when an edge is tapped.
	 */
	void onScreenTap();
}
