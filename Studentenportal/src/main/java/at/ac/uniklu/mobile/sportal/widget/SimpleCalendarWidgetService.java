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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import at.ac.uniklu.mobile.sportal.CalendarActivity;
import at.ac.uniklu.mobile.sportal.LoginActivity;
import at.ac.uniklu.mobile.sportal.R;
import at.ac.uniklu.mobile.sportal.api.ApiClientException;
import at.ac.uniklu.mobile.sportal.api.ApiServerException;
import at.ac.uniklu.mobile.sportal.api.Termin;
import at.ac.uniklu.mobile.sportal.model.CalendarModel;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.service.OnAlarmReceiver;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.Utils;

public class SimpleCalendarWidgetService extends IntentService {
	
	private static final String TAG = "SimpleCalendarWidgetService";

	public SimpleCalendarWidgetService() {
		super("SimpleCalendarWidgetService");
	}

	@Override
	public void onHandleIntent(Intent intent) {
		Log.d(TAG, "updating widgets...");
		
		// https://github.com/commonsguy/cw-andtutorials/blob/master/34-AdvAppWidget/LunchList/src/apt/tutorial/WidgetService.java
		ComponentName widgetComponent = new ComponentName(this, SimpleCalendarWidgetProvider.class);
		RemoteViews updateViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_simplecalendar);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		
		updateViews.setViewVisibility(R.id.widget_nocontent, View.GONE);
		updateViews.setViewVisibility(R.id.widget_content, View.GONE);
		updateViews.setViewVisibility(R.id.widget_refreshing, View.VISIBLE);
		appWidgetManager.updateAppWidget(widgetComponent, updateViews);
		
		Intent startApp = new Intent(this, LoginActivity.class);
		Intent startCalendar = new Intent(this, CalendarActivity.class);
		
		try {
			CalendarModel model = null;
			
			int retries = 3;
			for(int retry = 1; retry <= retries; retry++) {
				try {
					/* try to fetch the model for a few times since the internet connection
					 * may not be active if a widget is initialized after a (re)boot */
					model = ModelService.getDashboardModel(this);
					break;
				} catch (ApiClientException e) {
					/* if the internet connection isn't active we get a unknown host exception */
					if(e.hasCode() && e.getCode() == ApiClientException.Code.UNKNOWNHOST && retry < retries) {
						Log.d(TAG, "cannot fetch data, going to sleep for a while...");
						// sleep some time taking another try
						Thread.sleep(10000 * retry);
					} else {
						throw e;
					}
				}
			}
			
			// filter eventually passed or canceled events
			List<Termin> filteredDates = new ArrayList<Termin>();
			long currentTime = System.currentTimeMillis();
			for(Termin t : model.getDates()) {
				if(!(t.getEndDate().getTime() < currentTime) && !t.isStorniert()) {
					filteredDates.add(t);
				}
			}
			
			updateViews.setViewVisibility(R.id.widget_refreshing, View.GONE);
			if(filteredDates.isEmpty()) {
				updateViews.setTextViewText(R.id.widget_nocontent, getString(R.string.dates_list_empty));
				updateViews.setViewVisibility(R.id.widget_nocontent, View.VISIBLE);
				// start app on info message press
				updateViews.setOnClickPendingIntent(R.id.calendar, PendingIntent.getActivity(
						this, 0, startApp, Intent.FLAG_ACTIVITY_NEW_TASK));
			} else {
				Termin t = filteredDates.get(0);
				prepareDate(updateViews, R.id.date1_info, R.id.date1_title, t);
				
				if(filteredDates.size() > 1) {
					t = filteredDates.get(1);
					prepareDate(updateViews, R.id.date2_info, R.id.date2_title, t);
				} else {
					prepareDate(updateViews, R.id.date2_info, R.id.date2_title, null);
				}
				
				updateViews.setViewVisibility(R.id.widget_content, View.VISIBLE);
				
				// start calendar activity on calendar dates press
				updateViews.setOnClickPendingIntent(R.id.calendar, PendingIntent.getActivity(
						this, 0, startCalendar, 0));
				
				// schedule next widget refresh for when the current termin ends
				Date nextRefreshDate = filteredDates.get(0).getDatum();
				if(filteredDates.get(0).isNow()) {
					nextRefreshDate = filteredDates.get(0).getEndDate();
				}
				if(!Utils.isToday(nextRefreshDate)) {
					/* if the next date isn't today, we need to take care of the date labels on the widget */
					if(Utils.isTomorrow(nextRefreshDate)) {
						/* if the next date is tomorrow, refresh the widget at the start of
						 * tomorrow to change the "tomorrow" text to "today" */
						nextRefreshDate = Utils.getMidnight(nextRefreshDate);
					} else {
						/* if the next date is after tomorrow, refresh the widget at the start
						 * of the day before the actual date takes place to change the date to
						 * "tomorrow" */
						nextRefreshDate = new Date(Utils.getMidnight(nextRefreshDate).getTime() - Utils.MILLIS_PER_DAY);
					}
				}
				
				if(nextRefreshDate.before(new Date())) {
					/* This could happen in certain conditions with timezone changes or daylight saving changes. */
					Log.e(TAG, "refresh date before current date!!");
					nextRefreshDate = new Date(new Date().getTime() + Utils.MILLIS_PER_HOUR);
				}
				
				AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
				Intent alarmIntent = new Intent(this, OnAlarmReceiver.class).putExtra("target", "widget");
				PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(this, 1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				long time = SystemClock.elapsedRealtime() + (nextRefreshDate.getTime() - System.currentTimeMillis()) + 5000; //  convert unixtime to system runtime, add 5 seconds
				alarmManager.set(AlarmManager.ELAPSED_REALTIME, time, pendingAlarmIntent);
			}
		} catch (Exception e) {
			Analytics.onError(Analytics.ERROR_WIDGET_SIMPLECALENDAR_UPDATE, e);
			
			CharSequence errorMsg = null;
			if(e instanceof ApiServerException) {
				ApiServerException e2 = (ApiServerException)e;
				if(e2.getError() != null && e2.getError().getCode() == 401) {
					errorMsg = getString(R.string.error_login_failed_msg);
				}
			} else {
				// generic error message
				errorMsg = getString(R.string.error_fetching_dates);
			}
			updateViews.setTextViewText(R.id.widget_nocontent, errorMsg);
			
			updateViews.setViewVisibility(R.id.widget_refreshing, View.GONE);
			updateViews.setViewVisibility(R.id.widget_nocontent, View.VISIBLE);
			
			// start app on error message press
			updateViews.setOnClickPendingIntent(R.id.calendar, PendingIntent.getActivity(
					this, 0, startApp, 0));
		}
		
		// start app on logo button press
		updateViews.setOnClickPendingIntent(R.id.app, PendingIntent.getActivity(
				this, 1, startApp, 0));
		
		// update all widgets
		appWidgetManager.updateAppWidget(widgetComponent, updateViews);
	}
	
