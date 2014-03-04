package com.vng.bookreader.quickaction;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.content.Context;

import com.vng.bookreader.R;
import com.vng.bookreader.activity.LibraryActivity;
import com.vng.bookreader.library.LibraryBook;

public class LibraryQuickAction extends QuickAction implements QuickAction.OnActionItemClickListener {
	public static final int ID_VIEW_BOOK_DETAILS = 1;
	public static final int ID_DELETE_BOOK = 2;
	
	private LibraryActivity libraryActivity;
	private LibraryBook selectedBook;

	public LibraryQuickAction(Context context) {
		super(context);
		
		this.libraryActivity = (LibraryActivity)context;
		
		ActionItem viewDetailsItem = new ActionItem(ID_VIEW_BOOK_DETAILS, context.getString(R.string.qa_details), context.getResources().getDrawable(R.drawable.ic_menu_edit));
		ActionItem deleteItem = new ActionItem(ID_DELETE_BOOK, context.getString(R.string.qa_delete), context.getResources().getDrawable(R.drawable.ic_trash));
		
		addActionItem(viewDetailsItem);
		addActionItem(deleteItem);
		
		this.setOnActionItemClickListener(this);
	}
	
	public void setSelectedBook(LibraryBook selectedBook) {
		this.selectedBook = selectedBook;
	}

	@Override
	public void onItemClick(QuickAction source, int pos, int actionId) {
		switch (actionId) {
			case ID_VIEW_BOOK_DETAILS:
				if (selectedBook != null) {
					this.libraryActivity.showBookDetails(selectedBook);
				}
				break;
			case ID_DELETE_BOOK:
				if (selectedBook != null) {
					this.libraryActivity.doDeleteBook(selectedBook.getFileName());
				}
				break;
		}
	}
}
