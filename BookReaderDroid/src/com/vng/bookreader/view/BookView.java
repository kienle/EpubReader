package com.vng.bookreader.view;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.vng.bookreader.epub.PageTurnerSpine;
import com.vng.bookreader.epub.ResourceLoader;
import com.vng.bookreader.epub.ResourceLoader.ResourceCallback;
import com.vng.bookreader.selection.SelectionWord;

import net.nightwhistler.htmlspanner.FontFamily;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import net.nightwhistler.htmlspanner.handlers.TableHandler;
import net.nightwhistler.htmlspanner.spans.CenterSpan;
import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.StringUtil;

import android.R.layout;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class BookView extends ScrollView {
	private int storedIndex;
	private String storedAnchor;
	
	private TextView childView;
	
	private Set<BookViewListener> listeners;
	
	private HtmlSpanner spanner;
	private TableHandler tableHandler;
	
	private PageTurnerSpine spine;
	
	private String fileName;
	private Book book;
	
	private Map<String, Integer> anchors;
	
	private int prevIndex = -1;
	private int prevPos = -1;
	
	private PageChangeStrategy strategy;
	private ResourceLoader loader;
	
	private int horizontalMargin = 0;
	private int verticalMargin = 0;
	private int lineSpacing = 0;
	
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BookView.class);

	@SuppressWarnings("deprecation")
	public BookView(Context context, AttributeSet attributes) {
		super(context, attributes);
		
		this.listeners = new HashSet<BookViewListener>();
		
		this.childView = new TextView(context) {
			@Override
			protected void onSizeChanged(int w, int h, int oldw, int oldh) {
				super.onSizeChanged(w, h, oldw, oldh);
				
				BookView.this.restorePosition();
				
				int tableWidth = (int)(this.getWidth() * 0.9);
				tableHandler.setTableWidth(tableWidth);
			}
			
			public boolean dispatchKeyEvent(KeyEvent event) {
				return BookView.this.dispatchKeyEvent(event);
			}
			
			@Override
			protected void onSelectionChanged(int selStart, int selEnd) {
				super.onSelectionChanged(selStart, selEnd);
				
				LOG.debug("Got text selection from " + selStart + " to " + selEnd);
			}
		};
		
		this.childView.setCursorVisible(false);
		this.childView.setLongClickable(true);
		this.setVerticalFadingEdgeEnabled(false);
		this.childView.setFocusable(true);
		this.childView.setLinksClickable(true);
		this.childView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		MovementMethod m = this.childView.getMovementMethod();
		if (m == null || !(m instanceof LinkMovementMethod)) {
			if (this.childView.getLinksClickable()) {
				this.childView.setMovementMethod(LinkMovementMethod.getInstance());
			}
		}
		
		this.setSmoothScrollingEnabled(false);
		this.addView(this.childView);
		
		this.anchors = new HashMap<String, Integer>();
		this.tableHandler = new TableHandler();
	}
	
	public TextView getChildView() {
		return this.childView;
	}
	
	public void setSpanner(HtmlSpanner spanner) {
		this.spanner = spanner;
		
		ImageTagHandler imgHandler = new ImageTagHandler();
		this.spanner.registerHandler("img", imgHandler);
		this.spanner.registerHandler("image", imgHandler);
		
		this.spanner.registerHandler("a", new AnchorHandler(new LinkTagHandler()));
		
		this.spanner.registerHandler("h1", new AnchorHandler(this.spanner.getHandlerFor("h1")));
		this.spanner.registerHandler("h2", new AnchorHandler(this.spanner.getHandlerFor("h2")));
		this.spanner.registerHandler("h3", new AnchorHandler(this.spanner.getHandlerFor("h3")));
		this.spanner.registerHandler("h4", new AnchorHandler(this.spanner.getHandlerFor("h4")));
		this.spanner.registerHandler("h5", new AnchorHandler(this.spanner.getHandlerFor("h5")));
		this.spanner.registerHandler("h6", new AnchorHandler(this.spanner.getHandlerFor("h6")));
		
		this.spanner.registerHandler("p", new AnchorHandler(this.spanner.getHandlerFor("p")));
		this.spanner.registerHandler("table", this.tableHandler);
	}
	
	/**
	 * Returns if we're at the start of the book, i.e. displaying the title page.
	 * @return
	 */
	public boolean isAtStart() {
		if (this.spine == null) {
			return true;
		}
		
		return this.spine.getPosition() == 0 && this.strategy.isAtStart();
	}
	
	public boolean isAtEnd() {
		if (this.spine == null) {
			return false;
		}
		
		return this.spine.getPosition() >= this.spine.size() && this.strategy.isAtEnd();
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
		this.loader = new ResourceLoader(fileName);
	}
	
	@Override
	public void setOnTouchListener(OnTouchListener l) {
		super.setOnTouchListener(l);
		
		this.childView.setOnTouchListener(l);
	}
	
	public void setStripWhiteSpace(boolean stripWhiteSpace) {
		this.spanner.setStripExtraWhiteSpace(stripWhiteSpace);
	}
	
	public boolean hasLinkAt(float x, float y) {
		Integer offset = this.findOffsetForPosition(x, y);
		
		if (offset == null) {
			return false;
		}
		
		Spanned text = (Spanned)this.childView.getText();
		ClickableSpan[] spans = text.getSpans(offset, offset, ClickableSpan.class);
		
		return spans != null && spans.length > 0;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return this.childView.onTouchEvent(ev);
	}
	
	public boolean hasPrevPosition() {
		return this.prevIndex != -1 && this.prevPos != -1;
	}
	
	public void setLineSpacing(int lineSpacing) {
		if (this.lineSpacing != lineSpacing) {
			this.lineSpacing = lineSpacing;
			this.childView.setLineSpacing(this.lineSpacing, 1.0f);
			
			if (this.strategy != null) {
				this.strategy.updatePosition();
			}
		}
	}
	
	public int getLineSpacing() {
		return this.lineSpacing;
	}
	
	public void setHorizontalMargin(int horizontalMargin) {
		if (this.horizontalMargin != horizontalMargin) {
			this.horizontalMargin = horizontalMargin;
			this.setPadding(this.horizontalMargin, this.verticalMargin, this.horizontalMargin, this.verticalMargin);
			
			if (this.strategy != null) {
				this.strategy.updatePosition();
			}
		}
	}
	
	public void setLinkColor(int color) {
		this.childView.setLinkTextColor(color);
	}
	
	public void setVerticalMargin(int verticalMargin) {
		if (this.verticalMargin != verticalMargin) {
			this.verticalMargin = verticalMargin;
			this.setPadding(this.horizontalMargin, this.verticalMargin, this.horizontalMargin, this.verticalMargin);
			
			if (this.strategy != null) {
				this.strategy.updatePosition();
			}
		}
	}
	
	public int getHorizontalMargin() {
		return horizontalMargin;
	}
	
	public int getVerticalMargin() {
		return verticalMargin;
	}
	
	public void goBackInHistory() {
		this.strategy.clearText();
		this.spine.navigateByIndex(this.prevIndex);
		this.strategy.setPosition(this.prevPos);
		
		this.storedAnchor = null;
		this.prevIndex = -1;
		this.prevPos = -1;
		
		this.loadText();
	}
	
	public void clear() {
		this.childView.setText("");
		this.anchors.clear();
		this.storedAnchor = null;
		this.storedIndex = -1;
		this.book = null;
		this.fileName = null;
		
		this.strategy.reset();
	}
	
	/**
	 * Loads the text and saves the restored position.
	 */
	public void restore() {
		this.strategy.clearText();
		this.loadText();
	}
	
	public void setIndex(int index) {
		this.storedIndex = index;
	}
	
	void loadText() {		
        new LoadTextTask().execute();
	}
	
	public void setFontFamily(FontFamily family) {
		this.childView.setTypeface(family.getDefaultTypeface());
		this.tableHandler.setTypeFace(family.getDefaultTypeface());
		
		this.spanner.setFontFamily(family);
	}
	
	public void pageDown() {
		this.strategy.pageDown();
	}
	
	public void pageUp() {
		this.strategy.pageUp();
	}
	
	@Override
	public void scrollBy(int x, int y) {
		super.scrollBy(x, y);
		
		progressUpdate();
	}
	
	TextView getInnerView() {
		return this.childView;
	}
	
	PageTurnerSpine getSpine() {
		return this.spine;
	}
	
	@Override
	public void scrollTo(int x, int y) {
		super.scrollTo(x, y);
		
		progressUpdate();
	}
	
	private Integer findOffsetForPosition(float x, float y) {
		if (this.childView == null || this.childView.getLayout() == null) {
			return null;
		}
		
		Layout layout = this.childView.getLayout();
		int line = layout.getLineForVertical((int)y);
		
		return layout.getOffsetForHorizontal(line, x);
	}
	
	/**
	 * Returns the full word containing the character at the selected location.
	 * @param x
	 * @param y
	 * @return
	 */
	public SelectionWord getWordAt(float x, float y) {
		if (this.childView == null) {
			return null;
		}
		
		CharSequence text = this.childView.getText();
		
		if (text.length() == 0) {
			return null;
		}
		
		Integer offset = this.findOffsetForPosition(x, y);
		
		if (offset == null) {
			return null;
		}
		
		LOG.info(String.format("getWordAt:findOffsetForPosition: %d", offset));
		
		if (offset < 0 || offset > text.length() - 1) {
			return null;
		}
		
		if (BookView.isBoundaryCharacter(text.charAt(offset))) {
			return null;
		}
		
		int left = Math.max(0, offset - 1);
		int right = Math.min(text.length(), offset);
		
		CharSequence word = text.subSequence(left, right);
		while (left > 0 && !BookView.isBoundaryCharacter(word.charAt(0))) {
			left--;
			word = text.subSequence(left, right);
		}
		
		if (word.length() == 0) {
			return null;
		}
		
		while (right < text.length() && !BookView.isBoundaryCharacter(word.charAt(word.length() - 1))) {
			right++;
			word = text.subSequence(left, right);
		}
		
		int start = 0;
		int end = word.length();
		
		if (BookView.isBoundaryCharacter(word.charAt(0))) {
			start = 1;
			left++;
		}
		
		if (BookView.isBoundaryCharacter(word.charAt(word.length() - 1))) {
			end = word.length() - 1;
			right--;
		}
		
		if (0 <= start && start < word.length() && end < word.length()) {
			return new SelectionWord(word.subSequence(start, end), left, right);
		}
		
		return null;
	}
	
	private static boolean isBoundaryCharacter( char c ) {
		char[] boundaryChars = { ' ', '.', ',','\"',
				'\'', '\n', '\t', ':', '!'
		};
		
		for ( int i=0; i < boundaryChars.length; i++ ) {
			if (boundaryChars[i] == c) {
				return true;
			}		
		}
		
		return false;
	}
	
	public void navigateTo(String rawHref) {
		this.prevIndex = this.getIndex();
		this.prevPos = this.getPosition();
		
		//URLDecode the href, so it does not contain %20 etc.
		@SuppressWarnings("deprecation")
		String href = URLDecoder.decode(StringUtil.substringBefore(rawHref, Constants.FRAGMENT_SEPARATOR_CHAR));
		
		//Don't decode the anchor.
		String anchor = StringUtil.substringAfter(rawHref, Constants.FRAGMENT_SEPARATOR_CHAR);
		
		if (!anchor.equals("")) {
			this.storedAnchor = anchor;
		}
		
		this.strategy.clearText();
		this.strategy.setPosition(0);
		
		if (this.spine.navigateByHref(href)) {
			this.loadText();
		} else {
			new LoadTextTask().execute(href);
		}
	}
	
	public void navigateToPercentage(int percentage) {
		if (this.spine == null) {
			return;
		}
		
		double targetPoint = (double)percentage / 100d;
		List<Double> percentages = this.spine.getRelativeSize();
		
		if (percentages == null || percentages.isEmpty()) {
			return;
		}
		
		int index = 0;
		double total = 0;
		
		for ( ; total < targetPoint && index < percentages.size(); index++) {
			total += percentages.get(index);
		}
		
		index--;
		
		//Work-around for when we get multiple events.
		if (index < 0 || index >= percentages.size()) {
			return;
		}
		
		double partBefore = total - percentages.get(index);
		double progressInPart = (targetPoint - partBefore) / percentages.get(index);
		
		this.prevPos = this.getPosition();
		this.strategy.setRelativePosition(progressInPart);
		
		this.doNavigation(index);
	}
	
	private void doNavigation(int index) {
		//Check if we're already in the right part of the book
		if (this.getIndex() == index) {
			this.restorePosition();
			return;
		}
		
		this.prevIndex = this.getIndex();
		
		this.storedIndex = index;
		this.strategy.clearText();
		this.spine.navigateByIndex(index);
		
		this.loadText();
	}
	
	public void navigateTo(int index, int position) {
		this.prevPos = this.getPosition();
		this.strategy.setPosition(position);
		
		doNavigation(index);
	}
	
	public List<TocEntry> getTableOfContents() {
		if (this.book == null) {
			return null;
		}
		
		List<TocEntry> result = new ArrayList<BookView.TocEntry>();
		
		this.flatten(this.book.getTableOfContents().getTocReferences(), result, 0);
		
		return result;
	}
	
	private void flatten(List<TOCReference> refs, List<TocEntry> entries, int level) {
		if (refs == null || refs.isEmpty()) {
			return;
		}
		
		for (TOCReference ref : refs) {
			String title = "";
			
			for (int i = 0; i < level; i++) {
				title += "-";
			}
			
			title += ref.getTitle();
			
			if (ref.getResource() != null) {
				entries.add(new TocEntry(title, this.spine.resolveTocHref(ref.getCompleteHref())));
			}
			
			this.flatten(ref.getChildren(), entries, level + 1);
		}
	}
	
	@Override
	public void fling(int velocityY) {
		this.strategy.clearStoredPosition();
		
		super.fling(velocityY);
	}
	
	public int getIndex() {
		if (this.spine == null) {
			return this.storedIndex;
		}
		
		return this.spine.getPosition();
	}
	
	public int getPosition() {
		return this.strategy.getPosition();
	}
	
	public void setPosition(int pos) {
		this.strategy.setPosition(pos);
	}
	
	/**
	 * Scrolls to a previously stored point.
	 * 
	 * Call this after setPosition() to actually go there.
	 */
	private void restorePosition() {
		if (this.storedAnchor != null && this.anchors.containsKey(this.storedAnchor)) {
			this.strategy.setPosition(this.anchors.get(this.storedAnchor));
			this.storedAnchor = null;
		}
		
		if (this.strategy != null) {
			this.strategy.updatePosition();
		}
	}
	
	@Override
	public void setBackgroundColor(int color) {
		super.setBackgroundColor(color);
		
		if (this.childView != null) {
			this.childView.setBackgroundColor(color);
		}
	}
	
	public void setTextColor(int color) {
		if (this.childView != null) {
			this.childView.setTextColor(color);
		}
		
		this.tableHandler.setTextColor(color);
	}
	
	/**
	 * Sets the given text to be displayed, overriding the book.
	 * 
	 * @param text
	 */
	public void setText(Spanned text) {
		this.strategy.loadText(text);
		this.strategy.updatePosition();
	}
	
	public Book getBook() {
		return this.book;
	}
	
	public float getTextSize() {
		return this.childView.getTextSize();
	}
	
	public void setTextSize(float textSize) {
		this.childView.setTextSize(textSize);
		this.tableHandler.setTextSize(textSize);
	}
	
	public void addListener(BookViewListener listener) {
		this.listeners.add(listener);
	}
	
	private void bookOpened(Book book) {
		for (BookViewListener listener : this.listeners) {
			listener.bookOpened(book);
		}
	}
	
	private void errorOnBookOpening(String errorMessage) {
		for (BookViewListener listener: this.listeners) {
			listener.errorOnBookOpening(errorMessage);
		}
	}	 
	
	private void parseEntryStart(int entry) {
		for (BookViewListener listener: this.listeners) {
			listener.parseEntryStart(entry);
		}
	}	
	
	private void parseEntryComplete(int entry, String name) {
		for (BookViewListener listener: this.listeners) {
			listener.parseEntryComplete(entry, name);
		}
	}
	
	private void progressUpdate() {
		if (this.spine != null && this.strategy.getText() != null && this.strategy.getText().length() > 0) {
			double progressInPart = (double)this.getPosition() / (double)this.strategy.getText().length();
			
			if (this.strategy.getText().length() > 0 && this.strategy.isAtEnd()) {
				progressInPart = 1d;
			}
			
			int progress = this.spine.getProgressPercentage(progressInPart);
			
			if (progress != -1) {
				for (BookViewListener listener : this.listeners) {
					listener.progressUpdate(progress);
				}
			}
		}
	}
	
	
	public void setEnableScrolling(boolean enableScrolling) {
		if (this.strategy == null || this.strategy.isScrolling() != enableScrolling) {
			int pos = -1;
			boolean wasNull = true;
			
			Spanned text = null;
			
			if (this.strategy != null) {
				pos = this.strategy.getPosition();
				text = this.strategy.getText();
				
				this.strategy.clearText();
				wasNull = false;
			}
			
			if (enableScrolling) {
				
			} else {
				this.strategy = new SinglePageStrategy(this);
			}
			
			if (!wasNull) {
				this.strategy.setPosition(pos);
			}
			
			if (text != null && text.length() > 0) {
				this.strategy.loadText(text);
			}
		}
	}
	
	public void setBook(Book book) {
		this.book = book;
		this.spine = new PageTurnerSpine(this.book);
		this.spine.navigateByIndex(this.storedIndex);
	}
	
	private void initBook() throws IOException {
		if (this.fileName == null) {
			throw new IOException("No file-name specified.");
		}
		
		// Read epub file
		EpubReader epubReader = new EpubReader();
		
		MediaType[] lazyTypes = {
				MediatypeService.CSS, //We don't support CSS yet
				
				MediatypeService.GIF, MediatypeService.JPG,
				
				MediatypeService.PNG, MediatypeService.SVG, //Handled by the ResourceLoader
				
				MediatypeService.OPENTYPE, MediatypeService.TTF, //We don't support custom fonts either
				MediatypeService.XPGT,
				
				/*MediatypeService.MP3, MediatypeService.MP4,*/ //And no audio either
				/*MediatypeService.SMIL, */MediatypeService.XPGT,
				/*MediatypeService.PLS*/
		};
		
		Book newBook = epubReader.readEpubLazy(this.fileName, "UTF-8", Arrays.asList(lazyTypes));
		this.setBook(newBook);
	}
	
	/**
	 * Many books use <p> and <h1> tags as anchor points.
	 * This class harvests those point by wrapping the original
	 * handler.
	 * 
	 * @author Alex Kuiper
	 *
	 */
	private class AnchorHandler extends TagNodeHandler {
		private TagNodeHandler wrappedHandler;
		
		public AnchorHandler(TagNodeHandler wrappedHandler) {
			this.wrappedHandler = wrappedHandler;
		}
		
		@Override
		public void handleTagNode(org.htmlcleaner.TagNode node, SpannableStringBuilder builder,
				int start, int end) {
			
			String id = node.getAttributeByName("id");
			if (id != null) {
				BookView.this.anchors.put(id, start);
			}
			
			wrappedHandler.handleTagNode(node, builder, start, end);
		}
	}
	
	/**
	 * Creates clickable links.
	 * 
	 * @author work
	 *
	 */
	private class LinkTagHandler extends TagNodeHandler {
		private List<String> externalProtocols;
		
		public LinkTagHandler() {
			this.externalProtocols = new ArrayList<String>();
			this.externalProtocols.add("http://");
			this.externalProtocols.add("https://");
			this.externalProtocols.add("http://");
			this.externalProtocols.add("ftp://");
			this.externalProtocols.add("mailto:");
		}

		@Override
		public void handleTagNode(org.htmlcleaner.TagNode node, SpannableStringBuilder builder,
				int start, int end) {
			
			final String href = node.getAttributeByName("href");
			
			if (href == null) {
				return;
			}
			
			//First check if it should be a normal URL link
			for (String protocol : this.externalProtocols) {
				if (href.toLowerCase().startsWith(protocol)) {
					builder.setSpan(new URLSpan(href), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					return;
				}
			}
			
			//If not, consider it an internal nav link.			
			ClickableSpan span = new ClickableSpan() {
				
				@Override
				public void onClick(View widget) {
					navigateTo(BookView.this.spine.resolveHref(href));
					
				}
			};
			
			builder.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}
	
	private class LoadTextTask extends AsyncTask<String, Integer, Spanned> {
		private String name;
		private boolean wasBookLoaded;
		
		private String error;
		
		@Override
		protected void onPreExecute() {
			this.wasBookLoaded = book != null;
			parseEntryStart(getIndex());
		}

		@Override
		protected Spanned doInBackground(String... hrefs) {
			if (loader != null) {
				loader.clear();
			}
			
			if (book == null) {
				try {
					initBook();
				} catch (IOException io ) {
					this.error = io.getMessage();
					return null;
				}
			}
			
			this.name = spine.getCurrentTitle();
			
			Resource resource;
			
			if (hrefs.length == 0) {
				resource = spine.getCurrentResource();
			} else {
				resource = book.getResources().getByHref(hrefs[0]);
			}
			
			if (resource == null) {
				return new SpannedString("Sorry, it looks like you clicked a dead link.\nEven books have 404s these days." );
			}
			
			try {
				Spanned result = spanner.fromHtml(resource.getReader());
				loader.load(); //Load all image resources.
				
				return result;
			} catch (IOException io ) {
				return new SpannableString( "Could not load text: " + io.getMessage() );
			}	
		}
		
		@Override
		protected void onPostExecute(Spanned result) {
			if (!wasBookLoaded) {
				if (book != null) {
					bookOpened(book);
				} else {
					errorOnBookOpening(this.error);
					return;
				}
			}
			
			restorePosition();
			strategy.loadText(result);
			
			parseEntryComplete(spine.getPosition(), this.name);
		}
		
	}
	
	public static class TocEntry {
		private String title;
		private String href;
		
		public TocEntry(String title, String href) {
			this.title = title;
			this.href = href;
		}
		
		public String getHref() {
			return href;
		}
		
		public String getTitle() {
			return title;
		}
	}
	
	private class ImageCallback implements ResourceCallback {
		private SpannableStringBuilder builder;
		private int start;
		private int end;
		
		public ImageCallback(SpannableStringBuilder builder, int start, int end) {
			this.builder = builder;
			this.start = start;
			this.end = end;
		}

		@Override
		public void onLoadResource(String href, InputStream stream) {
			Bitmap bitmap = null;
			
			try {
				bitmap = getBitmap(stream);
				
				if (bitmap == null || bitmap.getHeight() < 1 || bitmap.getWidth() < 1) {
					return;
				}
			} catch (OutOfMemoryError outofmem) {
				LOG.error("Could not load image", outofmem);
			}
			
			if (bitmap != null) {
				Drawable drawable = new FastBitmapDrawable(bitmap);
				drawable.setBounds(0, 0, bitmap.getWidth() - 1, bitmap.getHeight() - 1);
				builder.setSpan(new ImageSpan(drawable), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				
				if (spine.isCover()) {
					builder.setSpan(new CenterSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
		}
		
		private Bitmap getBitmap(InputStream input) {
			Bitmap originalBitmap = BitmapFactory.decodeStream(input);
			
			int screenHeight = getHeight() - (2 * verticalMargin);
			int screenWidth = getWidth() - (2 * horizontalMargin);
			
			if (originalBitmap != null) {
				int originalWidth = originalBitmap.getWidth();
				int originalHeight = originalBitmap.getHeight();
				
				//We scale to screen width for the cover or if the image is too wide.
				if (originalWidth > screenWidth || originalHeight > screenHeight || spine.isCover()) {
					float ratio = (float)originalWidth / (float)originalHeight;
					
					int targetHeight = screenHeight - 1;
					int targetWidth = (int)(targetHeight * ratio);					
					
					if (targetWidth > screenWidth - 1) {
						targetWidth = screenWidth - 1;
						targetHeight = (int) (targetWidth * (1 / ratio));
					}				
					
					LOG.debug("Rescaling from " + originalWidth + "x" + originalHeight + " to " + targetWidth + "x" + targetHeight );
					
					if ( targetWidth <= 0 || targetHeight <= 0 ) {
						return null;
					}
					
					return Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
				}
			}
			
			return originalBitmap;
		}
	}
	
	private class ImageTagHandler extends TagNodeHandler {
		@Override
		public void handleTagNode(org.htmlcleaner.TagNode node, SpannableStringBuilder builder,
				int start, int end) {
			
			String src = node.getAttributeByName("src");
			
			if (src == null) {
				src = node.getAttributeByName("href");
			}
			
			if (src == null) {
				src = node.getAttributeByName("xlink:href");
			}
			
			builder.append("\uFFFC");
			
			loader.registerCallback(spine.resolveHref(src), new ImageCallback(builder, start, builder.length()));
		}
	}
}
