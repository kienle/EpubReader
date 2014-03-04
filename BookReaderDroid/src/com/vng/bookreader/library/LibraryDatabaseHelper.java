package com.vng.bookreader.library;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LibraryDatabaseHelper extends SQLiteOpenHelper {
	public enum Field {
		file_name("text primary key"),
		title("text"),
		
		first_name("text"),
		last_name("text"),
		
		date_added("integer"),
		date_last_read("integer"),
		
		description("text"),
		
		cover_image("blob"),
		
		progress("integer");
		
		private String fieldDef;
		
		private Field(String fieldDef) {
			this.fieldDef = fieldDef;
		}
	}
	
	private SQLiteDatabase database;
	
	public enum Order {ASC, DESC}
	
	private static final String DB_NAME = "BookReaderLibrary";
	private static final int VERSION = 4;
	
	private static final Logger LOG = LoggerFactory.getLogger(LibraryDatabaseHelper.class); 
	
	@Inject
	public LibraryDatabaseHelper(Context context) {
		super(context, DB_NAME, null, VERSION);
	}
	
	private static String getCreateTableString() {
		String create = "create table lib_books (";
		
		boolean first = true;
		
		for (Field field : Field.values()) {
			if (first) {
				first = false;
			} else {
				create += ",";
			}
			
			create += " " + field.name() + " " + field.fieldDef;
		}
		
		create += ");";
		
		return create;
	}
	
	private synchronized SQLiteDatabase getDatabase() {
		if (this.database == null || !this.database.isOpen()) {
			this.database = this.getWritableDatabase();
		}
		
		return this.database;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(getCreateTableString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		if ( oldVersion == 3 ) {
//			db.execSQL("ALTER TABLE lib_books ADD COLUMN progress integer" );
//		}
	}

	public void delete(String fileName) {
		String[] args = {fileName};
		
		this.getDatabase().delete("lib_books", Field.file_name + " = ?", args);
	}
	
	public void close() {
		if (this.database != null) {
			this.database.close();
			this.database = null;
		}
	}
	
	public void updateLastRead(String fileName, int progress) {
		String whereClause = Field.file_name.toString() + " like ?";
		String[] args = {"%" + fileName};
		
		ContentValues content = new ContentValues();
		content.put(Field.date_last_read.toString(), new Date().getTime());
		
		if (progress != -1) {
			content.put(Field.progress.toString(), progress);
		}
		
		this.getDatabase().update("lib_books", content, whereClause, args);
	}
	
	public void storeNewBook(String fileName, String authorFirstName, String authorLastName, 
							 String title, String description, byte[] coverImage, boolean setLastRead) {
		ContentValues content = new ContentValues();
		
		content.put(Field.title.toString(), title);
		content.put(Field.file_name.toString(), authorFirstName);
		content.put(Field.last_name.toString(), authorLastName);
		content.put(Field.cover_image.toString(), coverImage);
		content.put(Field.description.toString(), description);
		
		if (setLastRead) {
			content.put(Field.date_last_read.toString(), new Date().getTime());
		}
		
		content.put(Field.file_name.toString(), fileName);
		content.put(Field.date_added.toString(), new Date().getTime());
		
		this.getDatabase().insert("lib_books", null, content);
	}
	
	public boolean hasBook(String fileName) {
		Field[] fields = {Field.file_name};
		String[] args = {"%" + fileName};
		
		String whereClause = Field.file_name.toString() + " like ?";
		
		Cursor foundBook = this.getDatabase().query("lib_books", fieldsAsString(fields), whereClause, args, null, null, null);
		
		boolean result = foundBook.getCount() > 0;
		foundBook.close();
		
		return result;
	}
	
	public KeyedQueryResult<LibraryBook> findByField(Field fieldName, String fieldValue, Field orderField, Order ordering) {
		String[] args = {fieldValue};
		String whereClause;
		
		if (fieldValue == null) {
			whereClause = fieldName.toString() + " is null";
			args = null;
		} else {
			whereClause = fieldName.toString() + " = ?";
		}
		
		Cursor cursor = this.getDatabase().query("lib_books", fieldsAsString(Field.values()), 
													whereClause, args, null, null, 
													"LOWER(" + orderField + ") " + ordering);
		List<String> keys = this.getKeys(orderField, ordering);
		
		return new KeyedBookResult(cursor, keys);
	}
	
	public QueryResult<LibraryBook> findAllOrderedBy(Field fieldName, Order order) {
		Cursor cursor = this.getDatabase().query("lib_books", fieldsAsString(Field.values()), 
											fieldName != null ? fieldName.toString() + " is not null" : null,
											new String[0], null, null,
											fieldName != null ? "LOWER(" + fieldName.toString() + ") " + order.toString() : null );
		return new LibraryBookResult(cursor);
	}
	
	private List<String> getKeys(Field field, Order order) {
		String[] keyField = {field.toString()};
		Cursor fieldCursor = this.getDatabase().query("lib_books", keyField, null, new String[0], null, null, 
													field != null ? "LOWER(" + field.toString() + ") " + order.toString() : null);
		List<String> keys = new ArrayList<String>();
		
		fieldCursor.moveToFirst();
		
		while (!fieldCursor.isAfterLast()) {
			keys.add(fieldCursor.getString(0));
			
			fieldCursor.moveToNext();
		}
		
		fieldCursor.close();
		
		return keys;
	}
	
	public KeyedQueryResult<LibraryBook> findAllKeyedBy(Field fieldName, Order order) {
		List<String> keys = getKeys(fieldName, order);
						
		Cursor cursor = getDatabase().query("lib_books", fieldsAsString(Field.values()), 
				fieldName != null ? fieldName.toString() + " is not null" : null,
			    new String[0], null, null,
				fieldName != null ? "LOWER(" + fieldName.toString() + ") " 
						+ order.toString() : null );		
		
		return new KeyedBookResult(cursor, keys);
	}	
	
	private static LibraryBook doConvertRow(Cursor cursor) {
		LibraryBook newBook = new LibraryBook();
		
		String firstName = cursor.isNull(Field.first_name.ordinal()) ? "" : cursor.getString(Field.first_name.ordinal());
		String lastName = cursor.isNull(Field.last_name.ordinal()) ? "" : cursor.getString(Field.last_name.ordinal());
		newBook.setAuthor(new Author(firstName, lastName));
		newBook.setTitle(cursor.getString(Field.title.ordinal()));
		newBook.setDescription(cursor.getString(Field.description.ordinal()));
		
		try {
			newBook.setAddedToLibrary(new Date(cursor.getLong(Field.date_added.ordinal())));
		} catch (RuntimeException e) { }
		
		try {
			newBook.setLastRead(new Date(cursor.getLong(Field.date_last_read.ordinal())));
		} catch (RuntimeException e) { }
		
		byte[] coverData = cursor.getBlob(Field.cover_image.ordinal());
		newBook.setCoverImage(coverData);
		
		newBook.setFileName(cursor.getString(Field.file_name.ordinal()));
		
		newBook.setProgress(cursor.getInt(Field.progress.ordinal()));
		
		return newBook;
	}
	
	private static String[] fieldsAsString(Field[] fields) {
		String[] result = new String[fields.length];
		
		for (int i = 0; i < fields.length; i++) {
			result[i] = fields[i].toString();
		}
		
		return result;
	}
	
	private class KeyedBookResult extends KeyedQueryResult<LibraryBook> {
		public KeyedBookResult(Cursor cursor, List<String> keys) {
			super(cursor, keys);
		}
		
		@Override
		public LibraryBook convertRow(Cursor cursor) {
			return doConvertRow(cursor);
		}
	}
	
	private class LibraryBookResult extends QueryResult<LibraryBook> {
		public LibraryBookResult(Cursor cursor) {
			super(cursor);
		}
		
		@Override
		public LibraryBook convertRow(Cursor cursor) {
			return doConvertRow(cursor);
		}
	}
}
