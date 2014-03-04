package com.vng.bookreader.activity;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockActivity;
import com.google.inject.Inject;
import com.vng.bookreader.Configuration;
import com.vng.bookreader.Configuration.AnimationStyle;
import com.vng.bookreader.Configuration.ColourProfile;
import com.vng.bookreader.R;
import com.vng.bookreader.animation.Animations;
import com.vng.bookreader.animation.Animator;
import com.vng.bookreader.animation.PageCurlAnimator;
import com.vng.bookreader.library.LibraryService;
import com.vng.bookreader.selection.SelectionPointerImageView;
import com.vng.bookreader.selection.SelectionWord;
import com.vng.bookreader.selection.TextSelection;
import com.vng.bookreader.view.AnimatedImageView;
import com.vng.bookreader.view.BookView;
import com.vng.bookreader.view.BookViewListener;
import com.vng.bookreader.view.NavGestureDetector;

import fi.harism.curl.CurlPage;
import fi.harism.curl.CurlView;
import fi.harism.curl.CurlView.PageProvider;
import fi.harism.curl.CurlView.SizeChangedObserver;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.SpannedString;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import roboguice.RoboGuice;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class ReadingActivity extends RoboSherlockActivity implements BookViewListener {
	//private static final String TEST_EPUB_FILE_NAME = "/mnt/sdcard/HaiND/piedpiper.epub";
//	private static final String TEST_EPUB_FILE_NAME = "/mnt/sdcard/HaiND/Harry.Potter_1_Harry.Potter&Hon.da.phu.thuy_J.K.Rowling.epub";
	private static final String TEST_EPUB_FILE_NAME = Environment.getExternalStorageDirectory().getAbsolutePath() + "/KVH_01.epub";
	
	private static final String POS_KEY = "offset:";
	private static final String IDX_KEY = "index:";
	
	public static final String PICK_RESULT_ACTION = "colordict.intent.action.PICK_RESULT";	
	public static final String SEARCH_ACTION = "colordict.intent.action.SEARCH";
	public static final String EXTRA_QUERY = "EXTRA_QUERY";
	public static final String EXTRA_FULLSCREEN = "EXTRA_FULLSCREEN";
	public static final String EXTRA_HEIGHT = "EXTRA_HEIGHT";
	public static final String EXTRA_WIDTH = "EXTRA_WIDTH";
	public static final String EXTRA_GRAVITY = "EXTRA_GRAVITY";
	public static final String EXTRA_MARGIN_LEFT = "EXTRA_MARGIN_LEFT";
	public static final String EXTRA_MARGIN_TOP = "EXTRA_MARGIN_TOP";
	public static final String EXTRA_MARGIN_BOTTOM = "EXTRA_MARGIN_BOTTOM";
	public static final String EXTRA_MARGIN_RIGHT = "EXTRA_MARGIN_RIGHT";
	
	private static final Logger LOG = LoggerFactory.getLogger(ReadingActivity.class);
	
	@Inject
	private LibraryService libraryService;
	
	@Inject
	private Configuration config;
	
//	@InjectView(R.id.curlView)
//	private CurlView curlView;
	
	@InjectView(R.id.mainContainer)
	private ViewSwitcher viewSwitcher;
	
	@InjectView(R.id.bookView)
	private BookView bookView;
	
	@InjectView(R.id.titleBarText)
	private TextView titleBarText;
	
	@InjectView(R.id.titleBarLayout)
	private RelativeLayout titleBarLayout;
	
	@InjectView(R.id.titleProgress)
	private SeekBar progressBar;
	
	@InjectView(R.id.percentageText)
	private TextView percentageText;
	
	@InjectView(R.id.authorText)
	private TextView authorText;
	
	@InjectView(R.id.dummyView)
	private AnimatedImageView dummyView;
	
	@InjectView(R.id.startTextSelectView)
	private SelectionPointerImageView startTextSelectView;
	
	@InjectView(R.id.endTextSelectView)
	private SelectionPointerImageView endTextSelectView;
	
	private TextSelection textSelection;
	private boolean hasJustSelectedWord = false;
	
	private ProgressDialog waitDialog;
	private AlertDialog tocDialog;
	
	private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;
	
	private String bookTitle;
	private String titleBase;
	
	private String fileName;
	private int progressPercentage;
	
	private boolean oldBrightness = false;
	private boolean oldStripWhiteSpace = false;
	private String oldFontName = "";
	
	private enum Orientation {
		HORIZONTAL,
		VERTICAL
	}

	private CharSequence selectedWord = null;
	
	private Handler uiHandler;
	private Handler backgroundHandler;
	
	private Toast brightnessToast;
	
	public ReadingActivity() {
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		LOG.info("ReadingActivity!!!!!!!!!!!!!!!!");
		
		super.onCreate(savedInstanceState);
		
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.read_book);
		
		
		this.uiHandler = new Handler();
		
		HandlerThread bgThread = new HandlerThread("backgournd");
		bgThread.start();
		this.backgroundHandler = new Handler(bgThread.getLooper());
		
		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);
		this.waitDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				//This just consumes all key events and does nothing.
				return true;
			}
		});
		
		DisplayMetrics metrics = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		this.gestureDetector = new GestureDetector(new NavGestureDetector(this.bookView, this, metrics));
		
		this.gestureListener = new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (hasJustSelectedWord) {
					hasJustSelectedWord = false;
					
					return true;
				} else if (textSelection != null && textSelection.isShowing()) {
					textSelection.hide();
					
					return true;
				}
				
				return gestureDetector.onTouchEvent(event);
			}
		};
		
		this.progressBar.setFocusable(true);
		this.progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			private int seekValue;
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				bookView.navigateToPercentage(this.seekValue);
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					this.seekValue = progress;
					percentageText.setText(progress + "% ");
				}
			}
		});
		
		this.viewSwitcher.setOnTouchListener(this.gestureListener);
		this.bookView.setOnTouchListener(this.gestureListener);
		
		this.bookView.addListener(this);
		this.bookView.setSpanner(RoboGuice.getInjector(this).getInstance(HtmlSpanner.class));
		
		
		this.oldBrightness = config.isBrightnessControlEnabled();
    	this.oldStripWhiteSpace = config.isStripWhiteSpaceEnabled();
    	this.oldFontName = config.getFontFamily().getName();
    	
    	this.registerForContextMenu(this.bookView);
    	
