package net.cachapa.libra.plugin.sync.fitbit;

import net.cachapa.libra.plugin.sync.R;
import net.cachapa.libra.plugin.sync.util.OAuthApi;
import android.content.Context;

public class FitbitApi extends OAuthApi {
	/* OAuth Credentials */
	private static final String REQUEST_TOKEN_URL = "https://api.fitbit.com/oauth/request_token";
	private static final String ACCESS_TOKEN_URL = "https://api.fitbit.com/oauth/access_token";
	private static final String AUTHORIZE_URL = "https://api.fitbit.com/oauth/authorize";
	private static final String SERVICE_NAME = "fitbit";

	/* API */
	private static final String BASE_URL = "http://api.fitbit.com";

	private static FitbitApi instance = null;

	protected FitbitApi(Context context) {
		super(context, R.string.fitbit_consumer_key,
				R.string.dropbox_consumer_secret, REQUEST_TOKEN_URL,
				ACCESS_TOKEN_URL, AUTHORIZE_URL, SERVICE_NAME);
	}

	public static FitbitApi getInstance(Context context) {
		if (instance == null) {
			instance = new FitbitApi(context);
		}
		return instance;
	}

	public String getAllValues() throws Exception {
		return get(BASE_URL + "/1/user/-/body/date/2012-02-19.json");
	}
}
