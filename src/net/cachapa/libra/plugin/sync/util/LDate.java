/*
 * Copyright 2008-2010 Daniel Cachapa <cachapa@gmail.com>
 * 
 * This program is distributed under the terms of the GNU General Public License Version 3
 * The license can be read in its entirety in the LICENSE.txt file accompanying this source code,
 * or at: http://www.gnu.org/copyleft/gpl.html
 * 
 * This file is part of Libra.
 *
 * WeightWatch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * WeightWatch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the WeightWatch source code. If not, see: http://www.gnu.org/licenses
 */

package net.cachapa.libra.plugin.sync.util;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class LDate {
	private int year, month, day;
	private GregorianCalendar calendar = null;
	
	/***
	 * This constructor is more expensive than the others because it uses Java's GregorianCalendar class, which is slooooow.
	 * No, really. Sloooooooooow.
	 */
	public LDate() {
		calendar = new GregorianCalendar();
		year = calendar.get(Calendar.YEAR);
		month = calendar.get(Calendar.MONTH) + 1;
		day = calendar.get(Calendar.DAY_OF_MONTH);
	}
	
	public LDate(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}
	
	public LDate(int date) {
		this(
			date / 10000,			// Year
			(date % 10000) / 100,	// Month
			date % 100				// Day
		);
	}

	public LDate(LDate date) {
		this(date.getYear(), date.getMonth(), date.getDay());
	}
	
	public LDate(String csvString) throws IllegalArgumentException {
		String[] values = csvString.split("-");
		if (values == null || values.length != 3) {
			throw new IllegalArgumentException("Malformed date string: " + csvString);
		}
		
		Integer year;
		Integer month;
		Integer day;
		try {
			year = Integer.valueOf(values[0]);
			month = Integer.valueOf(values[1]);
			day = Integer.valueOf(values[2]);
			if (year < 1900 || month < 1 || month > 12 || day < 1 || day > 31) {
				// Detects when the date values are in the wrong order
				throw new IllegalArgumentException("Malformed date string: " + csvString);
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Malformed date string: " + csvString);
		}
		
		this.year = year.intValue();
		this.month = month.intValue();
		this.day = day.intValue();
	}
	
	/**
	 * Creates an object from a unix timestamp
	 * @param timestamp unix timestamp in ms
	 */
	public LDate(long timestamp) {
		calendar = new GregorianCalendar();
		calendar.setTimeInMillis(timestamp);
		year = calendar.get(Calendar.YEAR);
		month = calendar.get(Calendar.MONTH) + 1;
		day = calendar.get(Calendar.DAY_OF_MONTH);
	}

	public void setDate(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}
	
	public int toInt() {
		return year * 10000 + month * 100 + day;
	}
	
	public int getYear() {
		return year;
	}
	
	public void setYear(int year) {
		this.year = year;
	}
	
	public int getMonth() {
		return month;
	}
	
	public void setMonth(int month) {
		this.month = month;
	}
	
	public int getDay() {
		return day;
	}
	
	public void setDay(int day) {
		this.day = day;
	}
	
	private GregorianCalendar getCalendar() {
		if (calendar == null) {
			calendar = new GregorianCalendar(year, month-1, day);
		}
		else {
			calendar.set(year, month-1, day, 0, 0, 0);
		}
		return calendar;
	}
	
	private long getTime() {
		return getCalendar().getTimeInMillis();
	}
	
	public long getTimeInSeconds() {
		return getTime()/1000;
	}
	
	/**
	 * Difference in days between two dates. The difference is positive if the 
	 * date given is after the current date, and otherwise negative. 
	 * @param date The date to compare
	 * @return The number of days separating both dates
	 */
	public int differenceInDays(LDate date) {
		// This looks like bad hack, but there's a reason for this: You see, the GregorianCalendar class is very slow,
		// so I'm trying to use is as sparsly as possible. This function is accessed many times when drawing a chart,
		// so it needs to be efficient.
		//
		// So there you go. Homemade calendar class. 
		
		// This method only works for dates going forward, so if they are the other way around, we just flip them
		// and set the result to negative
		if (date.isBefore(this)) {
			return -date.differenceInDays(this);
		}
		
		int tempYear = year;
		int tempMonth = month;
		int diffDays = date.day - day;
		while (tempMonth < date.month || tempYear < date.year) {
			diffDays += getDaysInMonth(tempMonth, tempYear);
			tempMonth++;
			if (tempMonth == 13) {
				tempMonth = 1;
				tempYear++;
			}
		}
		return diffDays;
	}
	
	public void addDays(int days) {
		getCalendar().add(Calendar.DATE, days);
		year = calendar.get(Calendar.YEAR);
		month = calendar.get(Calendar.MONTH) + 1;
		day = calendar.get(Calendar.DAY_OF_MONTH);
	}
	
	public void goBackOneDay() {
		day--;
		if (day < 1) {
			goBackOneMonth();
			day = getDaysInMonth();
		}
	}
	
	public void goForwardOneDay() {
		day++;
		if (day > getDaysInMonth()) {
			goForwardOneMonth();
		}
	}
	
	/**
	 * Subtracts one month from the current date and sets the day to the 1st in order to avoid end-of-month problems
	 */
	public void goBackOneMonth() {
		day = 1;
		month--;
		if (month == 0) {
			month = 12;
			year--;
		}
	}
	
	/**
	 * Advances one month from the current date and sets the day to the 1st in order to avoid end-of-month problems
	 */
	public void goForwardOneMonth() {
		day = 1;
		month++;
		if (month == 13) {
			month = 1;
			year++;
		}
	}
	
	public String getMonthName() {
		return getMonthName(month);
	}
	
	public static String getMonthName(int month) {
		DateFormatSymbols dfs = new DateFormatSymbols();
		return dfs.getMonths()[month-1];
	}
	
	public String getShortMonthName() {
		return getShortMonthName(month);
	}
	
	public static String getShortMonthName(int month) {
		DateFormatSymbols dfs = new DateFormatSymbols();
		return dfs.getShortMonths()[month-1];
	}
	
	public String getWeekdayName() {
		return new DateFormatSymbols().getWeekdays()[getDayOfWeek()];
	}
	
	public String getShortWeekdayName() {
		return getShortWeekdayName(getDayOfWeek());
	}
	
	public static String getShortWeekdayName(int dayOfWeek) {
		DateFormatSymbols dfs = new DateFormatSymbols();
		return dfs.getShortWeekdays()[dayOfWeek];
	}
	
	public static int getDaysInMonth(int month, int year) {
		switch (month) {
			case 4:
			case 6:
			case 9:
			case 11:
				return 30;
			case 2:
				// From http://en.wikipedia.org/wiki/Leap_year#Algorithm
				return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0) ? 29 : 28;
			default:
				return 31;
		}
	}
	
	public int getDaysInMonth() {
		return getDaysInMonth(month, year);
	}
	
	public static int getDaysInYear(int year) {
		return year % 400 == 0 || (year % 4 == 0 && year % 100 != 0) ? 366 : 365;
	}
	
	public int getDayOfWeek() {
		return getCalendar().get(Calendar.DAY_OF_WEEK);
	}
	
	public int getWeek() {
		return getCalendar().get(Calendar.WEEK_OF_YEAR);
	}
	
	/*** Returns a string in the format YYYY-MM-DD ***/
	public String toString() {
		String dateString = String.format("%d-%2d-%2d", year, month, day);
		dateString = dateString.replaceAll(" ", "0");
		return dateString;
	}
	
	/*** Returns a medium string formatted to the current locale, for example, Apr 25, 1974 ***/
	public String toMediumString() {
		return DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(getTime());
	}
	
	/*** Returns a long string formatted to the current locale, for example, April 25, 1974 ***/
	public String toLongString() {
		return DateFormat.getDateInstance(java.text.DateFormat.LONG).format(getTime());
	}
	
	/*** Returns a long string with weekday, formatted to the current locale, for example, Thursday, April 25, 1974 ***/
	public String toExtraLongString() {
		return getWeekdayName() + ", " + toLongString();
	}
	
	public boolean equals(LDate other) {
		if (this.toInt() == other.toInt()) {
			return true;
		}
		else {
			return false;
		}
	}

	public boolean isBefore(LDate startDate) {
		return toInt() < startDate.toInt();
	}

	public int getFirstDayOfWeek() {
		return getCalendar().getFirstDayOfWeek();
	}
	
	public void backToFirstDayOfWeek() {
		int firstDayOfWeek = getCalendar().getFirstDayOfWeek();
		int dayOfWeek = getDayOfWeek();
		if (firstDayOfWeek > dayOfWeek) {
			dayOfWeek += 7;
		}
		addDays(firstDayOfWeek - dayOfWeek);
	}
}