//    	String file = this.getIntent().getStringExtra("file_name");
    	String file = TEST_EPUB_FILE_NAME;
    	
//    	if (file == null && this.getIntent().getData() != null) {
//    		file = this.getIntent().getData().getPath();
//    	}
//    	
//    	if (file == null) {
//    		file = config.getLastOpenedFile();
//    	}
    	
    	this.updateFromPrefs();
    	this.updateFileName(savedInstanceState, file);
    	
    	if (this.fileName.equals("")) {
    		Intent intent = new Intent(this, LibraryActivity.class);
    		this.startActivity(intent);
    		this.finish();
    		return;
    	} else {
    		if (savedInstanceState == null &&  config.isSyncEnabled()) {
        		//new DownloadProgressTask().execute();
        	} else {        	
        		this.bookView.restore();
        	}
    	}
    	
    	this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    	this.getSupportActionBar().hide();
    	
//    	this.curlView.setPageProvider(new CurlPageProvider());
//    	this.curlView.setSizeChangedObserver(new CurlPageSizeChangedObserver());
//    	this.curlView.setOnAnimationListener(new CurlView.OnAnimationListener() {
//			
//			@Override
//			public void onAnimationStart() {
////				LOG.info("$$$$$$$$$$$$$$$$$$$$$$$$ onAnimationStart");
//				bookView.pageDown();
//			}
//			
//			@Override
//			public void onAnimationComplete() {
////				LOG.info("$$$$$$$$$$$$$$$$$$$$$$$$ onAnimationComplete");
//			}
//		});
//    	
//    	this.curlView.setCurrentIndex(0);
    	
    	this.textSelection = new TextSelection(this.bookView, this.startTextSelectView, this.endTextSelectView);
    	this.textSelection.hide();
	}
	
	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		this.getSupportMenuInflater().inflate(R.menu.reading_menu, menu);
		
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		if (this.tocDialog == null) {
			this.initTocDialog();
		}
		
		com.actionbarsherlock.view.MenuItem nightModeMenu = menu.findItem(R.id.menu_night_mode);
		com.actionbarsherlock.view.MenuItem dayModeMenu = menu.findItem(R.id.menu_day_mode);
		
		com.actionbarsherlock.view.MenuItem tocMenu = menu.findItem(R.id.menu_toc);
		
