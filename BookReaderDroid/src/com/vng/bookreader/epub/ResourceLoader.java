package com.vng.bookreader.epub;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourceLoader {
	public String fileName;
	
	private List<Holder> callbacks = new ArrayList<ResourceLoader.Holder>();

	public ResourceLoader(String fileName) {
		this.fileName = fileName;
	}
	
	public void clear() {
		this.callbacks.clear();
	}
	
	public void load() throws IOException {
		ZipInputStream in = null;
		
		try {
			in = new ZipInputStream(new FileInputStream(this.fileName));
			
			for (ZipEntry zipEntry = in.getNextEntry(); zipEntry != null; zipEntry = in.getNextEntry()) {
				if (zipEntry.isDirectory()) {
					continue;
				}
				
				String href = zipEntry.getName();
				List<ResourceCallback> filteredCallbacks = findCallbacksFor(href);
				
				if (!filteredCallbacks.isEmpty()) {
					for (ResourceCallback callback : filteredCallbacks) {
						callback.onLoadResource(href, in);
					}
				}
			}
		} finally {
			if (in != null) {
				in.close();
			}
			
			this.callbacks.clear();
		}
	}
	
	private List<ResourceCallback> findCallbacksFor(String href) {
		List<ResourceCallback> result = new ArrayList<ResourceLoader.ResourceCallback>();
		
		for (Holder holder : this.callbacks) {
			if (href.endsWith(holder.href)) {
				result.add(holder.callback);
			}
		}
		
		return result;
	}
	
	public void registerCallback(String forHref, ResourceCallback callback) {
		Holder holder = new Holder();
		holder.href = forHref;
		holder.callback = callback;
		
		callbacks.add(holder);
	}

	public static interface ResourceCallback {
		void onLoadResource(String href, InputStream stream);
	}
	
	public class Holder {
		String href;
		ResourceCallback callback;
	}
}
