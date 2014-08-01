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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public abstract class LocalBroadcastCommunicator extends BroadcastReceiver {
	
	// the action name used to send and receive intents
	private static String sAction = null;

	private static <T> void registerReceiver(LocalBroadcastCommunicator receiver, Context context) {
		sAction = receiver.getClass().getName();
		LocalBroadcastManager.getInstance(context).registerReceiver(receiver, new IntentFilter(sAction));
	}
	
	private static <T> void unregisterReceiver(LocalBroadcastCommunicator receiver, Context context) {
		LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
	}
	
	public static void sendBroadcast(Context context, Intent intent) {
		if(sAction == null) {
			/* If sAction is null at this point, no receiver has been registered yet and there's no
			 * point in sending a broadcast. */
			return;
		}
		
		intent.setAction(sAction);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	public void registerReceiver(Context context) {
		registerReceiver(this, context);
	}
	
	public void unregisterReceiver(Context context) {
		unregisterReceiver(this, context);
	}
}
