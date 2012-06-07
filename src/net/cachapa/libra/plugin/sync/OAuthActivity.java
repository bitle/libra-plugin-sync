package net.cachapa.libra.plugin.sync;

import net.cachapa.libra.plugin.sync.util.OAuthApi;
import net.cachapa.libra.plugin.sync.util.TaskHelper;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class OAuthActivity extends PreferenceActivity implements OnPreferenceClickListener {
	private int preferencesResId;
	private OAuthApi api;
	private Preference loginPreference;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(preferencesResId);
		
		loginPreference = findPreference("LoginPreference");
		loginPreference.setOnPreferenceClickListener(this);
	}
	
	protected void setPreferencesResource(int preferencesResId) {
		this.preferencesResId = preferencesResId;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Check if we're coming back from the provider's login page
		Uri uri = getIntent().getData();
		if (uri != null && uri.toString().startsWith(api.getCallbackUrl()) && !api.isLoggedIn()) {
			// This means we're successfully logged in
			// All that's left is to complete the login procedure
			TaskHelper.completeLogin(this, api, uri.toString());
		}
		
		onContentChanged();
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == loginPreference) {
			if (api.isLoggedIn()) {
				api.logout();
				onContentChanged();
			} else {
				TaskHelper.login(this, api);
			}
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onContentChanged() {
		super.onContentChanged();
		
		// Update login item summaries
		if (api == null || loginPreference == null) {
			return;
		}
		if (api.isLoggedIn()) {
			loginPreference.setTitle(R.string.log_out);
			loginPreference.setSummary(R.string.logged_in);
		} else {
			loginPreference.setTitle(R.string.log_in);
			loginPreference.setSummary(null);
		}
	}
	
	protected void setApi(OAuthApi api) {
		this.api = api;
	}
	
	protected OAuthApi getApi() {
		return api;
	}
	
	protected Preference getLoginPreference() {
		return loginPreference;
	}
}