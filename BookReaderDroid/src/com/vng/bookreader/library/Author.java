package com.vng.bookreader.library;

import java.io.Serializable;

public class Author implements Serializable {
	private static final long serialVersionUID = -9027442126212861173L;
	
	private String firstName;
	private String lastName;
	
	private String authorKey;

	public Author(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
		
		this.authorKey = firstName.toLowerCase() + "_" + lastName.toLowerCase();
	}

	public String getAuthorKey() {
		return this.authorKey;
	}
	
	public String getFirstName() {
		return this.firstName;
	}
	
	public String getLastName() {
		return this.lastName;
	}
}
