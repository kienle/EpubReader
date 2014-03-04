package com.vng.bookreader.library;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vng.bookreader.R;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

public class ImportTask extends AsyncTask<File, Integer, Void> implements OnCancelListener {
	private Context context;
	private LibraryService libraryService;
	private ImportCallback callback;
	
	private boolean copyToLibrary;
	
	private List<String> errors = new ArrayList<String>();
	
	private static final Logger LOG = LoggerFactory.getLogger(ImportTask.class);
	
	private static final int UPDATE_FOLDER = 1;
	private static final int UPDATE_IMPORT = 2;
	
	private int foldersScanned = 0;
	private int booksImported = 0;
	
	private String importFailed = null;

	public ImportTask(Context context, LibraryService libraryService, ImportCallback callback, boolean copyToLibrary) {
		this.context = context;
		this.libraryService = libraryService;
		this.callback = callback;
		this.copyToLibrary = copyToLibrary;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		LOG.debug("User aborted import.");
		
	}

	@Override
	protected Void doInBackground(File... params) {
		File folder = params[0];
		
		if (!folder.exists()) {
			importFailed = String.format(context.getString(R.string.no_such_folder), folder.getPath());
			
			return null;
		}
		
		List<File> bookFiles = new ArrayList<File>();
		this.findEpubsInFolder(folder, bookFiles);
		
		int total = bookFiles.size();
		
		for (int i = 0; i < total && !this.isCancelled(); i++) {
			File bookFile = bookFiles.get(i);
			
			LOG.info("Importing: " + bookFile.getAbsolutePath());
			
			try {
				this.importBook(bookFile);
			} catch (OutOfMemoryError e) {
				this.errors.add(bookFile.getName() + ": Out of memory.");
				return null;
			}
			
			this.publishProgress(UPDATE_IMPORT, i, total);
			this.booksImported++;
		}
		
		return null;
	}

	private void findEpubsInFolder(File folder, List<File> items) {
		if (folder == null) return;
			
		if (this.isCancelled()) return;
		
		
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			
			if (files != null) {
				for (File file : files) {
					this.findEpubsInFolder(file, items);
				}
			}
			
			this.foldersScanned++;
			this.publishProgress(UPDATE_FOLDER, foldersScanned);
		} else {
			if (folder.getName().endsWith(".epub")) {
				items.add(folder);
			}
		}
	}
	
	private void importBook(File file) {
		try {
			if (this.libraryService.hasBook(file.getName())) return;
			
			String fileName = file.getAbsolutePath();
			
			// read epub file
			EpubReader epubReader = new EpubReader();
			
			Book importedBook = epubReader.readEpubLazy(fileName, "UTF-8", Arrays.asList(MediatypeService.mediatypes));
			
			this.libraryService.storeBook(fileName, importedBook, false, this.copyToLibrary);
		} catch (IOException e) {
			this.errors.add(file + ": " + e.getMessage());
			LOG.error("Error while reading book: " + file, e);
		}
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		String message;
		
		if (values[0] == UPDATE_IMPORT) {
			message = String.format(this.context.getString(R.string.importing), values[1], values[2]);
		} else {
			message = String.format(this.context.getString(R.string.scan_folder), values[1]);
		}
		
		this.callback.importStatusUpdate(message);
	}
	
	@Override
	protected void onPostExecute(Void result) {
		if (this.importFailed != null) {
			this.callback.importFailed(importFailed);
		} else if (!this.isCancelled()) {
			this.callback.importComplete(this.booksImported, this.errors);
		}
	}
}
