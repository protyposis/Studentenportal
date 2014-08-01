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

package at.ac.uniklu.mobile.sportal.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.service.OnAlarmReceiver;
import at.ac.uniklu.mobile.sportal.util.Analytics;

public class SimpleCalendarWidgetProvider extends AppWidgetProvider {
	
	private static final String TAG = "SimpleCalendarWidgetProvider";
	
	@Override
	public void onEnabled(Context context) {
		// nothing to do here
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		/* This method is only executed once when a widget instance is added, 
		 * because I don't use updatePeriodMillis. */
		Analytics.onEvent(Analytics.EVENT_WIDGET_SIMPLECALENDAR_ADD);
		/* Update the widget for the first time, subsequent updates will happen 
		 * through the alarmmanager which directly calls the service. */
		SimpleCalendarWidgetService.updateWidgets(context);
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		Analytics.onEvent(Analytics.EVENT_WIDGET_SIMPLECALENDAR_REMOVE);
	}
	
	@Override
	public void onDisabled(Context context) {
		Log.d(TAG, "onDisabled");
		/* When the last widget instance gets removed, the pending alarm intent
		 * that updates the widgets needs to be cancelled. */
		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent alarmIntent = new Intent(context, OnAlarmReceiver.class);
		PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(context, 1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.cancel(pendingAlarmIntent);
		pendingAlarmIntent.cancel();
	}
}
