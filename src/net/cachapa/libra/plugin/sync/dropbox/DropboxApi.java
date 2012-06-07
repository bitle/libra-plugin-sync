package net.cachapa.libra.plugin.sync.dropbox;

import net.cachapa.libra.plugin.sync.R;
import net.cachapa.libra.plugin.sync.util.OAuthApi;
import android.content.Context;

public class DropboxApi extends OAuthApi {
	/* OAuth Credentials */
	private static final String REQUEST_TOKEN_URL = "https://www.dropbox.com/1/oauth/request_token";
	private static final String ACCESS_TOKEN_URL = "https://www.dropbox.com/1/oauth/access_token";
	private static final String AUTHORIZE_URL = "https://www.dropbox.com/1/oauth/authorize";
	private static final String SERVICE_NAME = "dropbox";

	/* API */
	private static final String BASE_URL = "https://api-content.dropbox.com/1";
	private static final String API_CONTENT_URL = "https://api-content.dropbox.com/1";

	private static DropboxApi instance = null;

	protected DropboxApi(Context context) {
		super(context, R.string.dropbox_consumer_key,
				R.string.dropbox_consumer_secret, REQUEST_TOKEN_URL,
				ACCESS_TOKEN_URL, AUTHORIZE_URL, SERVICE_NAME);
	}

	public static DropboxApi getInstance(Context context) {
		if (instance == null) {
			instance = new DropboxApi(context);
		}
		return instance;
	}

	public String upload() throws Exception {
		return put(API_CONTENT_URL + "/files_put/sandbox/database.csv");
	}

	public String download() throws Exception {
		return get(API_CONTENT_URL + "/files/sandbox/database.csv");
	}

	public String isFileChanged() throws Exception {
		return get(BASE_URL + "/metadata/sandbox/database.csv");
	}
}
