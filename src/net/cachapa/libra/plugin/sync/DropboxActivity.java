package net.cachapa.libra.plugin.sync;

import net.cachapa.libra.plugin.sync.dropbox.DropboxApi;
import android.os.Bundle;

public class DropboxActivity extends OAuthActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setPreferencesResource(R.xml.dropbox_preferences);
		setApi(DropboxApi.getInstance(this));
		super.onCreate(savedInstanceState);
	}
}