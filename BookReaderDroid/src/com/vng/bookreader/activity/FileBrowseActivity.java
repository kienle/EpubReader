package com.vng.bookreader.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.inject.Inject;
import com.vng.bookreader.Configuration;
import com.vng.bookreader.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import roboguice.activity.RoboListActivity;

public class FileBrowseActivity extends RoboListActivity {
	private FileAdapter adapter;
	
	@Inject
	private Configuration config;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Uri data = this.getIntent().getData();
		
		File file = null;
		
		if (data != null) {
			file = new File(data.getPath());
		}
		
		if (file == null || !(file.exists() && file.isDirectory()) ) {
			file = new File(this.config.getStorageBase());
		}
		
		if (file == null || !(file.exists() && file.isDirectory()) ) {
			file = new File("/");
		}
		
		this.adapter = new FileAdapter();
		this.adapter.setFolder(file);
		
		this.setTitle(adapter.getCurrentFolder());
		
		this.setListAdapter(this.adapter);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File file = this.adapter.getItem(position);
		
		if (file.exists() && file.isDirectory()) {
			this.adapter.setFolder(file);
			this.setTitle(this.adapter.getCurrentFolder());
		}
	}
	
	private class FileAdapter extends BaseAdapter {
		private File currentFolder;
		private List<File> items = new ArrayList<File>();
		
		public void setFolder(File folder) {
			this.currentFolder = folder;
			this.items = new ArrayList<File>();
			
			File[] listing = folder.listFiles();
			
			if (listing != null) {
				for (File childFile : listing) {
					if (childFile.isDirectory() || childFile.getName().toLowerCase().endsWith(".epub")) {
						this.items.add(childFile);
					}
				}
			}
			
			Collections.sort(this.items, new FileSorter());
			
			if (folder.getParentFile() != null) {
				this.items.add(0, folder.getParentFile());
			}
			
			this.notifyDataSetChanged();
		}
		
		public String getCurrentFolder() {
			return this.currentFolder.getAbsolutePath();
		}
		
		@Override
		public int getCount() {
			return this.items.size();
		}
		
		@Override
		public File getItem(int position) {
			return this.items.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView;
			final File file = this.getItem(position);
			
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			if (convertView == null) {
				rowView = inflater.inflate(R.layout.folder_line, parent, false);
			} else {
				rowView = convertView;
			}
			
			ImageView folderIconImageView = (ImageView)rowView.findViewById(R.id.folderIconImageView);
			CheckBox selectCheck = (CheckBox)rowView.findViewById(R.id.selectCheck);
			
			if (file.isDirectory()) {
				folderIconImageView.setImageDrawable(getResources().getDrawable(R.drawable.folder));
				selectCheck.setVisibility(View.VISIBLE);
			} else {
				folderIconImageView.setImageDrawable(getResources().getDrawable(R.drawable.book));
				selectCheck.setVisibility(View.GONE);
			}
			
			selectCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						Intent intent = getIntent();
						intent.setData(Uri.fromFile(file));
						setResult(RESULT_OK, intent);
						finish();
					}
				}
			});
			selectCheck.setFocusable(true);
			
			TextView folderNameText = (TextView)rowView.findViewById(R.id.folderNameText);
			
			if (position == 0 && currentFolder.getParentFile() != null) {
				folderNameText.setText("..");
			} else {
				folderNameText.setText(file.getName());
			}
			
			return rowView;
		}
	}

	private class FileSorter implements Comparator<File> {
		@Override
		public int compare(File lhs, File rhs) {
			if ( (lhs.isDirectory() && rhs.isDirectory()) ||
				 (!lhs.isDirectory() && !rhs.isDirectory()) ) {
				return lhs.getName().compareTo(rhs.getName());
			}
			
			if (lhs.isDirectory()) return -1;
			else return 1;
		}
	}
}
