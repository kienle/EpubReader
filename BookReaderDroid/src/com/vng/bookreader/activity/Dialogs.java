package com.vng.bookreader.activity;

import com.vng.bookreader.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Html;

public class Dialogs {

	public static void showAboutDialog(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
		builder.setTitle(R.string.menu_about);
		builder.setIcon(R.drawable.ic_launcher);
		
		String version = "";
		try {
			version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			
		}
		
		String html = "<h2>" + context.getString(R.string.app_name) + "</h2>";
		
		html += context.getString(R.string.about);
		
		builder.setMessage(Html.fromHtml(html));
		
		builder.setNeutralButton(context.getString(android.R.string.ok), 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
		});
		
		builder.show();
	}
}
