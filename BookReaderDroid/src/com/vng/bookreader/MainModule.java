package com.vng.bookreader;

import com.google.inject.AbstractModule;
import com.vng.bookreader.library.LibraryService;
import com.vng.bookreader.library.SQLiteLibraryService;


public class MainModule extends AbstractModule {
	@Override
	protected void configure() {
		this.bind(LibraryService.class).to(SQLiteLibraryService.class);
	}
}
