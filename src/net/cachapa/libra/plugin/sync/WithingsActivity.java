package net.cachapa.libra.plugin.sync;

import net.cachapa.libra.plugin.sync.withings.WithingsApi;
import net.cachapa.libra.plugin.sync.withings.WithingsTaskHelper;
import android.os.Bundle;
import android.preference.Preference;

public class WithingsActivity extends OAuthActivity {
	private Preference getAllPreference;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setPreferencesResource(R.xml.withings_preferences);
		setApi(WithingsApi.getInstance(this));
		super.onCreate(savedInstanceState);
		
		getAllPreference = findPreference("GetAllPreference");
		getAllPreference.setOnPreferenceClickListener(this);
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == getAllPreference) {
			WithingsTaskHelper.getAllValues(this);
		}
		return super.onPreferenceClick(preference);
	}
	
	@Override
	public void onContentChanged() {
		super.onContentChanged();
		
		// Update login item summaries
		WithingsApi api = (WithingsApi) getApi();
		Preference loginPreference = getLoginPreference();
		if (api == null || loginPreference == null) {
			return;
		}
		if (api.isLoggedIn()) {
			String message = String.format(getString(R.string.logged_in_as_user), api.getUserName());
			getLoginPreference().setSummary(message);
		}
	}
}