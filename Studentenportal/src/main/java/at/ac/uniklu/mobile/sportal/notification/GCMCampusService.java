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

package at.ac.uniklu.mobile.sportal.notification;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.android.gcm.GCMRegistrar;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.CourseListActivity;
import at.ac.uniklu.mobile.sportal.ExamListActivity;
import at.ac.uniklu.mobile.sportal.GradeListActivity;
import at.ac.uniklu.mobile.sportal.R;
import at.ac.uniklu.mobile.sportal.Studentportal;
import at.ac.uniklu.mobile.sportal.api.Notification;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.Preferences;

/**
 * Service to (un)register the device on the campus server.
 *  
 * This service is existing to be able to (un)register from everywhere in the 
 * application without the need of messing around with threads.
 */
public class GCMCampusService extends IntentService {
	
	private static final String TAG = "GCMCampusService";
	
	public static final String ACTION_REGISTER = "register";
	public static final String ACTION_UNREGISTER = "unregister";
	public static final String ACTION_NOTIFY = "notify";
	public static final String EXTRA_REGID = "regId";
	

	public GCMCampusService() {
		super("GCMCampusService");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		if(action == null) {
			Log.e(TAG, "no action specified");
		}
		if(action.equals(ACTION_REGISTER)) {
			register(intent.getStringExtra(EXTRA_REGID));
		} else if(action.equals(ACTION_UNREGISTER)) {
			unregister(intent.getStringExtra(EXTRA_REGID));
		} else if(action.equals(ACTION_NOTIFY)) {
			notifyUser();
		}
	}
	
	private void register(String regId) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		try {
			Studentportal.getSportalClient().postGCMRegistrationId(regId);
			GCMRegistrar.setRegisteredOnServer(this, true);
			Preferences.setNotificationsEnabled(this, preferences, true);
			// only get notifications from the time of registering, none of the time before
			Preferences.setNotificationsLastCheckDate(this, preferences, new Date());
			GCMStatusBroadcastCommunicator.sendBroadcast(this, 
					new Intent().putExtra(GCMStatusBroadcastCommunicator.EXTRA_GCM_UP, true));
		} catch (Exception e) {
			Log.e(TAG, "gcm registering on campus failed", e);
			/* If registering at the campus server fails, unregister from GCM to avoid an illegal app
			 * state where the app thinks it is registered but the campus server doesn't know about its
			 * existence. 
			 * A new registration try will be executed on the next app start. */
			GCMUtils.unregister(this);
			Preferences.setNotificationsEnabled(this, preferences, false);
			GCMStatusBroadcastCommunicator.sendBroadcast(this, 
					new Intent().putExtra(GCMStatusBroadcastCommunicator.EXTRA_GCM_UP, false));
		}
	}
	
	private void unregister(String regId) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		try {
			Studentportal.getSportalClient().deleteGCMRegistrationId(regId);
			GCMRegistrar.setRegisteredOnServer(this, false);
			Preferences.setNotificationsEnabled(this, preferences, false);
			GCMStatusBroadcastCommunicator.sendBroadcast(this, 
					new Intent().putExtra(GCMStatusBroadcastCommunicator.EXTRA_GCM_UP, false));
		} catch (Exception e) {
			Log.e(TAG, "gcm unregistering from campus failed", e);
		}
	}

	private void notifyUser() {
		try {
			NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			
			// get all notifications since last check time
			List<Notification> notifications = Studentportal.getSportalClient()
					.getNotifications(Preferences.getNotificationsLastCheckDate(this, preferences));
			// set last check time to current time
			Preferences.setNotificationsLastCheckDate(this, preferences, new Date());
			
			Collections.reverse(notifications);
			for(Notification n : notifications) {
				Log.i(TAG, n.toString());
				
				String title = n.getName();
				String text = null;
				Intent i = null;
				
				switch(n.getType()) {
				case UNKNOWN:
					// skip unknown notification (might be a new type for a newer app version)
					continue;
				case INSKRIPTION:
					text = getString(R.string.notification_inskription);
					break;
				case LV_AUFGENOMMEN:
					text = getString(R.string.notification_lv_aufgenommen);
					Studentportal.getSportalClient().clearLehrveranstaltungen();
					i = new Intent(this, CourseListActivity.class);
					break;
				case LV_STATUS:
					text = getString(R.string.notification_lv_status) + " " + n.getValue();
					Studentportal.getSportalClient().clearLehrveranstaltungen();
					i = new Intent(this, CourseListActivity.class);
					break;
				case LV_UMMELDUNG:
					text = getString(R.string.notification_lv_ummeldung) + " " + n.getValue();
					Studentportal.getSportalClient().clearLehrveranstaltungen();
					i = new Intent(this, CourseListActivity.class);
					break;
				case PRUEFUNG_ANMELDUNG:
					text = getString(R.string.notification_pruefung_anmeldung);
					Studentportal.getSportalClient().clearPruefungen();
					i = new Intent(this, ExamListActivity.class);
					break;
				case NOTE_NEU:
					text = getString(R.string.notification_note_neu) + " " + n.getValue();
					Studentportal.getSportalClient().clearNoten();
					i = new Intent(this, GradeListActivity.class);
					break;
				case STUDIUM_ABSCHLUSS:
					text = getString(R.string.notification_studium_abschluss);
					break;
				case STUDIUM_ABSCHNITTSABSCHLUSS:
					text = getString(R.string.notification_studium_abschnittsabschluss);
					break;
				case STUDIUM_STEOPERFUELLT:
					text = getString(R.string.notification_steoperfuellt);
					break;
				}
				
				NotificationCompat.Builder nb = GCMUtils.getDefaultNotification(this);
				
				nb.setTicker(title);
				nb.setContentTitle(title);
				nb.setContentText(text);
				
				if(i == null) {
					/* intent must not be null on Android 2.3, even if the notification 
					 * doesn't do anything when selected; else, an exception gets thrown:
					 * java.lang.IllegalArgumentException: contentIntent required */
					i = new Intent();
				}
				
				/* Pass System.currentTimeMillis() as requestId to make every pending intent unique. Otherwise,
				 * they would overwrite each other if the intent is the same and not all notification (if shown
				 * at the same time) would trigger the desired action when selected. */
				nb.setContentIntent(PendingIntent.getActivity(this, (int)System.currentTimeMillis(), i, Intent.FLAG_ACTIVITY_NEW_TASK));
				
				notificationManager.notify(Studentportal.NOTIFICATION_GCM_NOTIFICATION + n.getType().ordinal(), nb.build());
			}
		} catch (Exception e) {
			Analytics.onError(Analytics.ERROR_NOTIFICATIONS_REQUEST, e);
		}
	}
}
