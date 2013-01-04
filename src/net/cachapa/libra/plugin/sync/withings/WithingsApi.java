package net.cachapa.libra.plugin.sync.withings;

import java.net.URLEncoder;
import java.util.LinkedList;

import oauth.signpost.signature.QueryStringSigningStrategy;

import org.json.JSONArray;
import org.json.JSONObject;

import net.cachapa.libra.plugin.sync.R;
import net.cachapa.libra.plugin.sync.util.CloudManager;
import net.cachapa.libra.plugin.sync.util.LDate;
import net.cachapa.libra.plugin.sync.util.OAuthApi;
import net.cachapa.libra.plugin.sync.util.Value;
import android.content.Context;
import android.net.Uri;

public class WithingsApi extends OAuthApi {
	/* OAuth Credentials */
	private static final String REQUEST_TOKEN_URL = "https://oauth.withings.com/account/request_token";
	private static final String ACCESS_TOKEN_URL = "https://oauth.withings.com/account/access_token";
	private static final String AUTHORIZE_URL = "https://oauth.withings.com/account/authorize";
	private static final String SERVICE_NAME = "withings";
	
	private static final String NOTIFICATION_URL = "http://libra.cachapa.net/notifications";

	private static final String PREF_USER_ID = "userId";
	private static final String PREF_USER_NAME = "userName";
	private static final String PREF_LAST_UPDATE = "lastUpdate";
	private static final String PREF_SUBSCRIPTION_DATE = "subscriptionDate";
	private static final String PREF_SUBSCRIPTION_ID = "subscriptionId";

	/* API */
	private static final String BASE_URL = "http://wbsapi.withings.net";

	private static WithingsApi instance = null;

	protected WithingsApi(Context context) {
		super(context, R.string.withings_consumer_key,
				R.string.withings_consumer_secret, REQUEST_TOKEN_URL,
				ACCESS_TOKEN_URL, AUTHORIZE_URL, SERVICE_NAME);
		
		// We set this so the requests are signed in the url of the request
		// instead of the authorization header because Withings requires the
		// "userid" parameter and I couldn't get it to append there
		setSigningStrategy(new QueryStringSigningStrategy());
	}

	public static WithingsApi getInstance(Context context) {
		if (instance == null) {
			instance = new WithingsApi(context);
		}
		return instance;
	}

	public String getUserName() {
		return loadString(PREF_USER_NAME);
	}
	
	public int getUserId() {
		return loadInt(PREF_USER_ID);
	}

	/*** API Methods ***/
	@Override
	public void completeLogin(Uri uri) throws Exception {
		// We override this method so we can retrieve the user's name and
		// subscribe to notifications immediately after logging in
		super.completeLogin(uri);

		try {
			// Get the user's name
			int userId = Integer.parseInt(uri.getQueryParameter("userid"));
			saveInt(PREF_USER_ID, userId);
			String userName = getUserNameFromApi();
			saveString(PREF_USER_NAME, userName);

			// Create an endpoint for notification subscriptions
			CloudManager.registerGCM(getContext(), CloudManager.SERVICE_WITHINGS);
		} catch (Exception e) {
			// If the name is not retrievable, then cancel the login procedure
			logout();
			throw e;
		}
	}
	
	@Override
	public void logout() {
		super.logout();
		// Reset the subscription preferences
		saveInt(PREF_SUBSCRIPTION_DATE, -1);
		saveLong(PREF_SUBSCRIPTION_ID, -1);
	}

	private String getUserNameFromApi() throws Exception {
		String json = get(BASE_URL + "/user?action=getbyuserid&userid="
				+ loadInt(PREF_USER_ID));
		JSONObject nameObject = new JSONObject(json).getJSONObject("body")
				.getJSONArray("users").getJSONObject(0);
		return nameObject.getString("firstname") + " "
				+ nameObject.getString("lastname");
	}

