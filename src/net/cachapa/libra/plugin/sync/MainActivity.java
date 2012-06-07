package net.cachapa.libra.plugin.sync;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MainActivity extends PreferenceActivity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.main_preferences);

		
		findPreference("DropboxPreference").setIntent(
				new Intent(this, net.cachapa.libra.plugin.sync.DropboxActivity.class));
		findPreference("FitbitPreference").setIntent(
				new Intent(this, net.cachapa.libra.plugin.sync.FitbitActivity.class));
		findPreference("WithingsPreference").setIntent(
				new Intent(this, net.cachapa.libra.plugin.sync.WithingsActivity.class));
	}
}