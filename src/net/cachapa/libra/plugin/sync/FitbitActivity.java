package net.cachapa.libra.plugin.sync;

import net.cachapa.libra.plugin.sync.fitbit.FitbitApi;
import android.os.Bundle;

public class FitbitActivity extends OAuthActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setPreferencesResource(R.xml.fitbit_preferences);
		setApi(FitbitApi.getInstance(this));
		super.onCreate(savedInstanceState);
	}
}