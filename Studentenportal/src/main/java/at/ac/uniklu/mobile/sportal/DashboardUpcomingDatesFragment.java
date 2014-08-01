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

package at.ac.uniklu.mobile.sportal;

import java.util.Date;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import at.ac.uniklu.mobile.sportal.api.Termin;
import at.ac.uniklu.mobile.sportal.loader.AsyncResult;
import at.ac.uniklu.mobile.sportal.loader.DashboardModelLoader;
import at.ac.uniklu.mobile.sportal.model.CalendarModel;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;
import at.ac.uniklu.mobile.sportal.util.Utils;
import at.ac.uniklu.mobile.sportal.widget.SimpleCalendarWidgetService;

public class DashboardUpcomingDatesFragment extends Fragment 
		implements ProgressNotificationToggle, Refreshable, 
		LoaderManager.LoaderCallbacks<AsyncResult<CalendarModel>> {
	
	private static final String TAG = "DashboardUpcomingDatesFragment";
	
	private CalendarModel mDashboardModel;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dashboard_upcomingdates, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		progressNotificationOn();
		getLoaderManager().initLoader(0, null, this);
		adjustDateListHeight();
	}

	@Override
	public void progressNotificationOn() {
		getView().findViewById(R.id.dates_progress).setVisibility(View.VISIBLE);
	}
	
	@Override
	public void progressNotificationOff() {
		getView().findViewById(R.id.dates_progress).setVisibility(View.GONE);
	}
	
	@Override
	public void refresh() {
		mDashboardModel = null;
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public Loader<AsyncResult<CalendarModel>> onCreateLoader(int id, Bundle args) {
		progressNotificationOn();
		return new DashboardModelLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<AsyncResult<CalendarModel>> loader, AsyncResult<CalendarModel> data) {
		Log.d(TAG, "onLoadFinished");
		progressNotificationOff();
		// TODO: if/else conditions vereinfachen
		if(data.isSuccess() && mDashboardModel == null) {
			mDashboardModel = data.getData();
			populateUpcomingDates();
			SimpleCalendarWidgetService.updateWidgets(getActivity());
		} else if(data.isFailure() && !UIUtils.processExceptionForLogin(data.getException(), getActivity(), Refreshable.REQUEST_REFRESH)) {
			Toast.makeText(getActivity(), R.string.error_fetching_dates, Toast.LENGTH_SHORT).show();
			Analytics.onError(Analytics.ERROR_UPCOMING_DATES, data.getException());
		}
	}

	@Override
	public void onLoaderReset(Loader<AsyncResult<CalendarModel>> loader) {
	}
	
	private void populateUpcomingDates() {
		long startTime = System.currentTimeMillis();
		TableLayout dateListTable = (TableLayout)getView().findViewById(R.id.dates);
		dateListTable.removeAllViews();
		
		if(mDashboardModel.getDates().isEmpty()) {
			getView().findViewById(R.id.dates_empty).setVisibility(View.VISIBLE);
			return;
		} else {
			getView().findViewById(R.id.dates_empty).setVisibility(View.GONE);
		}
		
		Termin previousDate = null;
		for(Termin date : mDashboardModel.getDates()) {
			Date from = date.getDatum();
			
			View dateTableRow = getActivity().getLayoutInflater().inflate(R.layout.dashboard_date, dateListTable, false);
			TextView dateText = (TextView)dateTableRow.findViewById(R.id.text_date);
			TextView timeText = (TextView)dateTableRow.findViewById(R.id.text_time);
			TextView roomText = (TextView)dateTableRow.findViewById(R.id.text_room);
			TextView titleText = (TextView)dateTableRow.findViewById(R.id.text_title);
			
			dateText.setText(getString(R.string.calendar_date, from));
			timeText.setText(getString(R.string.calendar_time, from));
			roomText.setText(date.getRaum());
			titleText.setText(date.getTitleWithType());
			
			int color = 0;
			if(date.isStorniert()) {
				color = getResources().getColor(R.color.date_canceled);
			} else if(date.isNow()) {
				color = getResources().getColor(R.color.date_running);
			}
			if(color != 0) {
				dateText.setTextColor(color);
				timeText.setTextColor(color);
				roomText.setTextColor(color);
				titleText.setTextColor(color);
			}
			
			if(previousDate != null && Utils.isSameDay(previousDate.getDatum(), date.getDatum())) {
				dateText.setVisibility(View.INVISIBLE);
			}
			
			dateTableRow.setVisibility(View.INVISIBLE); // will be unhidden by #adjustDateListHeight()
			dateListTable.addView(dateTableRow);
			previousDate = date;
		}
		long deltaTime = System.currentTimeMillis() - startTime;
		Log.d(TAG, "populateUpcomingDates: " + deltaTime + "ms");
		adjustDateListHeight();
	}
	
	private void adjustDateListHeight() {
		final TableLayout dateListTable = (TableLayout)getView().findViewById(R.id.dates);
		// post the action to the UI thread to defer execution until the view has been drawn
		dateListTable.post(new Runnable() {
			@Override
			public void run() {
				// get first row
				View v = dateListTable.getChildAt(0);
				int entryHeight;
				if(v != null && (entryHeight = v.getMeasuredHeight()) > 0) {
					int numPossibleVisibleRows = dateListTable.getMeasuredHeight() / entryHeight;
					int numExisitingRows = Math.min(numPossibleVisibleRows, dateListTable.getChildCount());
					int numVisibleRows = Math.min(numExisitingRows, numPossibleVisibleRows);
					// remove rows that are outside the screen
					if(numExisitingRows > numVisibleRows) {
						dateListTable.removeViews(numVisibleRows, numExisitingRows - numVisibleRows);
					}
					// toggle visibility of rows inside the screen
					for(int x = 0; x < numVisibleRows; x++) {
						dateListTable.getChildAt(x).setVisibility(View.VISIBLE);
					}
				} else {
					Log.d(TAG, "cannot adjust list height");
				}
			}
		});
	}
}