	private void prepareDate(RemoteViews remoteViews, int infoTextId, int titleTextId, Termin t) {
		if(t == null) { // if no date given, hide the textviews
			remoteViews.setViewVisibility(infoTextId, View.GONE);
			remoteViews.setViewVisibility(titleTextId, View.GONE);
		} else {
			remoteViews.setTextViewText(infoTextId, getInfoText(t));
			remoteViews.setTextViewText(titleTextId, t.getTitleWithType());
			remoteViews.setViewVisibility(infoTextId, View.VISIBLE);
			remoteViews.setViewVisibility(titleTextId, View.VISIBLE);
			
			if(t.isNow()) {
				remoteViews.setTextColor(titleTextId, getResources().getColor(R.color.date_running));
			} else {
				remoteViews.setTextColor(titleTextId, getResources().getColor(android.R.color.white));
			}
		}
	}
	
	private String getInfoText(Termin t) {
		String dateText;
		
		if(t.isNow()) {
			dateText = getString(R.string.dates_now).toUpperCase(Locale.GERMAN);
		} else if(Utils.isToday(t.getDatum())) {
			dateText = getString(R.string.dates_today).toUpperCase(Locale.GERMAN);
		} else if(Utils.isTomorrow(t.getDatum())) {
			dateText = getString(R.string.dates_tomorrow).toUpperCase(Locale.GERMAN);
		} else {
			dateText = getString(R.string.calendar_date, t.getDatum()).toUpperCase(Locale.GERMAN);
		}
		
		return dateText + " " 
				+ getString(R.string.calendar_timespan, t.getDatum(), t.getEndDate()) + " " 
				+ t.getRaum();
	}
	
	public static void updateWidgets(Context context) {
		// https://github.com/commonsguy/cw-andtutorials/blob/master/34-AdvAppWidget/LunchList/src/apt/tutorial/AppWidget.java
		Log.d(TAG, "requesting widgets update...");
		context.startService(new Intent(context, SimpleCalendarWidgetService.class));
	}
}
