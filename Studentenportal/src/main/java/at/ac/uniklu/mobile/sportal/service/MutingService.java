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

package at.ac.uniklu.mobile.sportal.service;

import java.util.Date;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.Studentportal;
import at.ac.uniklu.mobile.sportal.LoginActivity;
import at.ac.uniklu.mobile.sportal.R;
import at.ac.uniklu.mobile.sportal.persistence.MutingPeriod;
import at.ac.uniklu.mobile.sportal.persistence.StudentPortalDB;
import at.ac.uniklu.mobile.sportal.service.MutingUtils.MutingRegion;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.Preferences;

/**
 * This service manages the automute feature that mutes the phone while a course is ongoing.
 * TODO The code is a total mess and needs some serious refactoring.
 * 
 * cases:
 *  - SERVICE TURN ON: 
 *  	- take a currently running or the next upcoming event and schedule it
 *  	- if there are more events at the same time, take any one
 *  - MUTE:
 *  	- mute the phone if prerequisites are satisfied (location, preferences, etc...)
 *  	  and schedule unmute at the end of the event
 *  	- if prerequisites aren't satisfied, schedule next upcoming event
 *  - UNMUTE:
 *  	- look at other currently running but not yet finished events and schedule mute
 *  	- if no other running events found, unmute phone and schedule next upcoming event
 *  - SERVICE TURN OFF:
 *  	- if phone is muted by the service, unmute it
 *  	- if an upcoming alarm is scheduled, cancel it
 */
public class MutingService extends IntentService {
	
	public static final String ACTION = "mutingservice.action";
	public static final int ACTION_NONE = 0;
	public static final int ACTION_TURN_ON = 1;
	public static final int ACTION_TURN_OFF = 2;
	public static final int ACTION_MUTE = 3;
	public static final int ACTION_UNMUTE = 4;
	public static final int ACTION_SCHEDULE = 5;
	public static final int ACTION_REQUEST_ISRUNNING = 20;
	
	public static final String ACTION_RESPONSE_ISRUNNING = "response_running";
	
	public static final String ACTION_RESPONSE_SHUTDOWN = "response_shutdown";
	public static final int ACTION_RESPONSE_SHUTDOWN_NO_UPCOMING_DATES = 1;
	
	public static final String ACTION_RESPONSE_MESSAGE = "response_message";
	
	private static final String TAG = "MutingService";
	private static final boolean DEBUG_WITH_FAKE_ALARMS = false;
	public static final String EXTRA_ALARM_ID = "mutingservice.extraalarmid";

	public MutingService() {
		super("MutingService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			Analytics.onServiceStart(this);
			switch(intent.getExtras().getInt(ACTION, ACTION_NONE)) {
			case ACTION_TURN_ON:
				turnOn();
				break;
			case ACTION_TURN_OFF:
				turnOff();
				break;
			case ACTION_MUTE:
				mute(intent.getExtras().getInt(EXTRA_ALARM_ID));
				break;
			case ACTION_UNMUTE:
				unmute();
				break;
			case ACTION_SCHEDULE:
				if(isRunning()) scheduleMute();
				break;
			case ACTION_REQUEST_ISRUNNING:
				ServiceToActivityBroadcastReceiver.sendBroadcast(this, new Intent().putExtra(ACTION_RESPONSE_ISRUNNING, isRunning()));
				break;
			}
			Analytics.onServiceStop(this);	
		} finally {
			MutingServiceWakeLock.releaseWakeLock();
		}
	}
	
	/**
	 * Turns ON automatic ringtone muting during courses.
	 */
	private void turnOn() {
		Log.d(TAG, "turnOn()");
		
		if(isRunning()) {
			Log.d(TAG, "service is already running, cancelling TURN ON");
			return;
		}
		
		Analytics.onEvent(Analytics.EVENT_MUTINGSERVICE_ON);
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if(!Preferences.isAutomuteEnabled(this, preferences)) {
			Log.d(TAG, "Cancelling automute turn on. User wants it to be off.");
			return;
		}
		
		// reset the preference for the case of a crash where unmute() or turnOff() has never been called
		if(Preferences.isMutingPeriod(preferences)) {
			Preferences.setMutingPeriod(preferences, false);
		}
		
		scheduleMute();
	}
	
