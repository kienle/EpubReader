package com.vng.bookreader.library;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.vng.bookreader.Configuration;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;

@Singleton
public class SQLiteLibraryService implements LibraryService {
	private static final int THUMBNAIL_HEIGHT = 250;
	
	private static final long MAX_COVER_SIZE = 1024 * 1024; // 1MB
	
	private static final int LIMIT = 20;
	
	@Inject
	private LibraryDatabaseHelper helper;
	
	private static final Logger LOG = LoggerFactory.getLogger(SQLiteLibraryService.class);
	
	@Inject
	private Configuration config;
	
	public void setHelper(LibraryDatabaseHelper helper) {
		this.helper = helper;
	}
	
	@Override
	public void updateReadingProgress(String fileName, int progress) {
		this.helper.updateLastRead(new File(fileName).getName(), progress);
	}

	@Override
	public void storeBook(String fileName, Book book, boolean updateLastRead, boolean copyFile) throws IOException {
		File bookFile = new File(fileName);
		
		if (this.hasBook(bookFile.getName())) {
			if (!updateLastRead) {
				return;
			} else {
				this.helper.updateLastRead(bookFile.getName(), -1);
				return;
			}
		}
		
		Metadata metaData = book.getMetadata();
		
		String authorFirstName = "Unknown author";
		String authorLastName = "";
		
		if (metaData.getAuthors().size() > 0) {
			authorFirstName = metaData.getAuthors().get(0).getFirstname();
			authorLastName = metaData.getAuthors().get(0).getLastname();
		}
		
		byte[] thumbnail = null;
		
		try {
			if (book.getCoverImage() != null && book.getCoverImage().getSize() < MAX_COVER_SIZE) {
				thumbnail = resizeImage(book.getCoverImage().getData());
				book.getCoverImage().close();
			}
		} catch (IOException e) {
			
		} catch (OutOfMemoryError e) {
			
		}
		
		String description = "";
		
		if (!metaData.getDescriptions().isEmpty()) {
			description = metaData.getDescriptions().get(0);
		}
		
		String title = book.getTitle();
		
		if (title.trim().length() == 0) {
			title = fileName.substring(fileName.lastIndexOf('/') + 1);
		}
		
		if (copyFile) {
			bookFile = copyToLibrary(fileName, authorLastName + ", " + authorFirstName, title);
		}
		
		this.helper.storeNewBook(bookFile.getAbsolutePath(), authorFirstName, authorLastName, title, description, thumbnail, updateLastRead);
	}

	private String cleanUp(String input) {
		char[] illegalChars = {
				':', '/', '\\', '?', '<', '>', '\"', '*', '&'
		};
		
		String output = input;
		for (char c: illegalChars) {
			output = output.replace(c, '_');
		}
		
		return output.trim();
	}
	
	private File copyToLibrary(String fileName, String author, String title) throws IOException {
		File baseFile = new File(fileName);
		
		File targetFolder = new File(config.getLibraryFolder() + "/" + cleanUp(author) + "/" + cleanUp(title));
		
		targetFolder.mkdirs();
		
		FileChannel source = null;
		FileChannel destination = null;
		
		File targetFile = new File(targetFolder, baseFile.getName());
		
		if (baseFile.equals(targetFile)) {
			return baseFile;
		}
		
		LOG.debug("Copying to file: " + targetFile.getAbsolutePath() );
		
		targetFile.createNewFile();
		
		try {
			source = new FileInputStream(baseFile).getChannel();
			destination = new FileOutputStream(targetFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) source.close();
			
			if (destination != null) destination.close();
		}
		
		return targetFile;
	}
	
	@Override
	public QueryResult<LibraryBook> findUnread() {
		return this.helper.findByField(LibraryDatabaseHelper.Field.date_last_read, 
										null, LibraryDatabaseHelper.Field.title, LibraryDatabaseHelper.Order.ASC);
	}
	
	@Override
	public LibraryBook getBook(String fileName) {
		QueryResult<LibraryBook> booksByFile = this.helper.findByField(LibraryDatabaseHelper.Field.file_name, 
																		fileName, null, LibraryDatabaseHelper.Order.ASC);
		
		switch (booksByFile.getSize()) {
			case 0:
				return null;
			case 1:
				booksByFile.getItemAt(0);
			default:
				throw new IllegalStateException("Non unique file-name: " + fileName);
		}
	}

	@Override
	public QueryResult<LibraryBook> findAllByLastRead() {
		QueryResult<LibraryBook> result = this.helper.findAllOrderedBy(LibraryDatabaseHelper.Field.date_last_read, 
																		LibraryDatabaseHelper.Order.DESC);
		
		result.setLimit(LIMIT);
		
		return result;
	}
	
	@Override
	public QueryResult<LibraryBook> findAllByAuthor() {
		return this.helper.findAllKeyedBy(LibraryDatabaseHelper.Field.last_name, LibraryDatabaseHelper.Order.ASC);
	}

	@Override
	public QueryResult<LibraryBook> findAllByLastAdded() {
		QueryResult<LibraryBook> result = this.helper.findAllOrderedBy(LibraryDatabaseHelper.Field.date_added, 
																		LibraryDatabaseHelper.Order.DESC);
		
		result.setLimit(LIMIT);
		
		return result;
	}

	@Override
	public QueryResult<LibraryBook> findAllByTitle() {
		return this.helper.findAllKeyedBy(LibraryDatabaseHelper.Field.title, LibraryDatabaseHelper.Order.ASC);
	}

	@Override
	public void close() {
		this.helper.close();
	}

	@Override
	public void deleteBook(String fileName) {
		this.helper.delete(fileName);
		
		if (fileName.startsWith(config.getLibraryFolder())) {
			File bookFile = new File(fileName);
			File parentFolder = bookFile.getParentFile();
			
			bookFile.delete();
			
			while (parentFolder.list() == null || parentFolder.list().length == 0) {
				parentFolder.delete();
				parentFolder = parentFolder.getParentFile();
			}
		}
	}
	
	@Override
	public boolean hasBook(String fileName) {
		return this.helper.hasBook(fileName);
	}

	private byte[] resizeImage(byte[] image) {
		if (image == null) return null;
		
		Bitmap bitmapOrg = BitmapFactory.decodeByteArray(image, 0, image.length);
		
		if (bitmapOrg == null) return null;
		
		int width = bitmapOrg.getWidth();
		int height = bitmapOrg.getHeight();
		
		int newHeight = THUMBNAIL_HEIGHT;
		float scale = ((float)newHeight) / height;
		
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		
		Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
		
		bitmapOrg.recycle();
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		resizedBitmap.compress(CompressFormat.PNG, 0, bos);
		
		resizedBitmap.recycle();
		
		return bos.toByteArray();
	}
}
