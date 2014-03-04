package com.vng.bookreader.activity;

import com.actionbarsherlock.view.MenuItem;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockPreferenceActivity;
import com.vng.bookreader.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

public class BookReaderPrefsActivity extends RoboSherlockPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.addPreferencesFromResource(R.xml.bookreader_prefs);
		
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) 
			this.finish();
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		this.getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		this.getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		this.updatePreferences(this.getPreferenceScreen());
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		Preference pref = findPreference(key);
		if (pref instanceof ListPreference) {
			pref.setSummary(((ListPreference)pref).getEntry());
		}
		
		this.setResult(RESULT_OK);
	}
	
	private void updatePreferences(PreferenceGroup preferenceGroup) {
		int length = preferenceGroup.getPreferenceCount();
		Preference pref;
		
		for (int i = 0; i < length; i++) {
			pref = preferenceGroup.getPreference(i);
			if (pref instanceof PreferenceCategory || pref instanceof PreferenceScreen) {
				updatePreferences((PreferenceGroup)pref);
			}
			
			if (pref instanceof ListPreference) {
				pref.setSummary(((ListPreference)pref).getEntry());
			}
		}
	}
}
