package com.vng.bookreader.selection;

public class SelectionWord {
	public CharSequence word;
	public int start;
	public int end;

	public SelectionWord(CharSequence word, int start, int end) {
		this.word = word;
		this.start = start;
		this.end = end;
	}

}
