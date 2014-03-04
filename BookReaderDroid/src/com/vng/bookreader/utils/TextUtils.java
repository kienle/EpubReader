package com.vng.bookreader.utils;

public class TextUtils {
	
	public static boolean isBoundaryCharacter(char c) {
		char[] boundaryChars = { ' ', '\n', '\t' };
		
		for (int i = 0; i < boundaryChars.length; i++) {
			if (boundaryChars[i] == c) {
				return true;
			}		
		}
		
		return false;
	}
}