	/**
	 * Determines if the muting service is running (ON) or stopped (OFF).
	 * The muting service is defined as running if there is an alarm scheduled that will either mute or unmute 
	 * the phone at a specific time. It is stopped if no alarm is scheduled.
	 * http://code.google.com/p/android/issues/detail?id=3776
	 * http://stackoverflow.com/questions/2110620/how-to-handle-an-alarm-triggered-each-day-in-android
	 * @return true if the muting service is running or false if it is stopped
	 */
	private boolean isRunning() {
		Intent alarmIntent = new Intent(this, OnAlarmReceiver.class).putExtra(ACTION, ACTION_NONE);
		boolean isRunning = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_NO_CREATE) != null;
		Log.d(TAG, "isRunning(): " + isRunning);
		return isRunning;
	}
	
	/**
	 * Turns OFF automatic ringtone muting during courses.
	 */
	private void turnOff() {
		Log.d(TAG, "turnOff()");
		Analytics.onEvent(Analytics.EVENT_MUTINGSERVICE_OFF);
		
		// if the phone is currently in a muting period, turn the ringtone back on before turning off the service
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if(Preferences.isMutingPeriod(preferences)) {
			MutingUtils.ringtoneTurnOn(this);
			Preferences.setMutingPeriod(preferences, false);
		}
		
		// remove eventually existing user notification
		removeNotification(Studentportal.NOTIFICATION_MS_INFO);
		
		// cancel an eventually existing pending alarm and the beloging intent as well (otherwise isRunning would always return true)
		AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		Intent alarmIntent = new Intent(this, OnAlarmReceiver.class).putExtra(ACTION, ACTION_NONE);
		PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.cancel(pendingAlarmIntent);
		pendingAlarmIntent.cancel();
		
		// cancel an eventually exisiting location broadcast receiver
		LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		Intent locationIntent = new Intent("at.ac.uniklu.mobile.sportal.LOCATION_UPDATE");
		PendingIntent pendingLocationIntent = PendingIntent.getBroadcast(this, 0, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		locationManager.removeUpdates(pendingLocationIntent);
		pendingLocationIntent.cancel();
		
		if(isRunning()) {
			Log.e(TAG, "COULD NOT TURN OFF");
		}
	}
	
	/**
	 * Schedules an alarm through the AlarmManager. Alarms are typically scheduled at time when courses begin or end.
	 * @param time the time at which the alarm will go off
	 * @param action the action that will be called when the alarm goes off
	 */
	private void scheduleAlarm(long time, int action, int alarmId) {
		Log.d(TAG, "scheduling alarm action " + action + " @ " + new Date(time).toLocaleString() + " (aID:" + alarmId + ")");
		
		AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		Intent alarmIntent = new Intent(this, OnAlarmReceiver.class).putExtra(ACTION, action).putExtra(EXTRA_ALARM_ID, alarmId);
		PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		time = SystemClock.elapsedRealtime() + (time - System.currentTimeMillis()); //  convert unixtime to system runtime
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, pendingAlarmIntent);
	}
	
	private void scheduleMute() {
		scheduleMute(false);
	}
	
	private void scheduleMute(boolean next) {
		Log.d(TAG, "scheduleMute(next:" + next + ")");
		Analytics.onEvent(Analytics.EVENT_MUTINGSERVICE_MUTE_SCHEDULE);
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		// determine next muting time
		MutingPeriod mutingPeriod;
		if(DEBUG_WITH_FAKE_ALARMS) {
			mutingPeriod = new MutingPeriod();
			mutingPeriod.setId(-1);
			mutingPeriod.setBegin(System.currentTimeMillis() + 10000);
			mutingPeriod.setEnd(mutingPeriod.getBegin() + 10000);
			mutingPeriod.setName("Fakeevent");
		} else {
			StudentPortalDB db = Studentportal.getStudentPortalDB();
			
			if(Preferences.isMutingPeriod(preferences) || next) {
				// we are currently in a muting period, so schedule the next period
				mutingPeriod = db.mutingPeriods_getNextPeriod();
			} else {
				mutingPeriod = db.mutingPeriods_getCurrentPeriod();
				if(mutingPeriod == null) mutingPeriod = db.mutingPeriods_getNextPeriod();
			}
			
			if(mutingPeriod == null) {
				// no upcoming alarm stored in the DB, try to fetch new ones from the server
				Log.d(TAG, "muting DB empty, fetching new ones from the server...");
				try {
					if(!Studentportal.getSportalClient().login().isLoggedIn()) {
						Studentportal.getSportalClient().login(
								Preferences.getUsername(preferences), 
								Preferences.getPassword(preferences),
								null);
					}
					db.mutingPeriods_insertNew(Studentportal.getSportalClient().getTermine(null, null, 10, null));
					db.mutingPeriods_cleanup();
				}
				catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
					Analytics.onError(Analytics.ERROR_MUTINGSERVICE_ONLINEUPDATE, e);
					// notify user that there's a server connection problem
					notifyUser(Studentportal.NOTIFICATION_MS_ERROR, false, getString(R.string.app_name), getString(R.string.app_name), 
							getString(R.string.automute_notification_onlineupdate_failed), android.R.drawable.stat_notify_error,
							new Intent(this, LoginActivity.class));
				}
				if(!(Preferences.isMutingPeriod(preferences) || next)) {
					mutingPeriod = db.mutingPeriods_getCurrentPeriod();
				}
				if(mutingPeriod == null) {
					mutingPeriod = db.mutingPeriods_getNextPeriod();
				}
			}
		}
		
		boolean isUpcomingMutingDateExisting = (mutingPeriod != null);
		
		if(isUpcomingMutingDateExisting) {
			scheduleAlarm(mutingPeriod.getBegin(), ACTION_MUTE, mutingPeriod.getId());
			Log.d(TAG, "scheduled: " + mutingPeriod.getName() + " @ " + new Date(mutingPeriod.getBegin()).toLocaleString() + 
					" (until " + new Date(mutingPeriod.getEnd()).toLocaleString() + " / id: " + mutingPeriod.getId() + ")");
			// TODO notify user about next scheduled muting
		}
		else {
			// inform user via a notification that no upcoming dates were found to be muted and turn OFF
//			notifyUser(NOTIFICATION_ERROR, Notification.FLAG_AUTO_CANCEL, getString(R.string.app_name), getString(R.string.app_name), 
//					getString(R.string.automute_notification_disabled_no_courses));
			
			// set the automute preference to false so that the automute indicator corresponds with the preferences screen
			Preferences.setAutomuteEnabled(this, preferences, false);
			
			// notify listening activities about the shutdown
			ServiceToActivityBroadcastReceiver.sendBroadcast(this, new Intent()
					.putExtra(ACTION_RESPONSE_SHUTDOWN, ACTION_RESPONSE_SHUTDOWN_NO_UPCOMING_DATES)
					.putExtra(ACTION_RESPONSE_MESSAGE, getString(R.string.automute_notification_disabled_no_courses)));
		}
	}
	
	private void scheduleUnmute(long time) {
		Log.d(TAG, "scheduleUnmute()");
		Analytics.onEvent(Analytics.EVENT_MUTINGSERVICE_UNMUTE_SCHEDULE);
		
		if(DEBUG_WITH_FAKE_ALARMS) {
			scheduleAlarm(time, ACTION_UNMUTE, -1);
		} else {
			// determine next unmuting time
			// if there's another course starting before the current one is finished, schedule another mute instead
			StudentPortalDB db = Studentportal.getStudentPortalDB();
			MutingPeriod nextMutingPeriod = db.mutingPeriods_getNextPeriod();
			if(!DEBUG_WITH_FAKE_ALARMS && nextMutingPeriod != null && time > nextMutingPeriod.getBegin()) {
				Log.d(TAG, "overlap detected, turning unmute into another mute (" + 
						new Date(time).toLocaleString() + " overlaps with begin time " + 
						new Date(nextMutingPeriod.getBegin()).toLocaleString() + " / id: " + nextMutingPeriod.getId() + ")");
				scheduleMute();
			} else {
				scheduleAlarm(time, ACTION_UNMUTE, -1);
			}
		}
	}
	
	private void mute(int alarmId) {
		Log.d(TAG, "mute()");
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		if(Preferences.getLocationStatus(preferences) == Preferences.LOCATION_STATUS_WAITING) {
			Log.d(TAG, "mute() blocked - waiting for a location update");
			return;
		}
		
		// check if phone is already muted by the user
		AudioManager audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
		boolean isPhoneAlreadyMuted = audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL 
				&& !Preferences.isMutingPeriod(preferences);
		if(isPhoneAlreadyMuted) {
			Log.d(TAG, "phone is already muted, scheduling next mute");
			scheduleMute(true);
			return;
		}
		
		// load the current period from the db
		MutingPeriod mutingPeriod = null;
		Log.d(TAG, "muting period id: " + alarmId);
		if(DEBUG_WITH_FAKE_ALARMS) {
			mutingPeriod = new MutingPeriod();
			mutingPeriod.setId(-1);
			mutingPeriod.setBegin(System.currentTimeMillis());
			mutingPeriod.setEnd(mutingPeriod.getBegin() + 10000);
			mutingPeriod.setName("Fakeevent");
		} else {
			mutingPeriod = Studentportal.getStudentPortalDB().mutingPeriods_getPeriod(alarmId);
		}
		
		// check if phone is located at university
		notifyUser(Studentportal.NOTIFICATION_MS_INFO, true, mutingPeriod.getName(), 
				mutingPeriod.getName(), getString(R.string.automute_notification_course_started_locating));
		boolean isPhoneLocationKnown = false;
		boolean isPhoneLocatedAtUniversity = false;
		String locationSource = null;
		
		WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
		if(wifiManager.isWifiEnabled()) {
			ScanResult scanResult = MutingUtils.findMutingWifiNetwork(wifiManager.getScanResults());
			if(scanResult != null) {
				Log.d(TAG, "phone located by wifi: " + scanResult.SSID);
				isPhoneLocationKnown = true;
				isPhoneLocatedAtUniversity = true;
				locationSource = "wifi (" + scanResult.SSID + ")";
			}
		}
		
		if(!isPhoneLocationKnown) {
			// phone location could not be determined by wifi, trying network location instead...
			LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
			if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				Intent locationIntent = new Intent("at.ac.uniklu.mobile.sportal.LOCATION_UPDATE").putExtra(EXTRA_ALARM_ID, alarmId);
				PendingIntent pendingLocationIntent = PendingIntent.getBroadcast(this, 0, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				// remove the location receiver (so it doesn't get registered multiple times [could also happen on overlapping mute() calls)
				locationManager.removeUpdates(pendingLocationIntent);
				
				if(Preferences.getLocationStatus(preferences) == Preferences.LOCATION_STATUS_RECEIVED) {
					isPhoneLocationKnown = true;
					pendingLocationIntent.cancel();
					Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					if(location == null) {
						Log.d(TAG, "location received but still null");
					} else {
						MutingRegion mutingRegion = MutingUtils.findOverlappingMutingRegion(location);
						if(mutingRegion != null) {
							Log.d(TAG, "phone located by network @ " + mutingRegion.getName());
							isPhoneLocatedAtUniversity = true;
							locationSource = "location (" + mutingRegion.getName() + ")";
						}
					}
				} else {
					Log.d(TAG, "trying to locate the phone by network...");
					// wait for a location update
					Preferences.setLocationStatus(preferences, Preferences.LOCATION_STATUS_WAITING);
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, pendingLocationIntent);
					return; // exit method - it will be re-called from the location broadcast receiver on a location update
				}
			}
		}

		boolean isAlwaysMuteEnabled = Preferences.isAutomuteWithoutLocation(this, preferences);
		
		if(isPhoneLocationKnown) {
			if(!isPhoneLocatedAtUniversity) {
				Log.d(TAG, "phone is not located at university, scheduling next mute");
				scheduleMute(true);
				removeNotification(Studentportal.NOTIFICATION_MS_INFO);
				return;
			}
		} else {
			Log.d(TAG, "phone cannot be located");
			if(!isAlwaysMuteEnabled) {
				Log.d(TAG, "alwaysmute is disabled, scheduling next mute");
				Preferences.setLocationStatus(preferences, Preferences.LOCATION_STATUS_NONE);
				scheduleMute(true);
				removeNotification(Studentportal.NOTIFICATION_MS_INFO);
				return;
			}
		}
		
		// only turn the ringtone off if we aren't currently in a muting period.
		// if we are in a muting period the ringtone is already muted and the request should be ignored,
		// else rintoneTurnOn() won't turn the ringtone back on because ringtone override will be set to true
		if(!Preferences.isMutingPeriod(preferences)) {
			MutingUtils.ringtoneTurnOff(this);
		}
		
		// persist that from now on the phone is in a muting period
		Preferences.setMutingPeriod(preferences, true);
		
		// inform user via a notification that a course has started and the phone has been muted
		notifyUser(Studentportal.NOTIFICATION_MS_INFO, true, mutingPeriod.getName(), 
				mutingPeriod.getName(), getString(R.string.automute_notification_course_muted));
		
		final boolean isPhoneLocationKnownAnalytics = isPhoneLocationKnown;
		final String locationSourceAnalytics = locationSource;
		Analytics.onEvent(Analytics.EVENT_MUTINGSERVICE_MUTE, 
				"isPhoneLocationKnown", isPhoneLocationKnownAnalytics+"",
				"locationSource", locationSourceAnalytics);
		
		scheduleUnmute(mutingPeriod.getEnd());
	}
	
	private void unmute() {
		Log.d(TAG, "unmute()");
		Analytics.onEvent(Analytics.EVENT_MUTINGSERVICE_UNMUTE);

		MutingUtils.ringtoneTurnOn(this);
		
		// persist that from now on the phone is NOT in a muting period
		Editor preferenceEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		Preferences.setMutingPeriod(preferenceEditor, false);
		Preferences.setLocationStatus(preferenceEditor, Preferences.LOCATION_STATUS_NONE);
		preferenceEditor.commit();
		
		// remove eventually existing user notification
		removeNotification(Studentportal.NOTIFICATION_MS_INFO);
		
		scheduleMute();
	}
	
	private void notifyUser(int id, boolean ongoing, String tickerText, String contentTitle, String contentText, int icon, Intent intent) {
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		Notification notification = new NotificationCompat.Builder(this)
				.setSmallIcon(icon)
				.setTicker(tickerText)
				.setContentText(contentText)
				.setContentTitle(contentTitle)
				.setContentIntent(contentIntent)
				.setWhen(System.currentTimeMillis())
				.setOngoing(ongoing)
				.build();
		
		notificationManager.notify(id, notification);
	}
	
	private void notifyUser(int id, boolean ongoing, String tickerText, String contentTitle, String contentText) {
		notifyUser(id, ongoing, tickerText, contentTitle, contentText, R.drawable.notify_course, new Intent());
	}
	
	private void removeNotification(int id) {
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(id);
	}
}
