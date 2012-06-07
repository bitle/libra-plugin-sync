package net.cachapa.libra.plugin.sync.withings;

import java.util.LinkedList;

import net.cachapa.libra.plugin.sync.R;
import net.cachapa.libra.plugin.sync.util.LibraProvider;
import net.cachapa.libra.plugin.sync.util.TaskHelper;
import net.cachapa.libra.plugin.sync.util.Value;
import android.content.Context;
import android.os.AsyncTask;

public class WithingsTaskHelper extends TaskHelper {
	public static void getAllValues(Context context) {
		new NetworkTask(context, WithingsApi.getInstance(context)).execute(NetworkTask.ACTION_GET_ALL);
	}
	
	public static void getNewValues(Context context) {
		new NetworkTask(context, WithingsApi.getInstance(context)).execute(NetworkTask.ACTION_GET_NEW);
	}
	
	
	private static class NetworkTask extends AsyncTask<Integer, Void, String> {
		private static final int ACTION_GET_ALL = 0;
		private static final int ACTION_GET_NEW = 1;
		
		private Context context;
		private WithingsApi api;
		
		public NetworkTask(Context context, WithingsApi api) {
			this.context = context;
			this.api = api;
		}
		
		@Override
		protected String doInBackground(Integer... params) {
			try {
				LinkedList<Value> values = null;
				switch (params[0]) {
				case ACTION_GET_ALL:
					values = api.getAllValues();
					break;
				case ACTION_GET_NEW:
					values = api.getNewValues();
					break;
				}
				LibraProvider.insertValues(context, context.getString(R.string.withings_notification_title), values);
				
				// Update the subscription (if necessary)
				api.updateSubscription();
			} catch (Exception e) {
				e.printStackTrace();
				return e.getLocalizedMessage();
			}
			return null;
		}
	}
}
