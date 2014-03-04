package com.vng.bookreader.activity;

import java.io.File;
import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.nightwhistler.htmlspanner.HtmlSpanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Spinner;
import android.widget.ViewSwitcher;

import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockActivity;
import com.google.inject.Inject;
import com.vng.bookreader.Configuration;
import com.vng.bookreader.Configuration.LibraryView;
import com.vng.bookreader.R;
import com.vng.bookreader.Configuration.LibrarySelection;
import com.vng.bookreader.R.id;
import com.vng.bookreader.library.ImportCallback;
import com.vng.bookreader.library.ImportTask;
import com.vng.bookreader.library.KeyedQueryResult;
import com.vng.bookreader.library.KeyedResultAdapter;
import com.vng.bookreader.library.LibraryBook;
import com.vng.bookreader.library.LibraryService;
import com.vng.bookreader.library.QueryResult;
import com.vng.bookreader.quickaction.LibraryQuickAction;
import com.vng.bookreader.view.BookCaseView;
import com.vng.bookreader.view.FastBitmapDrawable;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class LibraryActivity extends RoboSherlockActivity implements ImportCallback, OnItemClickListener {
	
	@Inject
	private LibraryService libraryService;
	
	@InjectView(R.id.librarySpinner)
	private Spinner spinner;
	
	@InjectView(R.id.libraryList)
	private ListView libraryList;
	
	@InjectView(R.id.bookCaseView)
	private BookCaseView bookCaseView;
	
	@InjectView(R.id.alphabetList)
	private ListView alphabetList;
	
	private AlphabetAdapter alphabetAdapter;
	
	@InjectView(R.id.alphabetDivider)
	private ImageView alphabetDivider;
	
	@InjectView(R.id.libraryHolder)
	private ViewSwitcher switcher;
	
	@Inject
	private Configuration config;
	
	private Drawable backupCover;
	private Handler handler;
	
	private static final int[] ICONS = {
		R.drawable.book_binoculars,
		R.drawable.book_add,
		R.drawable.book_star,
		R.drawable.book,
		R.drawable.user
	};
	
	private KeyedResultAdapter bookAdapter;
	
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG);
	private static final int ALPHABET_THRESHOLD = 20;
	
	private ProgressDialog waitDialog;
	private ProgressDialog importDialog;
	
	private AlertDialog importQuestionDialog;
	
	private boolean askedUserToImport;
	private boolean oldKeepScreenOn;
	
	private static final Logger LOG = LoggerFactory.getLogger(LibraryActivity.class);
	
	private IntentCallback intentCallback;
	private List<CoverCallback> callbacks = new ArrayList<LibraryActivity.CoverCallback>();
	private Map<String, Drawable> coverCache = new HashMap<String, Drawable>();
	
	private LibraryQuickAction quickAction;
	
	private interface IntentCallback {
		void onResult(int resultCode, Intent data);
	}

	public LibraryActivity() {
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.library_menu);
		
		Bitmap backupBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.unknown_cover);
		this.backupCover = new FastBitmapDrawable(backupBitmap);
		
		this.handler = new Handler();
		
		if (savedInstanceState != null) {
			this.askedUserToImport = savedInstanceState.getBoolean("import_q", false);
		}
		
		this.bookCaseView.setOnScrollListener(new CoverScrollListener());
		this.libraryList.setOnScrollListener(new CoverScrollListener());
		
		if (this.config.getLibraryView() == LibraryView.BOOKCASE) {
			this.bookAdapter = new BookCaseAdapter(this);
			this.bookCaseView.setAdapter(this.bookAdapter);
			
			if (this.switcher.getDisplayedChild() == 0) {
				this.switcher.showNext();
			}
		} else {
			this.bookAdapter = new BookListAdapter(this);
			this.libraryList.setAdapter(this.bookAdapter);
		}
		
		ArrayAdapter<String> adapter = new QueryMenuAdapter(this, this.getResources().getStringArray(R.array.libraryQueries));
		this.spinner.setAdapter(adapter);
		this.spinner.setOnItemSelectedListener(new MenuSelectionListener());
		this.spinner.setSelection(this.config.getLastLibraryQuery().ordinal());
		
		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);
		
		this.importDialog = new ProgressDialog(this);
		this.importDialog.setOwnerActivity(this);
		this.importDialog.setTitle(R.string.importing_books);
		this.importDialog.setMessage(this.getString(R.string.scanning_epub));
		
		this.registerForContextMenu(this.libraryList);
		this.libraryList.setOnItemClickListener(this);
		
		this.setAlphabetListVisible(true);
		
		quickAction = new LibraryQuickAction(this);
	}
	
	private void buildImportQuestionDialog() {
		if (this.importQuestionDialog != null) return;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.no_books_found);
		builder.setMessage(this.getString(R.string.scan_bks_question));
		builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				showImportDialog();
			}
		});
		
		builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				importQuestionDialog = null;
			}
		});
		
		this.importQuestionDialog = builder.create();
	}
	
	private void setAlphabetListVisible(boolean visible) {
		int visibility = visible ? View.VISIBLE : View.GONE;
		
		this.alphabetList.setVisibility(visibility);
		this.alphabetDivider.setVisibility(visibility);
		this.libraryList.setFastScrollEnabled(visible);
	}
	
	private void loadCover(ImageView imageView, LibraryBook book, int index) {
		Drawable drawable = coverCache.get(book.getFileName());
		
		if (drawable != null) {
			imageView.setImageDrawable(drawable);
		} else {
			imageView.setImageDrawable(backupCover);
			
			if (book.getCoverImage() != null) {
				callbacks.add(new CoverCallback(book, index, imageView));
			}
		}
	}
	
	private void onBookClicked(LibraryBook book) {
//		this.showBookDetails(book);
		
		Intent intent = new Intent(this, ReadingActivity.class);
		
		intent.setData(Uri.parse(book.getFileName()));
//		setResult(RESULT_OK, intent);
				
		startActivityIfNeeded(intent, 99);	
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		this.onBookClicked(this.bookAdapter.getResultAt(position));
	}
	
	private Bitmap getCover( LibraryBook book ) {
		return BitmapFactory.decodeByteArray(book.getCoverImage(), 0, book.getCoverImage().length);
	}
	
	public void doDeleteBook(String fileName) {
		libraryService.deleteBook(fileName);
		new LoadBooksTask().execute(config.getLastLibraryQuery());
	}
	
	public void showBookDetails(final LibraryBook libraryBook) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.book_details);
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.book_details, null);
		
		builder.setView(layout);
		
		ImageView coverImageView = (ImageView)layout.findViewById(R.id.coverImageView);
		
		if (libraryBook.getCoverImage() != null) {
			coverImageView.setImageBitmap(this.getCover(libraryBook));
		} else {
			coverImageView.setImageDrawable(this.getResources().getDrawable(R.drawable.unknown_cover));
		}
		
		TextView titleText = (TextView) layout.findViewById(R.id.titleText);
		TextView authorText = (TextView) layout.findViewById(R.id.authorText);
		TextView lastReadText = (TextView) layout.findViewById(R.id.lastReadText);
		TextView addedToLibraryText = (TextView) layout.findViewById(R.id.addedToLibraryText);
		TextView bookDescriptionText = (TextView) layout.findViewById(R.id.bookDescriptionText);
		TextView fileNameText = (TextView) layout.findViewById(R.id.fileNameText);
		
		titleText.setText(libraryBook.getTitle());
		String authorStr = String.format( getString(R.string.book_by),
				 libraryBook.getAuthor().getFirstName() + " " 
				 + libraryBook.getAuthor().getLastName() );
		authorText.setText(authorStr);
		
		fileNameText.setText(libraryBook.getFileName());
		
		if (libraryBook.getLastRead() != null && !libraryBook.getLastRead().equals(new Date(0))) {
			String lastReadStr = String.format(getString(R.string.last_read),
													DATE_FORMAT.format(libraryBook.getLastRead()));
			lastReadText.setText(lastReadStr);
		} else {
			String lastReadStr = String.format(getString(R.string.last_read), getString(R.string.never_read));
			lastReadText.setText(lastReadStr);
		}
		
		String addedStr = String.format( getString(R.string.added_to_lib),
											DATE_FORMAT.format(libraryBook.getAddedToLibrary()));
		addedToLibraryText.setText(addedStr);
		
		bookDescriptionText.setText(new HtmlSpanner().fromHtml(libraryBook.getDescription()));
		
		builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				doDeleteBook(libraryBook.getFileName());
				
				dialog.dismiss();
			}
		});			
		
		builder.setNegativeButton(android.R.string.cancel, null);
		
		builder.setPositiveButton(R.string.read, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

				Intent intent = new Intent(LibraryActivity.this, ReadingActivity.class);
				
				intent.setData(Uri.parse(libraryBook.getFileName()));
				setResult(RESULT_OK, intent);
						
				startActivityIfNeeded(intent, 99);				
			}
		});
		
		builder.show();
	}
	
	private void showDownloadDialog() {
		final List<String> names = new ArrayList<String>(){{ 
			add("Feedbooks");
			add("Smashwords");
			add("Manybooks.net");
			add("Gutenberg.org");
			}};
	
		final List<String> addresses = new ArrayList<String>(){{
				add("http://www.feedbooks.com/site/free_books.atom");
				add("http://www.smashwords.com/nightwhistler");
				add("http://www.manybooks.net/opds/index.php");
				add("http://m.gutenberg.org/ebooks/?format=opds"); }};
		
		final List<String> users = new ArrayList<String>(){{
				add("");
				add("");
				add("");
				add(""); }};
				
		final List<String> passwords = new ArrayList<String>(){{
				add("");
				add("");
				add("");
				add(""); }};
				
		if (this.config.getCalibreServer().length() != 0) {
			names.add("Calibre server");
			addresses.add(this.config.getCalibreServer());
			
			if (this.config.getCalibreUser().length() != 0 ) {
				users.add(this.config.getCalibreUser());
			} else {
				users.add("");
			}
			
			if (this.config.getCalibrePassword().length() != 0 ) {
				passwords.add(this.config.getCalibrePassword());
			} else {
				passwords.add("");
			}
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.download);
		builder.setItems(names.toArray(new String[names.size()]), 
			new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//Intent intent = new Intent(LibraryActivity.this, Cata)
				}
			});
	}
	
	private void startImport(File startFolder, boolean copy) {
		ImportTask importTask = new ImportTask(this, libraryService, this, copy);
		
		importDialog.setOnCancelListener(importTask);
		importDialog.show();
		
		this.oldKeepScreenOn = this.libraryList.getKeepScreenOn();
		this.libraryList.setKeepScreenOn(true);
		
		importTask.execute(startFolder);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (this.intentCallback != null) {
			this.intentCallback.onResult(resultCode, data);
		}
		
		if (requestCode == 1000 && resultCode == RESULT_OK) {
			this.conditionallyRefresh();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		com.actionbarsherlock.view.MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.library_menu, menu);
		
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		boolean bookCaseActive = this.switcher.getDisplayedChild() != 0;
		
		if (bookCaseActive) {
			menu.findItem(R.id.submenu_shelves_view).setChecked(true);
			menu.findItem(R.id.menu_library_view_mode).setIcon(R.drawable.ic_action_grid_light);
		} else {
			menu.findItem(R.id.submenu_list_view).setChecked(!bookCaseActive);
			menu.findItem(R.id.menu_library_view_mode).setIcon(R.drawable.ic_action_list_light);
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		
		switch (item.getItemId()) {
			case R.id.submenu_shelves_view:
				this.changeViewMode(LibraryView.BOOKCASE);
				item.setChecked(true);
				this.invalidateOptionsMenu();
				return true;
			case R.id.submenu_list_view:
				this.changeViewMode(LibraryView.LIST);
				item.setChecked(true);
				this.invalidateOptionsMenu();
				return true;
			case R.id.menu_scan_books:
				this.showImportDialog();
				return true;
			case R.id.menu_preferences:
				Intent intent = new Intent(this, BookReaderPrefsActivity.class);
				this.startActivityForResult(intent, 1000);
				return true;
			case R.id.menu_about:
				Dialogs.showAboutDialog(this);
				return true;
			case R.id.menu_download:
				this.showDownloadDialog();
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void changeViewMode(LibraryView mode) {
		if ( (switcher.getDisplayedChild() == 0 && mode == LibraryView.LIST) || 
			 (switcher.getDisplayedChild() != 0 && mode == LibraryView.BOOKCASE) ) return;
		
		if (switcher.getDisplayedChild() == 0) {
			bookAdapter = new BookCaseAdapter(LibraryActivity.this);
			bookCaseView.setAdapter(bookAdapter);
			config.setLibraryView(LibraryView.BOOKCASE);
		} else {
			bookAdapter = new BookListAdapter(LibraryActivity.this);
			libraryList.setAdapter(bookAdapter);
			config.setLibraryView(LibraryView.LIST);
		}
		
		switcher.showNext();
		
		new LoadBooksTask().execute(config.getLastLibraryQuery());
	}
	
	private void showImportDialog() {
		AlertDialog.Builder builder;
		
		LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.import_dialog, null);
		final RadioButton scanFolderRadio = (RadioButton)layout.findViewById(R.id.scanFolderRadio);
		final TextView folderToScanText = (TextView)layout.findViewById(R.id.folderToScanText);
		final CheckBox copyToLibCheckBox = (CheckBox)layout.findViewById(R.id.copyToLibCheckBox);
		final Button browserButton = (Button)layout.findViewById(R.id.browseButton);
		
		folderToScanText.setText(this.config.getStorageBase() + "/eBooks");
		
		folderToScanText.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				scanFolderRadio.setChecked(true);
			}
		});
		
		//Copy default setting from the prefs
		copyToLibCheckBox.setChecked(this.config.isCopyToLibrayEnabled());
		
		builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		
		this.intentCallback = new IntentCallback() {
			
			@Override
			public void onResult(int resultCode, Intent data) {
				if (resultCode == RESULT_OK && data != null) {
					folderToScanText.setText(data.getData().getPath());
				}
			}
		};
		
		browserButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				scanFolderRadio.setChecked(true);
				Intent intent = new Intent(LibraryActivity.this, FileBrowseActivity.class);
				intent.setData(Uri.parse(folderToScanText.getText().toString()));
				startActivityForResult(intent, 0);
			}
		});
		
		builder.setTitle(R.string.import_books);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
				if (scanFolderRadio.isChecked()) {
					startImport(new File(folderToScanText.getText().toString()), copyToLibCheckBox.isChecked());
				} else {
					startImport(new File(config.getStorageBase()), copyToLibCheckBox.isChecked());
				}
			}
		});
		builder.setNegativeButton(android.R.string.cancel, null);
		
		builder.show();
	}
	
	private void conditionallyRefresh() {
		this.bookAdapter.clear();
		
		LibrarySelection lastSelection = this.config.getLastLibraryQuery();
		
		if (this.spinner.getSelectedItemPosition() != lastSelection.ordinal()) {
			this.spinner.setSelection(lastSelection.ordinal());
		} else {
			new LoadBooksTask().execute(lastSelection);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("import_q", this.askedUserToImport);
	}
	
	@Override
	protected void onStop() {
		this.libraryService.close();
		this.waitDialog.dismiss();
		this.importDialog.dismiss();
		
		super.onStop();
	}
	
	@Override
	public void onBackPressed() {
		this.moveTaskToBack(true);
		
		this.finish();
	}
	
	@Override
	public void importCancelled() {
		this.libraryList.setKeepScreenOn(this.oldKeepScreenOn);
		
		//Switch to the "recently added" view.
		if (this.spinner.getSelectedItemPosition() == LibrarySelection.LAST_ADDED.ordinal()) {
			new LoadBooksTask().execute(LibrarySelection.LAST_ADDED);
		} else {
			this.spinner.setSelection(LibrarySelection.LAST_ADDED.ordinal());
		}
	}
	
	@Override
	public void importComplete(int booksImported, List<String> errors) {
		this.importDialog.hide();
		
		//If the user cancelled the import, don't bug him/her with alerts.
		if (!errors.isEmpty()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.import_errors);
			builder.setItems(errors.toArray(new String[errors.size()]), null);
			
			builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			
			builder.show();
		}
		
		this.libraryList.setKeepScreenOn(this.oldKeepScreenOn);
		
		if (booksImported > 0) {
			//Switch to the "recently added" view.
			if (this.spinner.getSelectedItemPosition() == LibrarySelection.LAST_ADDED.ordinal()) {
				new LoadBooksTask().execute(LibrarySelection.LAST_ADDED);
			} else {
				this.spinner.setSelection(LibrarySelection.LAST_ADDED.ordinal());
			}
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.no_books_found);
			builder.setMessage(this.getString(R.string.no_bks_fnd_text));
			builder.setNeutralButton(android.R.string.ok, null);
			
			builder.show();
		}
	}
	
	@Override
	public void importFailed(String reason) {
		this.importDialog.hide();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.import_failed);
		builder.setMessage(reason);
		builder.setNeutralButton(android.R.string.ok, null);
		builder.show();
	}
	
	@Override
	public void importStatusUpdate(String update) {
		this.importDialog.setMessage(update);
	}
	
	public void onAlphabetListClick(KeyedQueryResult<LibraryBook> result, Character c) {
		int index = result.getOffsetFor(Character.toUpperCase(c));
		
		if (index == -1) return;
		
		if (this.alphabetAdapter != null) {
			this.alphabetAdapter.setHighlightChar(c);
		}
		
		if (this.config.getLibraryView() == LibraryView.BOOKCASE) {
			this.bookCaseView.setSelection(index);
		} else {
			this.libraryList.setSelection(index);
		}
	}
	
	///////////////////////////////INNER CLASSES////////////////////////////////////////////////////////////
	private class BookListAdapter extends KeyedResultAdapter {
		private Context context;
		
		public BookListAdapter(Context context) {
			this.context = context;
		}
		
		@Override
		public View getView(final int index, final LibraryBook book, final View convertView,
				ViewGroup parent) {
			
			View rowView;
			
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.book_row, parent, false);
			} else {
				rowView = convertView;
			}
			
			TextView titleText = (TextView)rowView.findViewById(R.id.bookTitle);
			TextView authorText = (TextView)rowView.findViewById(R.id.bookAuthor);
			TextView dateText = (TextView)rowView.findViewById(R.id.addedToLibrary);
			TextView progressText = (TextView)rowView.findViewById(R.id.readingProgress);
			
			final ImageView imageView = (ImageView)rowView.findViewById(R.id.bookCover);
			
			String author = String.format(getString(R.string.book_by), 
														book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName());
			authorText.setText(author);
			titleText.setText(book.getTitle());
			
			if (book.getProgress() > 0) {
				progressText.setText("" + book.getProgress() + "%");
			} else {
				progressText.setText("");
			}
			
			String dateStr = String.format(getString(R.string.added_to_lib), DATE_FORMAT.format(book.getAddedToLibrary()));
			dateText.setText(dateStr);
			
			loadCover(imageView, book, index);
			
			return rowView;
		}
	}
	
	private class CoverScrollListener implements AbsListView.OnScrollListener {
		private Runnable lastRunnable;
		private Character lastCharacter;
		
		@Override
		public void onScroll(AbsListView view, final int firstVisibleItem,
				final int visibleItemCount, final int totalItemCount) {
			if (visibleItemCount == 0) return;
			
			if (this.lastRunnable != null) handler.removeCallbacks(lastRunnable);
			
			this.lastRunnable = new Runnable() {
				
				@Override
				public void run() {
					if (bookAdapter.isKeyed()) {
						String key = bookAdapter.getKey(firstVisibleItem);
						Character keyChar = null;
						
						if (key != null && key.length() > 0) {
							keyChar = Character.toUpperCase(key.charAt(0));
						}
						
						if (keyChar != null && !keyChar.equals(lastCharacter)) {
							lastCharacter = keyChar;
							List<Character> alphabet = bookAdapter.getAlphabet();
							
							//If the highlight-char is already set, this means the 
							//user clicked the bar, so don't scroll it.
							if (alphabetAdapter != null && !keyChar.equals(alphabetAdapter.getHighlightChar())) {
								alphabetAdapter.setHighlightChar(keyChar);
								alphabetList.setSelection(alphabet.indexOf(keyChar));
							}
							
							for (int i = 0; i < alphabetList.getChildCount(); i++) {
								View child = alphabetList.getChildAt(i);
								if (child.getTag().equals(keyChar)) {
									child.setBackgroundColor(Color.BLUE);
								} else {
									child.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
								}
							}
						}
					}
					
					List<CoverCallback> localList = new ArrayList<LibraryActivity.CoverCallback>(callbacks);
					
					callbacks.clear();
					
					int lastVisibleItem = firstVisibleItem + visibleItemCount - 1;
					
					LOG.debug("Loading items " + firstVisibleItem + " to " + lastVisibleItem + " of " + totalItemCount );
					
					for (CoverCallback callback : localList) {
						if (firstVisibleItem <= callback.viewIndex && callback.viewIndex <= lastVisibleItem) {
							callback.run();
						}
					}
				}
			};
			
			handler.postDelayed(lastRunnable, 550);
		}
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			
		}
	}
	
	private class CoverCallback {
		protected LibraryBook book;
		protected int viewIndex;
		protected ImageView view;
		
		public CoverCallback(LibraryBook book, int viewIndex, ImageView view) {
			this.book = book;
			this.view = view;
			this.viewIndex = viewIndex;
		}
		
		public void run() {
			try {
				FastBitmapDrawable drawable = new FastBitmapDrawable(getCover(book));
				view.setImageDrawable(drawable);
				coverCache.put(book.getFileName(), drawable);
			} catch (OutOfMemoryError e) {
				coverCache.clear();
			}
		}
	}
	
	private class BookCaseAdapter extends KeyedResultAdapter {
		private Context context;
		
		public BookCaseAdapter(Context context) {
			this.context = context;
		}
		
		@Override
		public View getView(final int index, final LibraryBook object, View convertView,
				ViewGroup parent) {
			
			View rowView;
			
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.bookcase_row, parent, false);
			} else {
				rowView = convertView;
			}
			
			rowView.setTag(index);
			
			rowView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					onBookClicked(object);
				}
			});
			
			rowView.setLongClickable(true);
			rowView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					quickAction.setSelectedBook(object);
					quickAction.show(v);
					return true;
				}
			});
			
			final ImageView imageView = (ImageView)rowView.findViewById(R.id.bookCover);
			imageView.setImageDrawable(backupCover);
			
			final TextView textView = (TextView)rowView.findViewById(R.id.bookLabel);
			textView.setText(object.getTitle());
			textView.setBackgroundResource(R.drawable.alphabet_bar_bg_dark);
			
			loadCover(imageView, object, index);
			
			return rowView;
		}
	}
	
	private class QueryMenuAdapter extends ArrayAdapter<String> {
		String[] strings;
		
		public QueryMenuAdapter(Context context, String[] strings) {
			super(context, android.R.layout.simple_spinner_item, strings);
			this.strings = strings;
		}
		
		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			
			View rowView;
			
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.menu_row, parent, false);
			} else {
				rowView = convertView;
			}
			
			TextView textView = (TextView)rowView.findViewById(R.id.menuText);
			textView.setText(this.strings[position]);
			textView.setTextColor(Color.WHITE);
			
			ImageView imageView = (ImageView)rowView.findViewById(R.id.icon);
			
			imageView.setImageResource(ICONS[position]);
			
			return rowView;
		}
	}

	private class MenuSelectionListener implements OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
				long arg3) {
			LibrarySelection newSelection = LibrarySelection.values()[position];
			
			config.setLastLibraryQuery(newSelection);
			
			bookAdapter.clear();
			new LoadBooksTask().execute(newSelection);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			
		}
	}
	
	private class AlphabetAdapter extends ArrayAdapter<Character> {
		private List<Character> data;
		
		private Character highlightChar;
		
		public AlphabetAdapter(Context context, int layout, int view, List<Character> input) {
			super(context, layout, view, input);
			
			this.data = input;
		}
		
		@Override
		public View getView(int position, View convertView, android.view.ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			
			Character tag = data.get(position);
			view.setTag(tag);
			
			if (tag.equals(highlightChar)) {
				view.setBackgroundColor(Color.BLUE);
			} else {
				view.setBackgroundColor(getResources().getColor(android.R.color.background_dark));
			}
			
			return view;
		}
		
		public void setHighlightChar(Character highlightChar) {
			this.highlightChar = highlightChar;
		}
		
		public Character getHighlightChar() {
			return this.highlightChar;
		}
	}
	
	
	
	private class LoadBooksTask extends AsyncTask<Configuration.LibrarySelection, Integer, QueryResult<LibraryBook>> {
		private Configuration.LibrarySelection librarySelection;
		
		@Override
		protected void onPreExecute() {
			waitDialog.setTitle(R.string.loading_library);
			waitDialog.show();
			
			coverCache.clear();
		}
		
		@Override
		protected QueryResult<LibraryBook> doInBackground(Configuration.LibrarySelection... params) {
			Exception storedException = null;
			
			for (int i = 0; i < 3; i++) {
				try {
					this.librarySelection = params[0];
					
					switch (librarySelection) {
						case LAST_ADDED:
							return libraryService.findAllByLastAdded();
						case UNREAD:
							return libraryService.findUnread();
						case BY_TITLE:
							return libraryService.findAllByTitle();
						case BY_AUTHOR:
							return libraryService.findAllByAuthor();
						default:
							return libraryService.findAllByLastRead();
					}
				} catch (SQLException e) {
					storedException = e;
					try {
						//Sometimes the database is still locked.
						Thread.sleep(1000);
					} catch (InterruptedException ex) { }
				}
			}
			
			throw new RuntimeException("Failed after 3 attempts", storedException);
		}
		
		@Override
		protected void onPostExecute(QueryResult<LibraryBook> result) {
			bookAdapter.setResult(result);
			
			if (result instanceof KeyedQueryResult && result.getSize() >= ALPHABET_THRESHOLD) {
				final KeyedQueryResult<LibraryBook> keyedResult =(KeyedQueryResult<LibraryBook>)result;
				
				alphabetAdapter = new AlphabetAdapter(LibraryActivity.this, R.layout.alphabet_line, R.id.alphabetLabel, keyedResult.getAlphabet());
				
				alphabetList.setAdapter(alphabetAdapter);
				
				alphabetList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						Character c = keyedResult.getAlphabet().get(arg2);
						onAlphabetListClick(keyedResult, c);
					}
				});
				
				setAlphabetListVisible(true);
			} else {
				alphabetAdapter = null;
				setAlphabetListVisible(false);
			}
			
			waitDialog.hide();
			
			if (librarySelection == Configuration.LibrarySelection.LAST_ADDED && result.getSize() == 0 && !askedUserToImport) {
				askedUserToImport = true;
				buildImportQuestionDialog();
				importQuestionDialog.show();
			}
		}
	}
	
	
}
