package com.vng.bookreader.epub;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;

public class PageTurnerSpine {
	private List<SpineEntry> entries;
	
	private int position;
	
	public static final String COVER_HREF = "ZingBookCover";

	/** How long should a cover page be to be included **/
	private static final int COVER_PAGE_THRESHOLD = 1024;
	
	private String tocHref;
	
	public PageTurnerSpine(Book book) {
		this.entries = new ArrayList<PageTurnerSpine.SpineEntry>();
		this.position = 0;
		
		addResource(createCoverResource(book));
		
		String href = null;
		if (book.getCoverPage() != null && book.getCoverPage().getSize() <= COVER_PAGE_THRESHOLD) {
			href = book.getCoverPage().getHref();
		}
		
		for (int i = 0; i < book.getSpine().size(); i++) {
			Resource res = book.getSpine().getResource(i);
			
			if (href == null || !(href.equals(res.getHref())) ) {
				addResource(res);
			}
		}
		
		if (book.getNcxResource() != null) {
			this.tocHref = book.getNcxResource().getHref();
		}
	}
	
	/**
	 * Adds a new resource.
	 * @param resource
	 */
	private void addResource(Resource resource) {
		SpineEntry newEntry = new SpineEntry();
		newEntry.title = resource.getTitle();
		newEntry.resource = resource;
		newEntry.href = resource.getHref();
		newEntry.size = (int)resource.getSize();
		
		entries.add(newEntry);
	}
	
	/**
	 * Returns the number of entries in this spine.
	 * This includes the generated cover.
	 * 
	 * @return
	 */
	public int size() {
		return this.entries.size();
	}
	
	/**
	 * Navigates one entry forward.
	 * 
	 * @return false if we're already at the end.
	 */
	public boolean navigateForward() {
		
		if ( this.position == size() - 1 ) {
			return false;
		}
		
		this.position++;
		return true;				
	}
	
	/**
	 * Navigates one entry back.
	 * 
	 * @return false if we're already at the start
	 */
	public boolean navigateBack() {
		if ( this.position == 0 ) {
			return false;
		}
		
		this.position--;
		return true;
	}
	
	/**
	 * Checks if the current entry is the cover page.
	 * 
	 * @return
	 */
	public boolean isCover() {
		return this.position == 0;
	}
	
	/**
	 * Returns the title of the current entry,
	 * or null if it could not be determined.
	 * 
	 * @return
	 */
	public String getCurrentTitle() {
		if ( entries.isEmpty() ) {
			return null;
		}
		
		return entries.get(position).title;
	}
	
	/**
	 * Returns the current resource, or null
	 * if there is none.
	 * 
	 * @return
	 */
	public Resource getCurrentResource() {
		if ( this.entries.isEmpty() ) {
			return null;
		}
		
		return this.entries.get(this.position).resource;
	}
	
	/**
	 * Resolves a href relative to the current resource.
	 * 
	 * @param href
	 * @return
	 */
	public String resolveHref(String href) {
		Resource res = getCurrentResource();
		
		if ( res == null || res.getHref() == null ) {
			return href;
		}
		
		return resolveHref(href, res.getHref());
	}
	
	/**
	 * Resolves a HREF relative to the Table of Contents
	 * 
	 * @param href
	 * @return
	 */
	 public String resolveTocHref(String href) {
		if (this.tocHref != null) {
			return resolveHref(href, this.tocHref);
		}
		
		return href;
	}
	
	private static String resolveHref(String href, String against) {
		try {
			String result = new URI(encode(against)).resolve(encode(href)).getPath();
			
			return result;
		} catch (URISyntaxException e) {
			return href;
		}
	}
	
	private static String encode(String input) {
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (isUnsafe(ch)) {
                resultStr.append('%');
                resultStr.append(toHex(ch / 16));
                resultStr.append(toHex(ch % 16));
            } else {
                resultStr.append(ch);
            }
        }
        return resultStr.toString();
    }

    private static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }
    
    /**
     * This is slightly unsafe: it lets / and % pass, making 
     * multiple encodes safe.
     * @param ch
     * @return
     */
    private static boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0)
            return true;
        return " %$&+,:;=?@<>#[]".indexOf(ch) >= 0;
    }
    
    /**
	 * Returns the href of the current resource.
	 * @return
	 */
	public String getCurrentHref() {
		if ( entries.isEmpty() ) {
			return null;
		}
		
		return entries.get(position).href;
	}
    
    /**
	 * Navigates to a specific point in the spine.
	 * 
	 * @param index
	 * @return false if the point did not exist.
	 */
	public boolean navigateByIndex( int index ) {
		if ( index < 0 || index >= size() ) {
			return false;
		}
		
		this.position = index;
		return true;
	}
    
    /**
	 * Returns the current position in the spine.
	 * 
	 * @return
	 */
	public int getPosition() {
		return this.position;
	}
	
    /**
	 * Navigates to the point with the given href.
	 * 
	 * @param href
	 * @return false if that point did not exist.
	 */
	public boolean navigateByHref(String href) {
		String encodedHref = encode(href);
		
		for ( int i=0; i < size(); i++ ) {
			String entryHref = encode(entries.get(i).href);
			if (entryHref.equals(encodedHref)) {
				this.position = i;
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns a percentage, which indicates how
	 * far the given point in the current entry is
	 * compared to the whole book.
	 * 
	 * @param progressInPart
	 * @return
	 */
	public int getProgressPercentage(double progressInPart) {
		if (this.entries == null) {
			return -1;
		}
		
		double upToHere = 0;
		
		List<Double> percentages = getRelativeSize();
		
		for (int i = 0; i < percentages.size() && i < this.position; i++) {
			upToHere += percentages.get(i);
		}
		
		double thisPart = percentages.get(this.position);
		double progress = upToHere + (progressInPart * thisPart);
		
		return (int)(progress * 100);
	}
	
	/**
	 * Returns a list of doubles representing the relative size of each spine index.
	 * @return
	 */
	public List<Double> getRelativeSize() {
		int total = 0;
		List<Integer> sizes = new ArrayList<Integer>();
		
		for (int i = 0; i < entries.size(); i++) {
			int size = entries.get(i).size;
			sizes.add(size);
			total += size;
		}
		
		List<Double> result = new ArrayList<Double>();
		for (int i = 0; i < sizes.size(); i++) {
			double part = (double)sizes.get(i) / (double) total;
			result.add(part);
		}
		
		return result;
	}
	
	private Resource createCoverResource(Book book) {
		if (book.getCoverImage() != null && book.getCoverImage().getSize() > 0) {
			return book.getCoverPage();
		}
		
		Resource res = new Resource(generateCoverPage(book).getBytes(), COVER_HREF);
		res.setTitle("Cover");
		
		return res;
	}
	
	private String generateCoverPage(Book book) {
		String centerPiece;
		
		if (book.getCoverImage() == null) {
			centerPiece = "<h1>" + (book.getTitle() != null ? book.getTitle() : "Book without a title") +"</h1>";
			
			if (!book.getMetadata().getAuthors().isEmpty()) {
				for (Author author : book.getMetadata().getAuthors()) {
					centerPiece += "<h3>" + author.getFirstname() + " " + author.getLastname() + "</h3>";
				}
			} else {
				centerPiece += "<h3>Unkown author</h3>";
			}
		} else {
			centerPiece = "<img src='" + book.getCoverImage().getHref() + "'>";
		}
		
		return "<html><body>" + centerPiece + "</body></html>"; 
	}

	private class SpineEntry {
		private String title;
		private Resource resource;
		private String href;
		
		private int size;
	}
}
