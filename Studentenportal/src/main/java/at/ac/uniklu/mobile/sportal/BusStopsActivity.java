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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import at.ac.uniklu.mobile.sportal.publictransport.stw.Stop;
import at.ac.uniklu.mobile.sportal.publictransport.stw.StopDB;
import at.ac.uniklu.mobile.sportal.publictransport.stw.StopFinder;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.ui.RotationAwareAsyncTask;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class BusStopsActivity extends ListActivity 
		implements ProgressNotificationToggle, Refreshable {
    
	private static final String TAG = "BusStopsActivity";
	
	private StopsAdapter mStopsListAdapter;
    private StopDB mDB;
    private ActionBarHelper mActionBar;
    private BusStopsRefreshTask mRefreshTask;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bus_stops);
        mActionBar = new ActionBarHelper(this)
        		.setupHeader()
        		.addActionRefresh(R.string.bus_stops_reload);
        
		mDB = new StopDB(this);
		
		mRefreshTask = (BusStopsRefreshTask)getLastNonConfigurationInstance();
		if(mRefreshTask != null) {
			mRefreshTask.attach(this);
		}
		
		loadStops();
    }
	
	@Override
	protected void onStart() {
		super.onStart();
		Analytics.onActivityStart(this, Analytics.ACTIVITY_BUS_STOPS);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Analytics.onActivityStop(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDB.close();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		mRefreshTask.detach();
		return mRefreshTask;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if(getListView().isItemChecked(position)) {
			mDB.selectStop(mStopsListAdapter.getItem(position));
		} else {
			mDB.deselectStop(mStopsListAdapter.getItem(position));
		}
	}
	
	private void loadStops() {
		loadStops(false);
	}
	
	private void loadStops(final boolean reload) {
		mRefreshTask = new BusStopsRefreshTask(this, mDB, reload);
		mRefreshTask.execute();
	}
    
    public static void initializeStopDB(StopDB db, List<Stop> selectedStops) throws Exception {
		StopFinder mStopFinder = new StopFinder();
		List<Stop> stops = mStopFinder.queryAll();
		Log.d(TAG, stops.size() + " stops loaded from web");
		db.clearStops();
		db.insertStops(stops);
		
		// preselect stops
		if(selectedStops != null && selectedStops.size() > 0) {
			Set<Integer> stopCodes = new HashSet<Integer>();
			for(Stop stop : stops) {
				stopCodes.add(stop.getCode());
			}
			for(Stop selectedStop : selectedStops) {
				if(stopCodes.contains(selectedStop.getCode())) {
					db.selectStop(selectedStop);
				}
			}
		}
    }
    
    @Override
	public void progressNotificationOn() {
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
		mActionBar.progressNotificationOn();
	}

	@Override
	public void progressNotificationOff() {
		findViewById(R.id.progress).setVisibility(View.GONE);
		mActionBar.progressNotificationOff();
	}

	@Override
	public void refresh() {
		Analytics.onEvent(Analytics.EVENT_DEPARTURES_STOPS_REFRESH);
		loadStops(true);
	}
	
	private static class BusStopsRefreshTask extends RotationAwareAsyncTask<BusStopsActivity> {
    	
		private StopDB mDB;
		private boolean mReload;
		
		private List<Stop> mStops;
		private List<Stop> mSelectedStops;
		
    	public BusStopsRefreshTask(BusStopsActivity activity, StopDB db, boolean reload) {
			super(activity, activity);
			mDB = db;
			mReload = reload;
		}

		@Override
		protected void doInBackground() throws Exception {
			// check if the DB is empty and load stops from the webserver
			mStops = mDB.getStops();
			if(mStops.isEmpty() || mReload) {
				mSelectedStops = mDB.getSelectedStops();
				initializeStopDB(mDB, mSelectedStops);
			}
			
			// load stops from DB
			mStops = mDB.getStops();
			mSelectedStops = mDB.getSelectedStops();
		}
    	
    	@Override
		protected void onException(Exception e) {
    		e.printStackTrace();
			UIUtils.showSimpleErrorDialogAndClose(mActivity, e.getMessage());
			Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_BUS_STOPS, e);
		}
		
		@Override
		protected void onSuccess() {
			mActivity.mStopsListAdapter = new StopsAdapter(mActivity, R.layout.simple_list_item_checked, mStops);
			mActivity.setListAdapter(mActivity.mStopsListAdapter);
			ListView listView = mActivity.getListView();
			for(Stop stop : mSelectedStops) {
				listView.setItemChecked(mStops.indexOf(stop), true);
			}
		}
    }
	
	private static class StopsAdapter extends ArrayAdapter<Stop> implements SectionIndexer {

		private Map<String, Integer> mAlphaIndexer;
		private Object[] mSections;

		public StopsAdapter(Context context, int textViewResourceId, List<Stop> items) {
			super(context, textViewResourceId, items);

			// http://twistbyte.com/tutorial/android-listview-with-fast-scroll-and-section-index
			mAlphaIndexer = new HashMap<String, Integer>();
			List<String> sectionList = new ArrayList<String>(64);
			for (int x = 0; x < items.size(); x++) {
				String ch = items.get(x).getName().substring(0, 1);
				if(!mAlphaIndexer.containsKey(ch)) {
					mAlphaIndexer.put(ch, x);
					sectionList.add(ch);
				}
			}
			mSections = sectionList.toArray();
		}

		@Override
		public int getPositionForSection(int section) {
			return mAlphaIndexer.get(mSections[section]);
		}

		@Override
		public int getSectionForPosition(int position) {
			return 1;
		}

		@Override
		public Object[] getSections() {
			return mSections;
		}
	}
}
