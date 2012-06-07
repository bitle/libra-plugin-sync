package net.cachapa.libra.plugin.sync.util;

import java.util.LinkedList;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Pair;

public class LibraProvider {
	private static final String URL_VALUES = "content://net.cachapa.libra.provider/values";
	
	private static final String[] valuesProjection = {"_id", "weight", "weightTrend", "bodyFat", "bodyFatTrend", "comment"};

	public static LinkedList<Value> getAllValues(Context context) {
		Cursor cursor = context.getContentResolver().query(
				Uri.parse(URL_VALUES),
				valuesProjection,
				null,
				null,
				null);
		
		LinkedList<Value> values = new LinkedList<Value>();
		Value value;
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			value = new Value(new LDate(cursor.getInt(0)));
			value.weight = cursor.getFloat(1);
			value.weightTrend = cursor.getFloat(2);
			value.bodyFat = cursor.getFloat(3);
			value.bodyFatTrend = cursor.getFloat(4);
			value.comment = cursor.getString(5);
			
			values.add(value);
			cursor.moveToNext();
		}
		cursor.close();
		return values;
	}
	
	public static void insertValues(Context context, String notificationTitle, LinkedList<Value> values) {
		// The content provider only supplies a way to insert one value at a time.
		// This is very slow because the database creates and closes a new transaction for each operation.
		// So we go around this issue by sending all the data at once, encoded in an intent.
		// It's a bit of a hack. If anyone has a better solution, I'd be happy to hear it.
		
		// First we create the necessary arrays
		Pair<int[], float[]> weightData = generateDataArrays(values, TYPE_WEIGHT);
		Pair<int[], float[]> bodyFatData = generateDataArrays(values, TYPE_BODY_FAT);
		Pair<int[], String[]> comments = generateCommentArrays(values);
		
		// Then we put all the data into the intent
		Intent intent = new Intent("net.cachapa.libra.action.INSERT_VALUES");
		intent.putExtra("title", notificationTitle);
		intent.putExtra("weightDates", weightData.first);
		intent.putExtra("weightIndexes", weightData.second);
		intent.putExtra("bodyFatDates", bodyFatData.first);
		intent.putExtra("bodyFatIndexes", bodyFatData.second);
		intent.putExtra("commentDates", comments.first);
		intent.putExtra("comments", comments.second);
		
		// And finally we broadcast the intent. Hope Libra catches it!
		context.sendBroadcast(intent);
	}
	
	private static final int TYPE_WEIGHT = 0;
	private static final int TYPE_BODY_FAT = 1;
	private static Pair<int[], float[]> generateDataArrays(LinkedList<Value> values, int type) {
		int date;
		float index;
		LinkedList<Integer> dates = new LinkedList<Integer>();
		LinkedList<Float> indexes = new LinkedList<Float>();
		for (Value value : values) {
			date = value.date.toInt();
			if (type == TYPE_WEIGHT) {
				index = value.weight;
			} else {
				index = value.bodyFat;
			}
			
			if (index > 0) {
				dates.add(date);
				indexes.add(index);
			}
		}
		
		return new Pair<int[], float[]> (toIntArray(dates), toFloatArray(indexes));
	}
	
	private static Pair<int[], String[]> generateCommentArrays(LinkedList<Value> values) {
		int date;
		String comment;
		LinkedList<Integer> dates = new LinkedList<Integer>();
		LinkedList<String> comments = new LinkedList<String>();
		for (Value value : values) {
			date = value.date.toInt();
			comment = value.comment;
			
			if (comment != null) {
				dates.add(date);
				comments.add(comment);
			}
		}
		
		return new Pair<int[], String[]> (toIntArray(dates), comments.toArray(new String[0]));
	}

	private static int[] toIntArray(LinkedList<Integer> list) {
		int[] ret = new int[list.size()];
		int i = 0;
		for (Integer e : list)
			ret[i++] = e.intValue();
		return ret;
	}
	
	private static float[] toFloatArray(LinkedList<Float> list) {
		float[] ret = new float[list.size()];
		int i = 0;
		for (Float e : list)
			ret[i++] = e.floatValue();
		return ret;
	}
}
