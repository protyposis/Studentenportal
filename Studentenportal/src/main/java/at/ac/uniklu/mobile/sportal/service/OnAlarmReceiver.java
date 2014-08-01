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
import at.ac.uniklu.mobile.sportal.widget.SimpleCalendarWidgetService;

public class OnAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("OnAlarmReceiver", "RRRRRRIIIIINNGGGGGGGG for " + intent.getStringExtra("target"));
		
		if("widget".equals(intent.getStringExtra("target"))) {
			SimpleCalendarWidgetService.updateWidgets(context);
			return;
		}
		
		MutingServiceWakeLock.acquireWakeLock(context);
		
		int action = intent.getExtras().getInt(MutingService.ACTION);
		int alarmId = intent.getExtras().getInt(MutingService.EXTRA_ALARM_ID);
		context.startService(new Intent(context, MutingService.class)
				.putExtra(MutingService.ACTION, action)
				.putExtra(MutingService.EXTRA_ALARM_ID, alarmId));
	}

}
