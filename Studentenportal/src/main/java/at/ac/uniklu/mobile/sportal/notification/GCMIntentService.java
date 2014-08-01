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

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.Studentportal;
import at.ac.uniklu.mobile.sportal.util.Analytics;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {
	
	private static final String TAG = "GCMIntentService";
	
	public GCMIntentService() {
		super(GCMUtils.SENDER_ID);
	}

	@Override
	protected void onError(Context context, String errorId) {
		Log.e(TAG, "GCM error: " + errorId);
		Analytics.onError(Analytics.ERROR_GCM, new Exception("GCM error: " + errorId));
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		/* Default case. The notification tickle tells the app to fetch new notifications
		 * from the campus server. */
		if(intent.hasExtra("collapse_key") && intent.getStringExtra("collapse_key").equals("notification-tickle")) {
			Analytics.onEvent(Analytics.EVENT_GCM_MSG_NOTIFICATIONTICKLE);
			GCMUtils.notifyUser(context);
		}
		/* A message text with an optional URL */
		else if(intent.hasExtra("type") && intent.getStringExtra("type").equals("broadcastmessage")) {
			Analytics.onEvent(Analytics.EVENT_GCM_MSG_BROADCAST);
			
			String message = intent.getStringExtra("message");
			String url = null;

			if(intent.hasExtra("url")) {
				url = intent.getStringExtra("url");
			}

			NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			NotificationCompat.Builder nb = GCMUtils.getBroadcastMessageNotification(this, message, url);
			notificationManager.notify(Studentportal.NOTIFICATION_GCM_BROADCASTMESSAGE, nb.build());
		}
		/* Service message. Tells the client to invalidate it's ID and re-register. Might
		 * be useful some day if the database on the campus server needs to be cleaned/rebuilt/invalidated. */
		else if(intent.hasExtra("type") && intent.getStringExtra("type").equals("refresh-registration")) {
			GCMUtils.clearRegistrationId(context);
			GCMUtils.register(context);
		} 
		else {
			Analytics.onEvent(Analytics.EVENT_GCM_MSG_UNKNOWN);
		}
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		Analytics.onEvent(Analytics.EVENT_GCM_REGISTERED);
		GCMUtils.registerToCampus(context, regId);
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		/* If the regId is empty, unregister has probably been called although the device
		 * hasn't been registered... which makes further processing obsolete (and produce errors). */
		if("".equals(regId))
			return;
		
		Analytics.onEvent(Analytics.EVENT_GCM_UNREGISTERED);
		GCMUtils.unregisterFromCampus(context, regId);
	}

}
