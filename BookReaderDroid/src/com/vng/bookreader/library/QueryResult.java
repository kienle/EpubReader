package com.vng.bookreader.library;

import android.database.Cursor;

public abstract class QueryResult<T> {
	private Cursor wrappedCursor;
	
	private int limit = -1;

	public QueryResult(Cursor cursor) {
		this.wrappedCursor = cursor;
		
		cursor.moveToFirst();
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public int getSize() {
		int count = this.wrappedCursor.getCount();
		
		if (limit != -1 && count > limit) {
			return limit;
		}
		
		return count;
	}
	
	public T getItemAt(int index) {
		this.wrappedCursor.moveToPosition(index);
		
		return this.convertRow(this.wrappedCursor);
	}
	
	public boolean hasNext() {
		return !this.wrappedCursor.isAfterLast();
	}
	
	public void close() {
		this.wrappedCursor.close();
	}
	
	public abstract T convertRow(Cursor cursor);
}
