package net.cachapa.libra.plugin.sync.util;

import net.cachapa.libra.plugin.sync.withings.WithingsApi;
import net.cachapa.libra.plugin.sync.withings.WithingsTaskHelper;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class CloudManager extends BroadcastReceiver {
	public static final int SERVICE_WITHINGS = 0;
	
	private static int registeringService = -1;

	public static void registerC2DM(Context context, int service) {
		// HACK: I couldn't figure out how to know who called the registration in onReceive()
		registeringService = service;
		
		Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
		registrationIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
		registrationIntent.putExtra("sender", "libra.android@gmail.com");
		context.startService(registrationIntent);
	}

	public static void unregisterC2DM(Context context, int service) {
		registeringService = service;
		
		Log.d("cloud", "Trying to unregister from C2DM");
		Intent unregIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
		unregIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
		context.startService(unregIntent);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
			handleRegistration(context, intent);
		} else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
			handleMessage(context, intent);
		}
	}

	private void handleRegistration(Context context, Intent intent) {
		String registration = intent.getStringExtra("registration_id");
		if (intent.getStringExtra("error") != null) {
			Log.d("cloud", "Registration failed");
		} else if (intent.getStringExtra("unregistered") != null) {
			Log.d("cloud", "Unregistered from C2DM");
			new UnsubscribeTask(context).execute();
		} else if (registration != null) {
			Log.d("cloud", "Registred C2DM with id: " + registration);
			new RegisterNotificationTask(context, registration).execute();
		}
	}

	private void handleMessage(Context context, Intent intent) {
		Log.d("cloud", "Got a message from C2DM");
		int service = Integer.valueOf(intent.getStringExtra("service")).intValue();
		switch (service) {
		case SERVICE_WITHINGS:
			WithingsApi api = WithingsApi.getInstance(context);
			if (api.isLoggedIn()) {
				Log.d("cloud", "It's from Withings!");
				WithingsTaskHelper.getNewValues(context);
			} else {
				// If we're not logged in to Withings, then we should cancel the
				// notification service
				unregisterC2DM(context, SERVICE_WITHINGS);
			}
			break;
		}
	}

	private static void registerNotificationServer(Context context, String registration) throws Exception {
		// Register the notification on the third-party server and get the notification id
		String request = "http://cachapa.net/libra_notifications/register.php"
				+ "?c2dm_id=" + registration;
		String response = get(request);
		long notificationId = new JSONObject(response).getLong("id");

		switch (registeringService) {
		case SERVICE_WITHINGS:
			WithingsApi.getInstance(context).subscribe(notificationId);
			break;
		}
	}

	public static String get(String url) throws Exception {
		// Perform the request
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		String response = client.execute(get, new BasicResponseHandler());

		Log.d("oauth", response);
		// Decode JSon object
		JSONObject json = new JSONObject(response);
		// Make sure that the request was successful
		if (json.getInt("status") != 0) {
			throw new Exception(json.getString("message"));
		}

		return response;
	}

	// private class BodyEntriesDownloader extends AsyncTask<Long, Integer,
	// String> {
	// private Context context;
	//
	// public BodyEntriesDownloader(Context context) {
	// this.context = context;
	// }
	//
	// protected String doInBackground(Long... params) {
	// long valuesInserted = 0;
	// long startdate = params[0];
	// LinkedList<EntryValue> values;
	// try {
	// WithingsAPI withingsAPI = WithingsAPI.getInstance(context);
	// values = withingsAPI.getValues(new LDate(startdate * 1000));
	// Log.d("cloud", "Got " + values.size() + " values");
	// valuesInserted =
	// Database.getInstance(context).insertManyValues(EntryValue.TYPE_WEIGHT,
	// values);
	// } catch (Exception e) {
	// // Don't show a notification if nothing has been inserted
	// return null;
	// }
	//
	// String description;
	// switch ((int)valuesInserted) {
	// case 0:
	// // Don't show a notification if nothing has been inserted
	// return null;
	// case 1:
	// description = String.format(context
	// .getString(R.string.withings_notification_one_value), UnitManager
	// .getInstance(context).toWeightUnit(values.getLast().value));
	// break;
	// default:
	// description = String.format(context
	// .getString(R.string.withings_notification_many_values), valuesInserted);
	// }
	//
	// return description;
	// }
	//
	// protected void onPostExecute(String description) {
	// if (description == null) {
	// // Don't show a notification if nothing has been inserted
	// Log.d("cloud", "Nothing to insert");
	// return;
	// }
	// Notification notification = new
	// Notification(R.drawable.notification_icon,
	// description, System.currentTimeMillis());
	// notification.flags |= Notification.FLAG_AUTO_CANCEL;
	//
	// Intent notificationIntent = new Intent(context, Main.class);
	// PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
	// notificationIntent, 0);
	//
	// notification.setLatestEventInfo(context,
	// context.getString(R.string.withings_notification_title), description,
	// contentIntent);
	//
	// String ns = Context.NOTIFICATION_SERVICE;
	// NotificationManager notificationManager = (NotificationManager)
	// context.getSystemService(ns);
	// notificationManager.notify(0, notification);
	// }
	// }

	private static class RegisterNotificationTask extends AsyncTask<Void, Void, String> {
		private Context context;
		private String registration;

		public RegisterNotificationTask(Context context, String registration) {
			this.context = context;
			this.registration = registration;
		}

		protected String doInBackground(Void... params) {
			try {
				registerNotificationServer(context, registration);

			} catch (Exception e) {
				String error = e.getLocalizedMessage();
				e.printStackTrace();
				return error;
			}
			return null;
		}

		protected void onPostExecute(String error) {
			if (error != null) {
				Toast.makeText(context, error, Toast.LENGTH_LONG).show();
			}
		}
	}

	private static class UnsubscribeTask extends AsyncTask<Void, Void, Void> {
		private Context context;

		public UnsubscribeTask(Context context) {
			this.context = context;
		}

		protected Void doInBackground(Void... params) {
			try {
				switch (registeringService) {
				case SERVICE_WITHINGS:
					// Attempt to unsubscribe. If it doesn't work, no big deal:
					// the subscription elapses after three weeks
					WithingsApi.getInstance(context).unsubscribe();
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
