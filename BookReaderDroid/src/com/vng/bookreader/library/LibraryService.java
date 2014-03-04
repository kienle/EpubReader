package com.vng.bookreader.library;

import java.io.IOException;

import nl.siegmann.epublib.domain.Book;

public interface LibraryService {
	public void storeBook(String fileName, Book book, boolean updateLastRead, boolean copyFile) throws IOException;
	
	public void updateReadingProgress(String fileName, int progress);
	
	public QueryResult<LibraryBook> findAllByLastRead();
	
	public QueryResult<LibraryBook> findAllByLastAdded();
	
	public QueryResult<LibraryBook> findAllByTitle();
	
	public QueryResult<LibraryBook> findAllByAuthor();
	
	public QueryResult<LibraryBook> findUnread();
	
	public LibraryBook getBook(String fileName);
	
	public boolean hasBook(String fileName);
	
	public void deleteBook(String fileName);
	
	public void close();
}
