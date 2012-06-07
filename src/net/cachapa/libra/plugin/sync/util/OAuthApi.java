package net.cachapa.libra.plugin.sync.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.signature.SigningStrategy;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

public abstract class OAuthApi {
	public static final int ACTION_ERROR = -1;
	public static final int ACTION_LOGIN = 0;
	public static final int ACTION_COMPLETE_LOGIN = 1;
	
	private static final String CALLBACK_URL_PREFIX = "libra://";
	private static final String PREF_LOGGED_IN = "LoggedIn";
	
	private Context context;
	private String consumerKey;
	private String consumerSecret;
	private String service;
	private SharedPreferences preferences;
	private OAuthConsumer consumer;
	private OAuthProvider provider;
	private SigningStrategy strategy = null;
	
	public OAuthApi(Context context, int consumerKey, int consumerSecret,
			String requestTokenUrl, String accessTokenUrl, String authorizeUrl,
			String service) {
		this.context = context;
		this.consumerKey = context.getString(consumerKey);
		this.consumerSecret = context.getString(consumerSecret);
		this.service = service;

		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		consumer = loadConsumer();
		provider = new CommonsHttpOAuthProvider(requestTokenUrl,
				accessTokenUrl, authorizeUrl);
		provider.setOAuth10a(false);
	}
	
	public void login() throws Exception {
		String token = provider.retrieveRequestToken(consumer, getCallbackUrl());
		context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(token)));
	}

	public void completeLogin(Uri uri) throws Exception {
		String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
		provider.retrieveAccessToken(consumer, verifier);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(service + PREF_LOGGED_IN, true);
		editor.commit();
		saveConsumer();
	}
	
	public void logout() {
		consumer.setTokenWithSecret(null, null);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(service + PREF_LOGGED_IN, false);
		editor.commit();
		saveConsumer();
	}
	
	public boolean isLoggedIn() {
		return preferences.getBoolean(service + PREF_LOGGED_IN, false);
	}
	
	public String getCallbackUrl() {
		return CALLBACK_URL_PREFIX + service;
	}
	
	/* Support */
	protected String get(String url) throws Exception {
		HttpGet get;
		if (strategy == null) {
			// Assume the default strategy
			get = new HttpGet(url);
			consumer.sign(get);
		} else {
			consumer.setSigningStrategy(strategy);
			url = consumer.sign(url);
			get = new HttpGet(url);
		}
		
		// Perform the request
		DefaultHttpClient client = new DefaultHttpClient();
		String result = client.execute(get, new BasicResponseHandler());
		
		// Check the return code
		int statusCode = new JSONObject(result).getInt("status");
		if (statusCode != 0) {
			throw new Exception(String.valueOf(statusCode));
		}
		return result;
	}
	
	protected String put(String url) throws Exception {
		// Create and sign the request
		HttpPut put = new HttpPut(url);
		consumer.sign(put);

		StringEntity entity = new StringEntity("Teste");
		put.setEntity(entity);

		// Get the information from the server
		DefaultHttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(put);
		return response.getStatusLine() + ": "
				+ readResponse(response.getEntity().getContent());
	}
	
	protected void setSigningStrategy(SigningStrategy strategy) {
		this.strategy = strategy;
	}
	
	private static String readResponse(InputStream input) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		StringBuilder buff = new StringBuilder();
		String line = "";
		while ((line = reader.readLine()) != null) {
			buff.append(line);
		}
		return buff.toString();
	}
	
	/*** Preferences ***/
	private void saveConsumer() {
		SharedPreferences.Editor editor = preferences.edit();
		String token = consumer.getToken();
		String secret = consumer.getTokenSecret();
		if (token == null) {
			editor.remove(service + OAuth.OAUTH_TOKEN);
		} else {
			editor.putString(service + OAuth.OAUTH_TOKEN, token);
		}
		if (secret == null) {
			editor.remove(service + OAuth.OAUTH_TOKEN_SECRET);
		} else {
			editor.putString(service + OAuth.OAUTH_TOKEN_SECRET, secret);
		}
		editor.commit();
	}

	private OAuthConsumer loadConsumer() {
		OAuthConsumer c = new CommonsHttpOAuthConsumer(consumerKey,
				consumerSecret);
		String token = preferences.getString(service + OAuth.OAUTH_TOKEN,
				null);
		String secret = preferences.getString(service
				+ OAuth.OAUTH_TOKEN_SECRET, null);
		if (token != null && secret != null) {
			c.setTokenWithSecret(token, secret);
		}
		return c;
	}
	
	protected Context getContext() {
		return context;
	}
	
	protected void saveString(String key, String value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(service + key, value);
		editor.commit();
	}
	
	protected void saveInt(String key, int value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(service + key, value);
		editor.commit();
	}
	
	protected void saveLong(String key, long value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(service + key, value);
		editor.commit();
	}
	
	protected String loadString(String key) {
		return preferences.getString(service + key, null);
	}
	
	protected int loadInt(String key) {
		return preferences.getInt(service + key, -1);
	}
	
	protected long loadLong(String key) {
		return preferences.getLong(service + key, -1);
	}
}