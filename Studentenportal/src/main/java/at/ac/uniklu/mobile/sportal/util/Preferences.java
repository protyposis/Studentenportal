/*
 * Copyright (c) 2014 Mario Guggenberger <mario.guggenberger@aau.at>
 *
 * This file is part of AAU Studentenportal.
 *
 * AAU Studentenportal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AAU Studentenportal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AAU Studentenportal.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.ac.uniklu.mobile.sportal.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import at.ac.uniklu.mobile.sportal.R;

public class Preferences {
	
	public static final String PREFERENCE_USER_ID = "userid";
	public static final String PREFERENCE_USERNAME = "username";
	public static final String PREFERENCE_PASSWORD = "password";
	public static final String PREFERENCE_SAVE_PASSWORD = "save_password";
	public static final String PREFERENCE_STW_DISCLAIMER_ACCEPTED = "stw.disclaimer.accepted";
	public static final String PREFERENCE_STW_SELECTED_STOP = "stw.selectedstop";
	
	/**
	 * Preference key that stores a boolean value telling if the time of retrieval of this preference lies within a
	 * period where the phone is / might be muted.
	 */
	public static final String PREFERENCE_MUTING_PERIOD = "mutingservice.mutingperiod";
	public static final String PREFERENCE_LOCATION_STATUS = "mutingservice.locationstatus";
	public static final String PREFERENCE_RINGTONE_OVERRIDE = "mutingutils.ringtoneoverride";
	public static final String PREFERENCE_IGNORE_NEXT_RINGERBROADCAST = "mutingutils.ignorenextringerbroadcast";
	
	public static final String PREFERENCE_VERSIONCHECK_DATE = "versioncheck.date";
	public static final String PREFERENCE_VERSIONCHECK_VERSION = "versioncheck.version";
	
	public static final String PREFERENCE_NOTIFICATIONS_LASTCHECK = "notifications.lastcheckdate";
	
	public static final int LOCATION_STATUS_NONE = 0;
	public static final int LOCATION_STATUS_WAITING = 1;
	public static final int LOCATION_STATUS_RECEIVED = 2;
	
	private static final DateFormat iso8601Format;
	
	static {
		iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
	    iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public static String getUserId(SharedPreferences prefs) {
		return prefs.getString(PREFERENCE_USER_ID, null);
	}
	
	public static void setUserId(Editor editor, String userId) {
		editor.putString(PREFERENCE_USER_ID, userId);
	}
	
	public static void setUserId(SharedPreferences prefs, String userId) {
		prefs.edit().putString(PREFERENCE_USER_ID, userId).commit();
	}
	
	public static String getUsername(SharedPreferences prefs) {
		return prefs.getString(PREFERENCE_USERNAME, null);
	}
	
	public static void setUsername(Editor editor, String username) {
		editor.putString(PREFERENCE_USERNAME, username);
	}
	
	public static void setUsername(SharedPreferences prefs, String username) {
		prefs.edit().putString(PREFERENCE_USERNAME, username).commit();
	}
	
	public static String getPassword(SharedPreferences prefs) {
		String password = prefs.getString(PREFERENCE_PASSWORD, null);
		if(password != null) {
			try {
				return SimpleCrypto.decrypt(SimpleCrypto.SEED, password);
			} catch (Exception e) {}
			return password;
		}
		return null;
	}
	
	public static void setPassword(Editor editor, String password) {
		try {
			password = SimpleCrypto.encrypt(SimpleCrypto.SEED, password);
		} catch (Exception e) {}
		editor.putString(PREFERENCE_PASSWORD, password);
	}
	
	public static void setPassword(SharedPreferences prefs, String password) {
		Editor editor = prefs.edit();
		setPassword(editor, password);
		editor.commit();
	}
	
	public static boolean isSavePassword(SharedPreferences prefs) {
		return prefs.getBoolean(PREFERENCE_SAVE_PASSWORD, false);
	}
	
	public static void setSavePassword(Editor editor, boolean savePassword) {
		editor.putBoolean(PREFERENCE_SAVE_PASSWORD, savePassword);
	}
	
	public static void setSavePassword(SharedPreferences prefs, boolean savePassword) {
		prefs.edit().putBoolean(PREFERENCE_SAVE_PASSWORD, savePassword).commit();
	}
	
	public static boolean isStwDisclaimerAccepted(SharedPreferences prefs) {
		return prefs.getBoolean(PREFERENCE_STW_DISCLAIMER_ACCEPTED, false);
	}
	
	public static void setStwDisclaimerAccepted(Editor editor, boolean accept) {
		editor.putBoolean(PREFERENCE_STW_DISCLAIMER_ACCEPTED, accept);
	}
	
	public static void setStwDisclaimerAccepted(SharedPreferences prefs, boolean accept) {
		prefs.edit().putBoolean(PREFERENCE_STW_DISCLAIMER_ACCEPTED, accept).commit();
	}
	
	public static int getStwSelectedStop(SharedPreferences prefs, int defaultStop) {
		return prefs.getInt(PREFERENCE_STW_SELECTED_STOP, defaultStop);
	}
	
	public static void setStwSelectedStop(Editor editor, int stop) {
		editor.putInt(PREFERENCE_STW_SELECTED_STOP, stop);
	}
	
	public static void setStwSelectedStop(SharedPreferences prefs, int stop) {
		prefs.edit().putInt(PREFERENCE_STW_SELECTED_STOP, stop).commit();
	}

	public static boolean isAutomuteEnabled(Context context, SharedPreferences prefs) {
		return prefs.getBoolean(context.getString(R.string.preference_automute_key), false);
	}
	
	public static void setAutomuteEnabled(Context context, Editor editor, boolean enabled) {
		editor.putBoolean(context.getString(R.string.preference_automute_key), enabled);
	}
	
	public static void setAutomuteEnabled(Context context, SharedPreferences prefs, boolean enabled) {
		prefs.edit().putBoolean(context.getString(R.string.preference_automute_key), enabled).commit();
	}
	
	public static boolean isAutomuteWithoutLocation(Context context, SharedPreferences prefs) {
		return prefs.getBoolean(context.getString(R.string.preference_automute_always_key), false);
	}
	
	public static boolean isMutingPeriod(SharedPreferences prefs) {
		return prefs.getBoolean(PREFERENCE_MUTING_PERIOD, false);
	}
	
	public static void setMutingPeriod(Editor editor, boolean active) {
		editor.putBoolean(PREFERENCE_MUTING_PERIOD, active);
	}
	
	public static void setMutingPeriod(SharedPreferences prefs, boolean active) {
		prefs.edit().putBoolean(PREFERENCE_MUTING_PERIOD, active).commit();
	}
	
	public static int getLocationStatus(SharedPreferences prefs) {
		return prefs.getInt(PREFERENCE_LOCATION_STATUS, LOCATION_STATUS_NONE);
	}
	
	public static void setLocationStatus(Editor editor, int status) {
		editor.putInt(PREFERENCE_LOCATION_STATUS, status);
	}
	
	public static void setLocationStatus(SharedPreferences prefs, int status) {
		prefs.edit().putInt(PREFERENCE_LOCATION_STATUS, status).commit();
	}
	
	public static boolean isRingtoneOverride(SharedPreferences prefs) {
		return prefs.getBoolean(PREFERENCE_RINGTONE_OVERRIDE, false);
	}
	
	public static void setRingtoneOverride(Editor editor, boolean ringtoneOverride) {
		editor.putBoolean(PREFERENCE_RINGTONE_OVERRIDE, ringtoneOverride);
	}
	
	public static void setRingtoneOverride(SharedPreferences prefs, boolean ringtoneOverride) {
		prefs.edit().putBoolean(PREFERENCE_RINGTONE_OVERRIDE, ringtoneOverride).commit();
	}
	
	public static boolean isIgnoreNextRingerbroadcast(SharedPreferences prefs) {
		return prefs.getBoolean(PREFERENCE_IGNORE_NEXT_RINGERBROADCAST, false);
	}
	
	public static void setIgnoreNextRingerbroadcast(Editor editor, boolean ignoreNextBroadcast) {
		editor.putBoolean(PREFERENCE_IGNORE_NEXT_RINGERBROADCAST, ignoreNextBroadcast);
	}
	
	public static void setIgnoreNextRingerbroadcast(SharedPreferences prefs, boolean ignoreNextBroadcast) {
		prefs.edit().putBoolean(PREFERENCE_IGNORE_NEXT_RINGERBROADCAST, ignoreNextBroadcast).commit();
	}
	
	public static String getDebugServerURL(Context context, SharedPreferences prefs) {
		return prefs.getString(context.getString(R.string.preference_debug_serverurl_key), null);
	}
	
	public static int getVersioncheckVersion(SharedPreferences prefs) {
		return prefs.getInt(PREFERENCE_VERSIONCHECK_VERSION, -1);
	}
	
	public static void setVersioncheckVersion(Editor editor, int versionCode) {
		editor.putInt(PREFERENCE_VERSIONCHECK_VERSION, versionCode);
	}
	
	public static Date getVersioncheckDate(SharedPreferences prefs) {
		return new Date(prefs.getLong(PREFERENCE_VERSIONCHECK_VERSION, 0));
	}
	
	public static void setVersioncheckDate(Editor editor, Date date) {
		editor.putLong(PREFERENCE_VERSIONCHECK_VERSION, date.getTime());
	}
	
	public static boolean isNotificationsEnabled(Context context, SharedPreferences prefs) {
		return prefs.getBoolean(context.getString(R.string.preference_notifications_key), true);
	}
	
	public static void setNotificationsEnabled(Context context, Editor editor, boolean enabled) {
		editor.putBoolean(context.getString(R.string.preference_notifications_key), enabled);
	}
	
	public static void setNotificationsEnabled(Context context, SharedPreferences prefs, boolean enabled) {
		prefs.edit().putBoolean(context.getString(R.string.preference_notifications_key), enabled).commit();
	}
	
	public static Date getNotificationsLastCheckDate(Context context, SharedPreferences prefs) {
		String date = prefs.getString(PREFERENCE_NOTIFICATIONS_LASTCHECK, "");
		
		try {
			return iso8601Format.parse(date);
		} catch (ParseException e) {
			return new Date(0);
		}
	}
	
	public static void setNotificationsLastCheckDate(Context context, Editor editor, Date date) {
		editor.putString(PREFERENCE_NOTIFICATIONS_LASTCHECK, iso8601Format.format(date));
	}
	
	public static void setNotificationsLastCheckDate(Context context, SharedPreferences prefs, Date date) {
		prefs.edit().putString(PREFERENCE_NOTIFICATIONS_LASTCHECK, iso8601Format.format(date)).commit();
	}
}
