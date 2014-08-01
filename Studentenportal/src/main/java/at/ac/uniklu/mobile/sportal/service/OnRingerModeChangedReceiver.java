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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receives a broadcast telling that the ringer mode of the telephone has changed. This is used
 * for cases when the MutingService has muted the phone but the user has changed it manually 
 * afterwards. In this case, the phone will not be unmuted after a course since I assume that the 
 * user wants his manual setting to be active. The following course is independent of the current 
 * one and will be muted either way, at least in case the phone isn't already muted.
 */
public class OnRingerModeChangedReceiver extends BroadcastReceiver  {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		boolean ignoreThisBroadcast = MutingUtils.isRingerModeChangedBroadcastToIgnore(context);
		Log.d("OnRingerModeChangedReceiver", "ringer mode changed (ignored: " + ignoreThisBroadcast + ")");
		if(!ignoreThisBroadcast) {
//			Analytics.onServiceStart(context);
			MutingUtils.ringtoneUserOverride(context);
//			Analytics.onEvent(Analytics.EVENT_MUTINGSERVICE_OVERRIDE);
//			Analytics.onServiceStop(context);
		}
	}
}
