package com.vng.bookreader.preference;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class ColourChooserPreference extends DialogPreference implements OnAmbilWarnaListener {
	private static final String androidns="http://schemas.android.com/apk/res/android";
	
	protected int defaultColour;

	public ColourChooserPreference(Context context, AttributeSet attributes) {
		super(context, attributes);
		
		this.defaultColour = attributes.getAttributeIntValue(androidns, "defaultValue", Color.BLACK);
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		super.setDefaultValue(defaultValue);
		
		this.defaultColour = (Integer)defaultValue;
	}

	@Override
	public void onCancel(AmbilWarnaDialog dialog) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onOk(AmbilWarnaDialog dialog, int color) {
		this.persistInt(color);
	}
	
	@Override
	protected void onClick() {
		this.getDialog().show();
	}
	
	@Override
	public Dialog getDialog() {
		int colour = this.getPersistedInt(this.defaultColour);
		
		return new AmbilWarnaDialog(this.getContext(), colour, this).getDialog();
	}
}
