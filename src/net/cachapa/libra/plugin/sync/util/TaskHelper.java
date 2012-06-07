package net.cachapa.libra.plugin.sync.util;

import net.cachapa.libra.plugin.sync.R;
import net.cachapa.libra.plugin.sync.withings.WithingsErrorCodes;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class TaskHelper {
	public static void login(PreferenceActivity activity, OAuthApi api) {
		new LoginTask(activity, api, LoginTask.ACTION_LOGIN).execute();
	}
	
	public static void completeLogin(PreferenceActivity activity, OAuthApi api, String uri) {
		new LoginTask(activity, api, LoginTask.ACTION_COMPLETE_LOGIN).execute(uri);
	}
	
	protected static void showErrorDialog(Context context, String errorMessage) {
		new AlertDialog.Builder(context)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.error)
			.setMessage(WithingsErrorCodes.getMessage(errorMessage))
			.setPositiveButton(R.string.close, null)
			.show();
	}
	
	
	private static class LoginTask extends AsyncTask<String, Void, String> {
		private static final int ERROR = -1;
		private static final int ACTION_LOGIN = 0;
		private static final int ACTION_COMPLETE_LOGIN = 1;
		private static final int ACTION_TEST = 10;
		
		private PreferenceActivity activity;
		private OAuthApi api;
		private int action;
		private Dialog dialog;
		
		public LoginTask(PreferenceActivity activity, OAuthApi api, int action) {
			this.activity = activity;
			this.api = api;
			this.action = action;
		}
		
		@Override
		protected void onPreExecute() {
			switch (action) {
			case ACTION_LOGIN:
				dialog = ProgressDialog.show(activity, null, activity.getString(R.string.redirecting), true, false);
				break;
				
			case ACTION_COMPLETE_LOGIN:
				dialog = ProgressDialog.show(activity, null, activity.getString(R.string.completing_login), true, false);
				break;
			}
		}
		
		@Override
		protected String doInBackground(String... params) {
			try {
				switch (action) {
				case ACTION_LOGIN:
					api.login();
					break;
					
				case ACTION_COMPLETE_LOGIN:
					api.completeLogin(Uri.parse(params[0]));
					break;
					
				case ACTION_TEST:
//					LDate date = new LDate(2012, 03, 28);
//					LinkedList<Value> values = api.getAllValues();
//					LibraProvider.insertValues(WithingsActivity.this, values);
//					CloudManager.registerC2DM(WithingsActivity.this, CloudManager.SERVICE_WITHINGS);
					return null;

				default:
					action = ERROR;
					return "Unrecognized action id: " + action;
				}
			} catch (Exception e) {
				action = ERROR;
				e.printStackTrace();
				return e.getLocalizedMessage();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (dialog != null && dialog.isShowing()) {
				dialog.dismiss();
			}
			
			switch (action) {
			case ACTION_COMPLETE_LOGIN:
				String message = String.format(
						activity.getString(R.string.logged_in_to_service),
						activity.getString(R.string.withings));
				Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
				
				// Update the preference text
				activity.onContentChanged();
				break;

			default:
				showErrorDialog(activity, result);
				break;
			}
		}
	}
}