//		tocMenu.setEnabled(this.tocDialog != null);
		
		if (this.config.getColourProfile() == ColourProfile.DAY) {
			dayModeMenu.setVisible(false);
			nightModeMenu.setVisible(true);
		} else {
			dayModeMenu.setVisible(true);
			nightModeMenu.setVisible(false);
		}
		
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		this.updateFromPrefs();
	}
	
	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		this.hideTitleBar();
		
		switch (item.getItemId()) {
//			case R.id.menu_open_library:
//				this.launchLibrary();
//				break;
			case R.id.menu_night_mode:
				this.config.setColourProfile(ColourProfile.NIGHT);
				this.restoreColorProfile();
				this.invalidateOptionsMenu();
				break;
			case R.id.menu_day_mode:
				this.config.setColourProfile(ColourProfile.DAY);
				this.restoreColorProfile();
				this.invalidateOptionsMenu();
				break;
			case R.id.menu_toc:
				if (this.tocDialog == null) this.initTocDialog();
				
				this.tocDialog.show();
				break;
			case R.id.menu_preferences:
				//Cache old settings to check if we'll need a restart later
				this.oldBrightness = this.config.isBrightnessControlEnabled();
				this.oldStripWhiteSpace = this.config.isStripWhiteSpaceEnabled();
				
				Intent intent = new Intent(this, BookReaderPrefsActivity.class);
				this.startActivity(intent);
				
				return true;
			case R.id.menu_about:
				Dialogs.showAboutDialog(this);
				break;
			case android.R.id.home:
				this.finish();
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (this.bookView != null) {
			outState.putInt(POS_KEY, this.bookView.getPosition());
			outState.putInt(IDX_KEY, this.bookView.getIndex());
			
			this.libraryService.updateReadingProgress(this.fileName, this.progressPercentage);
			this.libraryService.close();
			
//			this.backgroundHandler.post(new Runnable() {
//				@Override
//				public void run() {
//					try {
//							
//					} catch (AccessException)
//				}
//			});
		}
	}
	
	private void showTitleBar() {
		this.titleBarLayout.setVisibility(View.VISIBLE);
	}
	
	private void hideTitleBar() {
		this.titleBarLayout.setVisibility(View.GONE);
	}
	
	private void launchLibrary() {
		Intent intent = new Intent(this, LibraryActivity.class);
		this.startActivity(intent);
	}
	
	private void initTocDialog() {
		if (this.tocDialog != null) {
			return;
		}
		
		final List<BookView.TocEntry> tocList = this.bookView.getTableOfContents();
		
		if (tocList == null || tocList.isEmpty()) {
			return;
		}
		
		final CharSequence[] items = new CharSequence[tocList.size()];
		
		for (int i = 0; i < items.length; i++) {
			items[i] = tocList.get(i).getTitle();
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.toc_label);
		
		builder.setItems(items, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				bookView.navigateTo(tocList.get(which).getHref());
			}
		});
		
		this.tocDialog = builder.create();
		this.tocDialog.setOwnerActivity(this);
	}
	
	private void updateFileName(Bundle savedInstanceState, String fileName) {
		this.fileName = fileName;
		
		int lastPos = config.getLastPosition(fileName);
		int lastIndex = config.getLastIndex(fileName);
		
		if (savedInstanceState != null) {
			lastPos = savedInstanceState.getInt(POS_KEY, -1);
    		lastIndex = savedInstanceState.getInt(IDX_KEY, -1);
		}
		
		this.bookView.setFileName(fileName);
		this.bookView.setPosition(lastPos);
		this.bookView.setIndex(lastIndex);
		
		config.setLastOpenedFile(fileName);
	}
	
	/**
     * Immediately updates the text size in the BookView,
     * and saves the preference in the background.
     * 
     * @param textSize
     */
	private void updateTextSize(final int textSize) {
		this.bookView.setTextSize(textSize);
		this.backgroundHandler.post(new Runnable() {
			
			@Override
			public void run() {
				config.setTextSize((int)textSize);
			}
		});
	}
	
	@Override
	public void progressUpdate(int progressPercentage) {
		//Work-around for calculation errors and weird values.
		if (progressPercentage < 0 || progressPercentage > 100) {
    		return;
    	}
		
		this.progressPercentage = progressPercentage;
		this.percentageText.setText("" + this.progressPercentage + "% ");
		
		this.progressBar.setProgress(this.progressPercentage);
		this.progressBar.setMax(100);
	}
	
	private void updateFromPrefs() {
		//this.progressService.setConfig(this.config);
		
		this.bookView.setTextSize(this.config.getTextSize());
		
		int marginH = this.config.getHorizontalMargin();
        int marginV = this.config.getVerticalMargin();
        
        this.bookView.setFontFamily(this.config.getFontFamily());
        
        this.bookView.setHorizontalMargin(marginH);
        this.bookView.setVerticalMargin(marginV);
        
        if (!isAnimating()) {
        	this.bookView.setEnableScrolling(this.config.isScrollingEnabled());
        }
        
        this.bookView.setStripWhiteSpace(this.config.isStripWhiteSpaceEnabled()); 
        this.bookView.setLineSpacing(this.config.getLineSpacing());
        
        if (config.isFullScreenEnabled()) {
        	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        	this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);            
        } else {    
        	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        	this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);        	
    	}
        
        if (this.config.isKeepScreenOn() ) {
        	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
        	this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        
        this.restoreColorProfile();
        
        //Check if we need a restart
        if ( config.isBrightnessControlEnabled() != oldBrightness
        		|| config.isStripWhiteSpaceEnabled() != oldStripWhiteSpace
        		|| ! this.oldFontName.equalsIgnoreCase(config.getFontFamily().getName())) {
        	
        	Intent intent = new Intent(this, ReadingActivity.class);
        	intent.setData(Uri.parse(this.fileName));
        	startActivity(intent);
        	finish();
        }
        
        Configuration.OrientationLock orientation = config.getScreenOrientation(); 
        
        switch (orientation) {
	        case PORTRAIT:
	        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	        	break;
	        case LANDSCAPE:
	        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	        	break;
	        case REVERSE_LANDSCAPE:
	        	this.setRequestedOrientation(8); //Android 2.3+ value
	        	break;
	        case REVERSE_PORTRAIT:
	        	this.setRequestedOrientation(9); //Android 2.3+ value
	        	break;
	        default:
	        	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			this.updateFromPrefs();
		} else {
			this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return this.bookView.onTouchEvent(event);
	}
	
	@Override
	public void bookOpened(final Book book) {
		this.bookTitle = book.getTitle();
		this.titleBase = this.bookTitle;
		
		this.setTitle(this.titleBase);
		this.titleBarText.setText(this.titleBase);
		
		if (book.getMetadata() != null && !book.getMetadata().getAuthors().isEmpty()) {
			Author author = book.getMetadata().getAuthors().get(0);
			this.authorText.setText(author.getFirstname() + " " + author.getLastname());
		}
		
		this.backgroundHandler.post(new Runnable() {
			
			@Override
			public void run() {
				try {
					libraryService.storeBook(fileName, book, true, config.isCopyToLibrayEnabled() );
				} catch (IOException e) {
					LOG.error("Copy to library failed.", e);
				}
			}
		});
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		//This is a hack to give the longclick handler time
    	//to find the word the user long clicked on.    	
		if (this.selectedWord != null) {
			final CharSequence word = this.selectedWord;
			
			String header = String.format(this.getString(R.string.word_select), this.selectedWord);
			menu.setHeaderTitle(header);
			
			final Intent intent = new Intent(PICK_RESULT_ACTION);
			intent.putExtra(EXTRA_QUERY, word.toString()); //Search Query
			intent.putExtra(EXTRA_FULLSCREEN, false);
			intent.putExtra(EXTRA_HEIGHT, 400); //400pixel, if you don't specify, fill_parent"
			intent.putExtra(EXTRA_GRAVITY, Gravity.BOTTOM);
			intent.putExtra(EXTRA_MARGIN_LEFT, 100);
			
			if (isIntentAvailable(this, intent)) {
				MenuItem item = menu.add(this.getString(R.string.dictionary_lookup));
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						startActivityForResult(intent, 5);
						return true;
					}
				});
			}
			
			MenuItem newItem = menu.add(this.getString(R.string.wikipedia_lookup));
			newItem.setOnMenuItemClickListener(
					new BrowserSearchMenuItemClickListener(
							"http://en.wikipedia.org/wiki/Special:Search?search=" +
							URLEncoder.encode(word.toString())));
			
			this.selectedWord = null;
		}
	}
	
	public static boolean isIntentAvailable(Context context, Intent intent) {
		final PackageManager packageManage = context.getPackageManager();
		List<ResolveInfo> list = packageManage.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		
		return list.size() > 0;
	}
	
	private void restoreColorProfile() {
    	this.bookView.setBackgroundColor(config.getBackgroundColor());
    	this.viewSwitcher.setBackgroundColor(config.getBackgroundColor());
    	this.bookView.setTextColor(config.getTextColor());   
    	this.bookView.setLinkColor(config.getLinkColor());

    	int brightness = config.getBrightNess();
    	
    	if (this.config.isBrightnessControlEnabled()) {
    		this.setScreenBrightnessLevel(brightness);
    	}  
    }    
	
	private void setScreenBrightnessLevel(int level) {
    	WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = (float) level / 100f;
		getWindow().setAttributes(lp);
    }
	
	@Override
	public void errorOnBookOpening(String errorMessage) {
		this.waitDialog.hide();
		String message = String.format(this.getString(R.string.error_open_bk), errorMessage);
		this.bookView.setText(new SpannedString(message));
	}
	
	@Override
	public void parseEntryComplete(int entry, String name) {
		if (name != null && !name.equals(this.bookTitle)) {
			this.titleBase = this.bookTitle + " - " + name;
		} else {
			this.titleBase = this.bookTitle;
		}
		
		this.setTitle(this.titleBase);
		this.waitDialog.hide();
	}
	
	@Override
	public void parseEntryStart(int entry) {
		this.viewSwitcher.clearAnimation();
		this.viewSwitcher.setBackgroundDrawable(null);
		this.restoreColorProfile();
		
		this.waitDialog.setTitle(this.getString(R.string.loading_wait));
		this.waitDialog.show();
	}
	
	private boolean isAnimating() {
    	Animator anim = dummyView.getAnimator();
    	
    	return anim != null && !anim.isFinished();
    }

	private void doPageCurl(boolean flipRight) {
		if (this.isAnimating()) {
			return;
		}
		
		this.viewSwitcher.setInAnimation(null);
		this.viewSwitcher.setOutAnimation(null);
		
		if (viewSwitcher.getCurrentView() == this.dummyView) {
			this.viewSwitcher.showNext();
		}
		
		Bitmap before = this.getBookViewSnapshot();
		PageCurlAnimator animator = new PageCurlAnimator(flipRight);
		
		//Pagecurls should only take a few frames. When the screen gets
    	//bigger, so do the frames.
		animator.SetCurlSpeed(this.bookView.getWidth() / 8);
		animator.setBackgroundColor(this.config.getBackgroundColor());
		
		LOG.debug("Before size: w=" + before.getWidth() + " h=" + before.getHeight() ); 
		
		if (flipRight) {
			this.bookView.pageDown();
			
			Bitmap after = this.getBookViewSnapshot();
//			LOG.debug("After size: w=" + after.getWidth() + " h=" + after.getHeight() );
			
			animator.setBackgroundBitmap(after);
			animator.setForegroundBitmap(before);
		} else {
			this.bookView.pageUp();
			
    		Bitmap after = getBookViewSnapshot();
//    		LOG.debug("After size: w=" + after.getWidth() + " h=" + after.getHeight() );
    		
    		animator.setBackgroundBitmap(before);
    		animator.setForegroundBitmap(after);
		}
		
		this.dummyView.setAnimator(animator);
		
		this.viewSwitcher.showNext();
		
		this.uiHandler.post(new PageCurlRunnable(animator));
		
		this.dummyView.invalidate();
	}
	
	private void stopAnimation() {
		if (this.dummyView.getAnimator() != null) {
			this.dummyView.getAnimator().stop();
			this.dummyView.setAnimator(null);
		}
		
		if (this.viewSwitcher.getCurrentView() == this.dummyView) {
			this.viewSwitcher.showNext();
		}
		
		this.bookView.setKeepScreenOn(false);
	}

	private Bitmap getBookViewSnapshot() {
		try {
			Bitmap bitmap = Bitmap.createBitmap(this.viewSwitcher.getWidth(), this.viewSwitcher.getHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			
			this.bookView.layout(0, 0, this.viewSwitcher.getWidth(), this.viewSwitcher.getHeight());
			this.bookView.draw(canvas);
			
			return bitmap;
		} catch (OutOfMemoryError e) {
			this.viewSwitcher.setBackgroundColor(this.config.getBackgroundColor());
		}
		
		return null;
	}

	private void prepareSlide(Animation inAnim, Animation outAnim) {
		Bitmap bitmap = this.getBookViewSnapshot();
		
		this.dummyView.setImageBitmap(bitmap);
		
		this.viewSwitcher.layout(0, 0, this.viewSwitcher.getWidth(), this.viewSwitcher.getHeight());
		this.dummyView.layout(0, 0, this.viewSwitcher.getWidth(), this.viewSwitcher.getHeight());
		
		this.viewSwitcher.showNext();
		
		this.viewSwitcher.setInAnimation(inAnim);
		this.viewSwitcher.setOutAnimation(outAnim);
	}

	private void pageDown(Orientation orientation) {
		if (this.bookView.isAtEnd()) {
			return;
		}
		
		this.stopAnimation();
		
		if (orientation == Orientation.HORIZONTAL) {
			AnimationStyle animStyle = this.config.getHorizontalAnim();
			
			if (animStyle == AnimationStyle.CURL) {
				this.doPageCurl(true);
			} else if (animStyle == AnimationStyle.SLIDE) {
				this.prepareSlide(Animations.inFromRightAnimation(), Animations.outToLeftAnimation());
				
				this.viewSwitcher.showNext();
				
				this.bookView.pageDown();
			} else {
				this.bookView.pageDown();
			}
		} else {
			if (this.config.getVerticalAnim() == AnimationStyle.SLIDE) {
				this.prepareSlide(Animations.inFromBottomAnimation(), Animations.outToTopAnimation());
				
				this.viewSwitcher.showNext();
			}
			
			this.bookView.pageDown();
		}
	}
	
	private void pageUp(Orientation orientation) {
    	
    	if (this.bookView.isAtStart()) {
    		return;
    	}
    	
    	this.stopAnimation();   	
    	
    	if (orientation == Orientation.HORIZONTAL) {
    		
    		AnimationStyle animStyle = config.getHorizontalAnim();
    		
    		if (animStyle == AnimationStyle.CURL) {
    			this.doPageCurl(false);
    		} else if (animStyle == AnimationStyle.SLIDE) {
    			prepareSlide(Animations.inFromLeftAnimation(), Animations.outToRightAnimation());
    			
        		this.viewSwitcher.showNext();
        		
        		this.bookView.pageUp();
    		} else {
    			this.bookView.pageUp();
    		}
    		
    	} else {
    		if (this.config.getVerticalAnim() == AnimationStyle.SLIDE ){    	
    			this.prepareSlide(Animations.inFromTopAnimation(), Animations.outToBottomAnimation());    		
    			this.viewSwitcher.showNext();
    		}
    		
    		this.bookView.pageUp();
    	}    	
    }

	

	@Override
	public void onWordLongPressed(SelectionWord selectionWord) {
//		this.selectedWord = selectionWord.word;
		
//		this.openContextMenu(this.bookView);
		
		this.hasJustSelectedWord = true;
		
		LOG.info(String.format("####################### %d %d", selectionWord.start, selectionWord.end));
		this.textSelection.doSelectionParagraph(selectionWord.start, selectionWord.end);
		this.textSelection.show();
	}

	@Override
	public boolean onSwipeUp() {
		if (this.config.isVerticalSwipeEnabled()) {
			this.pageUp(Orientation.VERTICAL);
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean onSwipeDown() {
		if (this.config.isVerticalSwipeEnabled()) {
			this.pageDown(Orientation.VERTICAL);
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean onSwipeLeft() {
		if (this.config.isHorizontalSwipeEnabled()) {
			this.pageDown(Orientation.HORIZONTAL);
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean onSwipeRight() {
		if (this.config.isHorizontalSwipeEnabled()) {
			this.pageUp(Orientation.HORIZONTAL);
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean onTapLeftEdge() {
		if (this.config.isHorizontalTappingEnabled()) {
			this.pageUp(Orientation.HORIZONTAL);
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean onTapRightEdge() {
		if (this.config.isHorizontalTappingEnabled()) {
			this.pageDown(Orientation.HORIZONTAL);
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean onTapTopEdge() {
		if (this.config.isVerticalTappingEnabled()) {
			this.pageUp(Orientation.VERTICAL);
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean onTapBottomEdge() {
		if (this.config.isVerticalTappingEnabled()) {
			this.pageDown(Orientation.VERTICAL);
			
			return true;
		}
		
		return false;
	}

	@Override
	public boolean onLeftEdgeSlide(int value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onRightEdgeSlide(int value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onScreenTap() {
		if (this.getSupportActionBar().isShowing()) {
			this.getSupportActionBar().hide();
			this.hideTitleBar();
		} else {
			this.getSupportActionBar().show();
			this.showTitleBar();
		}
	}
	
	private class BrowserSearchMenuItemClickListener implements MenuItem.OnMenuItemClickListener {
		private String launchURL;
		public BrowserSearchMenuItemClickListener(String launchURL) {
			this.launchURL = launchURL;
		}
		
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(this.launchURL));
			startActivity(intent);
			
			return true;
		}
	}
	
	private class PageCurlRunnable implements Runnable {
		private PageCurlAnimator animator;

		public PageCurlRunnable(PageCurlAnimator animator) {
			this.animator = animator;
		}
		
		@Override
		public void run() {
			if (this.animator.isFinished()) {
				if (viewSwitcher.getCurrentView() == dummyView) {
					viewSwitcher.showNext();
				}
				
				dummyView.setAnimator(null);
			} else {
				this.animator.advanceOneFrame();
				dummyView.invalidate();
				
				int delay = 1000 / this.animator.getAnimationSpeed();
				
				uiHandler.postDelayed(this, delay);
			}
		}
		
	}
	
//	private class CurlPageProvider implements PageProvider {
//
//		@Override
//		public int getPageCount() {
//			return 5;
//		}
//
//		@Override
//		public void updatePage(CurlPage page, int width, int height, int index) {
//			page.setTexture(getBookViewSnapshot(), CurlPage.SIDE_BOTH);
////			page.setColor(0xFFFFFF, CurlPage.SIDE_BOTH);
//		}
//		
//	}
//	
//	private class CurlPageSizeChangedObserver implements SizeChangedObserver {
//
//		@Override
//		public void onSizeChanged(int width, int height) {
//			curlView.setViewMode(CurlView.SHOW_ONE_PAGE);
//			curlView.setMargins(0.0f, 0.0f, 0.0f, 0.0f);
//		}
//		
//	}
}
