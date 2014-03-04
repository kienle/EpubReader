package com.vng.bookreader.library;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class QueryResultAdapter<T> extends BaseAdapter {
	QueryResult<T> result;

	public void setResult(QueryResult<T> result) {
		if (this.result != null) this.result.close();
		
		this.result = result;
		
		this.notifyDataSetChanged();
	}

	public void clear() {
		if (this.result != null) this.result.close();
		
		this.result = null;
		this.notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if (this.result == null) return 0;
		
		return this.result.getSize();
	}
	
	@Override
	public Object getItem(int position) {
		if (this.result == null) return null;
		
		return this.result.getItemAt(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getView(position, this.result.getItemAt(position), convertView, parent);
	}
	
	public T getResultAt(int position) {
		if (this.result == null) return null;
		
		return this.result.getItemAt(position);
	}
	
	public abstract View getView(int index, T object, View convertView, ViewGroup parent);
}