	public void subscribe(long subscriptionId) throws Exception {
		// Create/update a Withings notification subscription
		String callbackUrl = NOTIFICATION_URL + "/callback_withings.php?id=" + subscriptionId;
		get(BASE_URL
				+ "/notify?action=subscribe"
				+ "&userid=" + loadInt(PREF_USER_ID)
				+ "&callbackurl=" + URLEncoder.encode(callbackUrl, "UTF-8")
				+ "&appli=1"	// Only values from the scale
				+ "&comment="
				+ URLEncoder.encode("Libra Weight Manager for Android", "UTF-8"));

		// The subscription needs to be renewed at most every 3 weeks.
		// We save the current date so we know when it's time to re-subscribe.
		saveInt(PREF_SUBSCRIPTION_DATE, new LDate().toInt());
		saveLong(PREF_SUBSCRIPTION_ID, subscriptionId);
	}
	
	public void updateSubscription() throws Exception {
		// Check if the subscription needs to be updated
		// Subscriptions elapse after 3 weeks. We update them after two weeks, to be safe
		int dateInt = loadInt(PREF_SUBSCRIPTION_DATE);
		if (dateInt < 0) {
			// No subscription is active
			return;
		}
		LDate today = new LDate();
		LDate subscriptionDate = new LDate(dateInt);
		if (subscriptionDate.differenceInDays(today) >= 14) {
			subscribe(loadLong(PREF_SUBSCRIPTION_ID));
		}
	}
	
	public void unsubscribe() throws Exception {
		String callbackUrl = NOTIFICATION_URL + "/callback_withings.php?id=" + loadLong(PREF_SUBSCRIPTION_ID);
		get(BASE_URL
				+ "/notify?action=revoke"
				+ "&userid=" + loadInt(PREF_USER_ID)
				+ "&callbackurl=" + URLEncoder.encode(callbackUrl, "UTF-8"));
	}

	public LinkedList<Value> getAllValues() throws Exception {
		return getValues(null);
	}

	public LinkedList<Value> getNewValues() throws Exception {
		long lastUpdate = loadInt(PREF_LAST_UPDATE);
		LDate lastUpdateDate = null;
		if (lastUpdate > 0) {
			lastUpdateDate = new LDate(lastUpdate * 1000);
		}
		return getValues(lastUpdateDate);
	}

	public LinkedList<Value> getValues(LDate updatedSinceDate) throws Exception {
		String lastUpdateString = null;
		if (updatedSinceDate != null) {
			lastUpdateString = "&lastupdate="
					+ updatedSinceDate.getTimeInSeconds();
		}

		String request = BASE_URL
				+ "/measure?action=getmeas"
				+ "&userid=" + loadInt(PREF_USER_ID)
				+ lastUpdateString + "&devtype=1"; // Only get values from the scale
		String response = get(request);

		// Decode the JSon string
		JSONObject jsonBody = new JSONObject(response).getJSONObject("body");

		// Save the last update time
		int lastUpdate = jsonBody.getInt("updatetime");
		saveInt(PREF_LAST_UPDATE, lastUpdate);

		// Parse the values into a list
		JSONArray JSONvalues = jsonBody.getJSONArray("measuregrps");
		JSONObject JSONvalue, JSONtype;
		JSONArray JSONtypes;
		long timestamp;
		Value value;
		LinkedList<Value> values = new LinkedList<Value>();
		int size = JSONvalues.length();
		for (int i = 0; i < size; i++) {
			JSONvalue = JSONvalues.getJSONObject(i);
			if (JSONvalue.getInt("category") != 1) { // Make sure it's a measurement (=1)
				continue;
			}
			timestamp = JSONvalue.getLong("date");
			value = new Value(new LDate(timestamp * 1000));

			JSONtypes = JSONvalue.getJSONArray("measures");
			for (int j = 0; j < JSONtypes.length(); j++) {
				JSONtype = JSONtypes.getJSONObject(j);
				if (JSONtype.getInt("type") == 1) { // Weight (kg)
					value.weight = (float) (JSONtype.getInt("value") * Math
							.pow(10, JSONtype.getInt("unit")));
				}
				if (JSONtype.getInt("type") == 6) { // Fat Ratio (%)
					value.bodyFat = (float) (JSONtype.getInt("value") * Math
							.pow(10, JSONtype.getInt("unit")));
				}
			}
			if (value.weight > 0 || value.bodyFat > 0) {
				values.add(value);
			}
		}
		return values;
	}
}
