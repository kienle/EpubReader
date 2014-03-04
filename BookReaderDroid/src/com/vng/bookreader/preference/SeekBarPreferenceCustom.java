package com.vng.bookreader.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;

import com.hlidskialf.android.preference.SeekBarPreference;

public class SeekBarPreferenceCustom extends SeekBarPreference {
	private int curValue;

	public SeekBarPreferenceCustom(Context context, AttributeSet attrs) {
		super(context,attrs);
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			persistInt(curValue);
			callChangeListener(new Integer(curValue));
		}
	}

	@Override
	public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
		String t = String.valueOf(value);
	    mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
	    
	    curValue = value;
	}
}
