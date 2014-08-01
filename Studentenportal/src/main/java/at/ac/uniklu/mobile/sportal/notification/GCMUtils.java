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

import java.lang.reflect.Method;

import com.google.android.gcm.GCMRegistrar;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import at.ac.uniklu.mobile.sportal.R;
import at.ac.uniklu.mobile.sportal.Studentportal;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.Preferences;

public class GCMUtils {
	
	private static final String TAG = "GCMUtils";
	
	public static final int DISABLED = 1;
	public static final int ALREADY_REGISTERED = 2;
	public static final int REGISTERING_TRIGGERED = 3;
	public static final int UNSUPPORTED = 4;
	
	public static final String SENDER_ID = "222426773067";
	
	public static NotificationCompat.Builder getDefaultNotification(Context context) {
		String defaultText = context.getString(R.string.app_name);
		return new NotificationCompat.Builder(context)
				.setDefaults(android.app.Notification.DEFAULT_VIBRATE | android.app.Notification.DEFAULT_SOUND)
				.setLights(context.getResources().getColor(R.color.unilightblue), 1000, 3000)
				.setAutoCancel(true)
				.setOnlyAlertOnce(true)
				.setTicker(defaultText)
				.setContentTitle(defaultText)
				.setSmallIcon(R.drawable.ic_stat_default);
	}
	
	public static NotificationCompat.Builder getBroadcastMessageNotification(Context context, CharSequence message, String url) {
		NotificationCompat.Builder nb = getDefaultNotification(context)
				.setContentText(message)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
		
		Intent i;
		
		if(url != null) {
			i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		} else {
			i = new Intent();
		}
		
		PendingIntent pi = PendingIntent.getActivity(context, 0, i, Intent.FLAG_ACTIVITY_NEW_TASK);
		nb.setContentIntent(pi);
		
		return nb;
	}
	
	public static int register(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if(Preferences.isNotificationsEnabled(context, preferences)) {
			//GCMRegistrar.unregister(context);
			try {
				GCMRegistrar.checkDevice(context);
				if(Studentportal.isDebugBuild()) { // the published app manifest doesn't need to be checked
					GCMRegistrar.checkManifest(context);
				}
				String regId = GCMRegistrar.getRegistrationId(context);
				if (regId.equals("")) {
					GCMRegistrar.register(context, SENDER_ID);
					return REGISTERING_TRIGGERED;
				} else {
					Log.v(TAG, "Already registered");
					return ALREADY_REGISTERED;
				}
			} catch (UnsupportedOperationException e) {
				Log.i(TAG, "this device doesn't support cloud messaging");
				Toast.makeText(context, R.string.error_notification_not_supported, Toast.LENGTH_SHORT).show();
				Preferences.setNotificationsEnabled(context, preferences, false);
				GCMStatusBroadcastCommunicator.sendBroadcast(context, 
						new Intent().putExtra(GCMStatusBroadcastCommunicator.EXTRA_GCM_UP, false));
				return UNSUPPORTED;
			}
		} else {
			return DISABLED;
		}
	}
	
	public static void unregister(Context context) {
		GCMRegistrar.unregister(context);
	}
	
	/**
	 * 
	 * @param context
	 * @return true if registered and unregistering has been triggered, false if not registered
	 */
	public static boolean unregisterIfRegistered(Context context) {
		if(GCMRegistrar.isRegistered(context)) {
			GCMUtils.unregister(context);
			return true;
		}
		return false;
	}
	
	public static void registerToCampus(Context context, String regId) {
		context.startService(new Intent(context, GCMCampusService.class)
				.setAction(GCMCampusService.ACTION_REGISTER)
				.putExtra(GCMCampusService.EXTRA_REGID, regId));
	}
	
	public static void unregisterFromCampus(Context context, String regId) {
		context.startService(new Intent(context, GCMCampusService.class)
				.setAction(GCMCampusService.ACTION_UNREGISTER)
				.putExtra(GCMCampusService.EXTRA_REGID, regId));
	}
	
	public static void notifyUser(Context context) {
		context.startService(new Intent(context, GCMCampusService.class)
				.setAction(GCMCampusService.ACTION_NOTIFY));
	}
	
	public static void OnMainActivityCreate(Context context) {
		int registrationStatus = register(context);
		
		/* If the app has been registered to GCM but not to the campus server, catch
		 * up the registration on the campus server. */
		if(registrationStatus == ALREADY_REGISTERED && !GCMRegistrar.isRegisteredOnServer(context)) {
			registerToCampus(context, GCMRegistrar.getRegistrationId(context));
		}
	}
	
	public static void OnMainActivityDestroy(Context context) {
		// application context must be used, else an exception is thrown
		// http://stackoverflow.com/a/11937272
		GCMRegistrar.onDestroy(context.getApplicationContext());
	}
	
	/**
	 * Clears the registered GCM ID and makes the GCM library think that the app isn't 
	 * registered at GCM.
	 * After this method has been called, the ID is still stored in the GCM system and
	 * also on the campus server for the previous user. Sooner or later the GCM will
	 * invalidate the ID (either because it's getting too old or it notices that no client
	 * responds to messages with this ID [just my wild guess]), similar to the process
	 * described here: http://developer.android.com/google/gcm/adv.html#unreg
	 * At some later time the campus server might want to send a message to the device
	 * with this ID, get the answer from GCM that the ID is invalid and delete it from
	 * it's database.
	 * The GCM library does exactly the same after the app has been updated to a new
	 * version, so this procedure should be save and the server infrastructure should
	 * be able to handle it.
	 */
	public static void clearRegistrationId(Context context) {
		try {
			/* The method is private and therefore needs to be accessed by reflection.
			 * Maybe Google doesn't want app to call it directly, but the alternative would
			 * be to set up a much more complicated workflow that first unregisters the app,
			 * and then checks in the onUnregistered handler if some flag is set that tells
			 * the app to register again. The advantage would though be, that the ID gets
			 * invalidated instantly in the GCM system. */
			Method m = GCMRegistrar.class.getDeclaredMethod("clearRegistrationId", Context.class);
			m.setAccessible(true);
			m.invoke(null, context);
		} catch (Exception e) {
			Log.e(TAG, "reflection call to clear regId failed", e);
			Analytics.onError(Analytics.ERROR_GCM_CLEAR_REGID, e);
		}
	}
}
