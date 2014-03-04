package com.vng.bookreader.library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import android.database.Cursor;

public abstract class KeyedQueryResult<T> extends QueryResult<T> {
	private List<String> keys;
	
	private List<Character> alphabet;
	private String alphabetString;

	public KeyedQueryResult(Cursor cursor, List<String> keys) {
		super(cursor);
		
		this.keys = keys;
		
		this.alphabet = this.calculateAlphabet();
		this.alphabetString = this.calculateAlphabetString();
	}

	public List<String> getKeys() {
		return this.keys;
	}
	
	private List<Character> calculateAlphabet() {
		SortedSet<Character> result = new TreeSet<Character>();
		
		for (String key : this.keys) {
			if (key.length() > 0) {
				result.add(Character.toUpperCase(key.charAt(0)));
			}
		}
		
		return Collections.unmodifiableList(new ArrayList<Character>(result));
	}
	
	private String calculateAlphabetString() {
		StringBuffer buffer = new StringBuffer();
		
		for (Character ch : this.getAlphabet()) {
			buffer.append(ch);
		}
		
		return buffer.toString();
	}
	
	public String getAlphabetString() {
		return this.alphabetString;
	}
	
	public List<Character> getAlphabet() {
		return this.alphabet;
	}
	
	public Character getCharacterFor(int position) {
		String key = this.keys.get(position);
		
		if (key.length() > 0) {
			return Character.toUpperCase(key.charAt(0));
		} else {
			return null;
		}
	}
	
	public int getOffsetFor(Character ch) {
		Character input = Character.toUpperCase(ch);
		
		int size = this.keys.size();
		String key;
		Character keyStart;
		for (int i = 0; i < size; i++) {
			key = this.keys.get(i);
			if (key.length() > 0) {
				keyStart = Character.toUpperCase(key.charAt(0));
				if (keyStart.compareTo(input) >= 0) {
					return i;
				}
			}
		}
		
		return -1;
	}
	
	public T getFirstItemFor(Character ch) {
		int i = this.getOffsetFor(ch);
		
		if (i == -1) {
			return null;
		} else {
			return this.getItemAt(i);
		}
	}
}
