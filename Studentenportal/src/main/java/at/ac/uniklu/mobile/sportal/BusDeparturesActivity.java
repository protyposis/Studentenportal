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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import at.ac.uniklu.mobile.sportal.publictransport.stw.Departure;
import at.ac.uniklu.mobile.sportal.publictransport.stw.DepartureMonitor;
import at.ac.uniklu.mobile.sportal.publictransport.stw.DepartureMonitorException;
import at.ac.uniklu.mobile.sportal.publictransport.stw.Departures;
import at.ac.uniklu.mobile.sportal.publictransport.stw.LineColors;
import at.ac.uniklu.mobile.sportal.publictransport.stw.Stop;
import at.ac.uniklu.mobile.sportal.publictransport.stw.StopDB;
import at.ac.uniklu.mobile.sportal.ui.AsyncTask;
import at.ac.uniklu.mobile.sportal.ui.FixedDatePickerDialog;
import at.ac.uniklu.mobile.sportal.ui.FixedTimePickerDialog;
import at.ac.uniklu.mobile.sportal.ui.GenericListAdapter;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationAsyncTask;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.Preferences;
import at.ac.uniklu.mobile.sportal.util.UIUtils;
import at.ac.uniklu.mobile.sportal.util.Utils;

public class BusDeparturesActivity extends ListActivity 
		implements ProgressNotificationToggle, Refreshable {
	
	private static final String TAG = "BusDeparturesActivity";
	private static final long REFRESH_INTERVAL = 60000; // 1 minute
	
	/* Some limit must be set to avoid loading of infinite departures, filling the memory
	 * and provoking exceptions or other system problems.
	 */
	private static final long DEPARTURES_INTERVAL = 86400000; // 24 hours
	
	private DeparturesListAdapter mDeparturesListAdapter;
    private DepartureMonitor mDepartureMonitor;
    private Departures mDepartures;
    private ProgressNotificationAsyncTask mBusDeparturesLoadingTask;
    private Filter mFilter;
    
    /**
     * Used to distinguish between the first programmatic firing of the Spinner's onItemSelected event
     * and succeeding user triggered calls.
     */
    private boolean mStopSpinnerInitialized;
    private Spinner mStopSpinner;
    private Stop mStopSelected;
    
    private View mFooterView;
    private ActionBarHelper mActionBar;
    
    private Handler mHandler = new Handler();
    private CountdownUpdater mUpdateTimeTask;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bus_departures);
        mActionBar = new ActionBarHelper(this).setupHeader();
        mActionBar.addActionButton(R.drawable.ic_action_busstop, R.string.bus_stops, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(BusDeparturesActivity.this, BusStopsActivity.class));
			}
		});
        mActionBar.addActionRefresh();
        
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        mStopSpinner = (Spinner)findViewById(R.id.bus_stops);
        mStopSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Log.d(TAG, "STOP SELECTED");
				if(!mStopSpinnerInitialized) {
					// capture the first handler call that is always made by the android system, not by the user
					mStopSpinnerInitialized = true;
				} else {
					Stop stop = (Stop)mStopSpinner.getSelectedItem();
					if(stop != mStopSelected) {
						Log.d(TAG, "stop selected: " + stop);
						Analytics.onEvent(Analytics.EVENT_LOAD_BUS_DEPARTURES, "stop", stop.getName());
						Preferences.setStwSelectedStop(preferences, stop.getCode());
						mDeparturesListAdapter = null;
						mFooterView.setVisibility(View.VISIBLE);
						loadDepartures(stop, false);
					}
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
        
        mFilter = new Filter();
        View headerView = getLayoutInflater().inflate(R.layout.bus_departures_filter, null);
        getListView().addHeaderView(headerView);
        final Button dateFilterButton = (Button)headerView.findViewById(R.id.dateselector);
        final Button timeFilterButton = (Button)headerView.findViewById(R.id.timeselector);
        
        View emptyView = findViewById(android.R.id.empty);
        final Button dateFilterButton2 = (Button)emptyView.findViewById(R.id.dateselector);
        final Button timeFilterButton2 = (Button)emptyView.findViewById(R.id.timeselector);
        
        final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				mFilter.setDate(year, monthOfYear, dayOfMonth);
				refresh();
				refreshDateButtonText(dateFilterButton, dateFilterButton2);
			}
		};
		View.OnClickListener dateFilterClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mFilter.updateUnsetDateTime();
				DatePickerDialog d = new FixedDatePickerDialog(BusDeparturesActivity.this, dateSetListener,
						mFilter.getYear(), mFilter.getMonth(), mFilter.getDay());
				d.setButton(DatePickerDialog.BUTTON_NEUTRAL, getString(R.string.dates_today), 
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mFilter.resetDate();
						refresh();
						refreshDateButtonText(dateFilterButton, dateFilterButton2);
					}
				});
				d.show();
			}
		};
		dateFilterButton.setOnClickListener(dateFilterClickListener);
		dateFilterButton2.setOnClickListener(dateFilterClickListener);

        final TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				mFilter.setTime(hourOfDay, minute);
				refresh();
				refreshTimeButtonText(timeFilterButton, timeFilterButton2);
			}
		};
		View.OnClickListener timeFilterClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mFilter.updateUnsetDateTime();
				TimePickerDialog d = new FixedTimePickerDialog(BusDeparturesActivity.this, timeSetListener, 
						mFilter.getHour(), mFilter.getMinute(), true);
				d.setButton(TimePickerDialog.BUTTON_NEUTRAL, getString(R.string.dates_now), 
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mFilter.resetTime();
						refresh();
						refreshTimeButtonText(timeFilterButton, timeFilterButton2);
					}
				});
				d.show();
			}
		};
		timeFilterButton.setOnClickListener(timeFilterClickListener);
		timeFilterButton2.setOnClickListener(timeFilterClickListener);
		
        refreshDateButtonText(dateFilterButton, dateFilterButton2);
        refreshTimeButtonText(timeFilterButton, timeFilterButton2);
        
        mFooterView = getLayoutInflater().inflate(R.layout.list_footer_loadingindicator, null);
        getListView().addFooterView(mFooterView, null, false);
        /* change reference to inner progress indicator since hiding the footer 
         * container itself doesn't resize the layout (black box stays visible) */
        mFooterView = mFooterView.findViewById(R.id.list_progress);
        getListView().setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				int lastInScreen = firstVisibleItem + visibleItemCount;
				if((lastInScreen == totalItemCount) && mBusDeparturesLoadingTask == null 
						&& mDeparturesListAdapter != null 
						&& !mDeparturesListAdapter.getItemList().isEmpty()
						&& mFooterView.getVisibility() == View.VISIBLE) {
					loadDepartures(mStopSelected, true);
				}
			}
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {}
        });
        
        mUpdateTimeTask = new CountdownUpdater(mHandler, this, REFRESH_INTERVAL);
    }
	
	private void refreshDateButtonText(Button... buttons) {
		String text;
		if(mFilter.isDateSet()) {
			text = getString(R.string.exam_details, mFilter.getDateTime());
		} else {
			text = getString(R.string.dates_today).toUpperCase(Locale.GERMAN);
		}
		for(Button b : buttons) {
			b.setText(text);
		}
	}
	
	private void refreshTimeButtonText(Button... buttons) {
		String text;
		if(mFilter.isTimeSet()) {
			text = getString(R.string.calendar_time, mFilter.getDateTime());
		} else {
			text = getString(R.string.dates_now).toUpperCase(Locale.GERMAN);
		}
		for(Button b : buttons) {
			b.setText(text);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        try {
			mDepartureMonitor = new DepartureMonitor();
		} catch (Exception e) {
			e.printStackTrace();
		}
        
		mStopSpinnerInitialized = false;
		final StopDB stopDB = new StopDB(this);
        ArrayAdapter<Stop> stopAdapter = new ArrayAdapter<Stop>(this, android.R.layout.simple_spinner_item, stopDB.getSelectedStops());
        stopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStopSpinner.setAdapter(stopAdapter);
        // load the previously selected stop or the default stop if it isn't part of the selected set
        mStopSelected = stopDB.getSelectedStop(Preferences.getStwSelectedStop(preferences, Stop.DefaultStop.UNIVERSITAET.getCode()));
        if(mStopSelected == null) {
        	// the previously selected stop isn't part of the selected set
        	// load the University stop as default replacement stop
        	mStopSelected = stopDB.getSelectedStop(Stop.DefaultStop.UNIVERSITAET.getCode());
        }
        if(mStopSelected != null) {
        	int position = stopAdapter.getPosition(mStopSelected);
        	if(position >= 0) {
        		// if the loaded stop is part of the selected set, select it in the spinner
        		// (otherwise the first entry in the list will be loaded)
        		mStopSpinner.setSelection(position);
        	}
        }
        
        // if the departure monitor gets loaded for the first time, the stops DB is empty
        // and needs to be populated with stops
        if(stopAdapter.isEmpty() && stopDB.isEmpty()) {
        	// get stops from the webservice and put them into the DB
        	new AsyncTask() {
        		
        		private ProgressDialog mProgressDialog;
        		
        		@Override
        		protected void onPreExecute() {
        			mProgressDialog = ProgressDialog.show(
        					BusDeparturesActivity.this, 
        					getString(R.string.bus_stops_loading), 
        					getString(R.string.bus_stops_loading_note), 
        					true, false);
        		}
            	
            	@Override
    			protected void doInBackground() throws Exception {
            		List<Stop> defaultStops = new ArrayList<Stop>();
    				for(Stop.DefaultStop stop : Stop.DefaultStop.values()) {
    					defaultStops.add(stop.getStop());
    				}
        			BusStopsActivity.initializeStopDB(stopDB, defaultStops);
    			}
            	
            	@Override
            	protected void onPostExecute() {
            		stopDB.close();
            		mProgressDialog.dismiss();
            	}
            	
            	@Override
    			protected void onException(Exception e) {
            		e.printStackTrace();
    				UIUtils.showSimpleErrorDialogAndClose(BusDeparturesActivity.this, e.getMessage());
    				Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_BUS_DEPARTURES, e);
    			}
    			
    			@Override
    			protected void onSuccess() {
    				// reload current activity
    				UIUtils.reloadActivity(BusDeparturesActivity.this);
    			}
            }.execute();
        } else {
        	stopDB.close();
        }
               
    	Stop stop = (Stop)mStopSpinner.getSelectedItem();
    	if(stop != null) {
    		loadDepartures(stop, false);
    	} else { 
    		if(mDeparturesListAdapter != null) {
    			mDepartures.getDepartures().clear();
    			mDeparturesListAdapter.notifyDataSetChanged();
    		}
    		findViewById(R.id.progress).setVisibility(View.GONE);
    	}
        
        if(!Preferences.isStwDisclaimerAccepted(preferences)) {
	        new AlertDialog.Builder(this)
	        .setTitle(R.string.attention)
			.setMessage(R.string.bus_departures_disclaimer)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
			.setNeutralButton(R.string.ok_dont_show_again,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Preferences.setStwDisclaimerAccepted(preferences, true);
							dialog.dismiss();
						}
					}).create().show();
        }
		
		Analytics.onActivityStart(this, Analytics.ACTIVITY_BUS_DEPARTURES);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// http://developer.android.com/resources/articles/timed-ui-updates.html
        // start the refresh cycle (also called if the screen goes on / out of standby)
		mUpdateTimeTask.setEnabled(true);
		mUpdateTimeTask.start();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Analytics.onActivityStop(this);

		// stop the refresh cycle
		mUpdateTimeTask.stop();
		mUpdateTimeTask.setEnabled(false);
	}
	
	private void loadDepartures(final Stop stop, final boolean more) {
		if(mBusDeparturesLoadingTask != null) {
			// another task instance is still processing
			return;
		}
		
		Log.d(TAG, "loading departures for " + stop);
		mBusDeparturesLoadingTask = new ProgressNotificationAsyncTask(
				new ProgressNotificationToggle() {
			@Override
			public void progressNotificationOn() {
				if(more) {
					mActionBar.progressNotificationOn();
				} else {
					BusDeparturesActivity.this.progressNotificationOn();
				}
				mUpdateTimeTask.stop();
			}
			@Override
			public void progressNotificationOff() {
				if(more) {
					mActionBar.progressNotificationOff();
				} else {
					BusDeparturesActivity.this.progressNotificationOff();
				}
				mUpdateTimeTask.start();
			}
		}) {
			
			private Departures mNewDepartures;
			private boolean mOverflow;
        	
        	@Override
			protected void doInBackground() throws Exception {
				if(!more) {
					mFilter.updateUnsetDateTime();
					mDepartures = mNewDepartures = mDepartureMonitor.queryMonitorForStop(stop, mFilter.getDateTime(), null);
					mUpdateTimeTask.signalUpdate();
					mDeparturesListAdapter = new DeparturesListAdapter(
							BusDeparturesActivity.this, mDepartures.getDepartures());
				} else {
					mNewDepartures = mDepartureMonitor.queryMonitorForStop(stop, 
							mDepartures.getLastDate(), null);
					
					// find out how many departures are overlapping and remove those duplicates
					/* The new result returns all departures at the specified time (HH:mm). The
					 * previous result contains at least 1 departure with that time, since that
					 * time is taken for the new query. So I need to count the departures of the
					 * query time in the previous results and omit this number of departures
					 * from the new result.
					 */
					int duplicates = 0;
					Date temp = mDepartures.getLastDate();
					for(int i = mDepartures.getDepartures().size() - 1; i >= 0; i--) {
						Date depTime = mDepartures.getDepartures().get(i).getTime();
						if(depTime.equals(temp)) {
							duplicates++;
						} else {
							break;
						}
					}
					
					/* Remove departures that are beyond the supported time interval of departures
					 * to be shown.
					 */
					int overflow = 0;
					long referenceTime = mDepartures.getFirstDate().getTime();
					for(int i = mNewDepartures.getDepartures().size() - 1; i >= 0; i--) {
						if(mNewDepartures.getDepartures().get(i).getTime().getTime() - referenceTime > DEPARTURES_INTERVAL) {
							overflow++;
						} else {
							break;
						}
					}
					
					Log.d(TAG, mNewDepartures.getDepartures().size() + " departures, " 
							+ duplicates + " duplicates, " + overflow + " overflow");
					
					if(overflow > 0) {
						mOverflow = true;
					}
					
					if(duplicates + overflow >= mNewDepartures.getDepartures().size()) {
						mNewDepartures.getDepartures().clear();
					} else {
						// remove duplicates from new list
						int numDepartures = mDepartures.getDepartures().size();
						for(int x = 0; x < duplicates; x++) {
							mNewDepartures.getDepartures().remove(
									mDepartures.getDepartures().get(numDepartures - x - 1));
						}
						
						// remove time overflow from newly loaded departures
						mNewDepartures.setDepartures(mNewDepartures.getDepartures()
								.subList(0, mNewDepartures.getDepartures().size() - overflow));

						// add new departures to the exisiting list
						mDepartures.getDepartures().addAll(mNewDepartures.getDepartures());
					}
				}
    			
    			// calculate countdown times (the countdown times that the departure monitor provides are off by
    			// a few minutes [probably by the offset between the departure monitor time field "now" in the root
    			// xml element and the actual time])
    			for(Departure departure : mNewDepartures.getDepartures()) {
    				// no need for floating point operations, just take the integer floor
    				// I rather display the departure time too early than too late to avoid missing a bus/train
    				departure.setCountdown((int)((departure.getTime().getTime() - mUpdateTimeTask.getLastUpdateTime()) / 60000));
    			}
			}
        	
        	@Override
        	protected void onPostExecute() {
        		mBusDeparturesLoadingTask = null;
        	}
        	
        	@Override
			protected void onException(Exception e) {
        		if(e instanceof DepartureMonitorException) {
        			new AlertDialog.Builder(BusDeparturesActivity.this)
        					.setIcon(android.R.drawable.ic_dialog_alert)
        					.setTitle("Error")
        					.setMessage(R.string.bus_departures_error)
        					.setNeutralButton(R.string.retry, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									refresh();
									dialog.dismiss();
								}
							}).create().show();
        		} else {
	        		e.printStackTrace();
					UIUtils.showSimpleErrorDialogAndClose(BusDeparturesActivity.this, e.getMessage());
					Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_BUS_DEPARTURES, e);
        		}
			}
			
			@Override
			protected void onSuccess() {
				if(!more) {
					setListAdapter(mDeparturesListAdapter);
					mStopSelected = stop;
				} else {
					mDeparturesListAdapter.notifyDataSetChanged();
				}
				// hide the footer loading indicator if there are no more departures to load
				if(mOverflow || mNewDepartures.getDepartures().isEmpty()) {
					mFooterView.setVisibility(View.GONE);
				}
			}
        };
		mBusDeparturesLoadingTask.execute();
	}
    
    @Override
	public void progressNotificationOn() {
    	if(mDeparturesListAdapter == null) {
    		findViewById(R.id.progress).setVisibility(View.VISIBLE);
    		mActionBar.progressNotificationOn();
    	}
	}

	@Override
	public void progressNotificationOff() {
		findViewById(R.id.progress).setVisibility(View.GONE);
		mActionBar.progressNotificationOff();
	}

	@Override
	public void refresh() {
		mDeparturesListAdapter = null;
		mFooterView.setVisibility(View.VISIBLE);
		if(mStopSelected != null) {
			loadDepartures(mStopSelected, false);
		}
	}
	
	private class DeparturesListAdapter extends GenericListAdapter<Departure> {

		public DeparturesListAdapter(ListActivity context, List<Departure> list) {
			super(context, list, R.layout.bus_departures_item);
		}

		@Override
		protected void updateView(int position, View itemView) {
			TextView stwLineNumberText = (TextView) itemView.findViewById(R.id.text_stw_line_number);
			TextView oebbLineNumberText = (TextView) itemView.findViewById(R.id.text_oebb_line_number);
            TextView directionText = (TextView) itemView.findViewById(R.id.text_direction);
            TextView dateText = (TextView) itemView.findViewById(R.id.text_date);
            TextView timeText = (TextView) itemView.findViewById(R.id.text_time);
            
            Departure departure = (Departure)getItem(position);
            
            int countdown = departure.getCountdown();
            Date departureTime = departure.getTime();
            Date today = new Date();
            Date tomorrow = new Date(today.getTime() + Utils.MILLIS_PER_DAY);
            Date yesterday = new Date(today.getTime() - Utils.MILLIS_PER_DAY);
            CharSequence departureTimeText = "";
            
            if(countdown > 0 && countdown <= 30) {
            	dateText.setVisibility(View.GONE);
            	departureTimeText = getString(R.string.bus_departure_time_near_future, countdown);
            } else if(countdown == 0) {
            	dateText.setVisibility(View.GONE);
            	departureTimeText = getString(R.string.bus_departure_time_now);
            } else if(countdown < 0 && countdown >= -30) {
            	dateText.setVisibility(View.GONE);
            	departureTimeText = getString(R.string.bus_departure_time_passed, countdown * -1);
            } else if(Utils.isSameDay(today, departureTime)) {
            	dateText.setVisibility(View.GONE);
            	departureTimeText = DateFormat.format(getString(R.string.time_format), departure.getTime());
            } else if(Utils.isSameDay(tomorrow, departureTime)) {
            	dateText.setVisibility(View.VISIBLE);
            	dateText.setText(getString(R.string.tomorrow));
            	departureTimeText = DateFormat.format(getString(R.string.time_format), departure.getTime());
            } else if(Utils.isSameDay(yesterday, departureTime)) {
            	dateText.setVisibility(View.VISIBLE);
            	dateText.setText(getString(R.string.yesterday));
            	departureTimeText = DateFormat.format(getString(R.string.time_format), departure.getTime());
            } else {
            	dateText.setVisibility(View.VISIBLE);
            	dateText.setText(DateFormat.format(getString(R.string.date_format), departure.getTime()));
            	departureTimeText = DateFormat.format(getString(R.string.time_format), departure.getTime());
            }
            
        	directionText.setText(getString(R.string.specialchar_arrowright) + " " + departure.getDirection());
        	timeText.setText(departureTimeText);
        	
        	if(departure.getNetwork().equals("stw")) {
        		stwLineNumberText.setVisibility(View.VISIBLE);
        		stwLineNumberText.setBackgroundColor(LineColors.getColor(departure.getLineNumber()));
        		stwLineNumberText.setText(departure.getLineNumber());
        		oebbLineNumberText.setVisibility(View.GONE);
        	} else if(departure.getNetwork().equals("obb")) {
        		// remove the line sponsor name
        		String lineNumber = departure.getLineNumber();
        		int substringEnd = lineNumber.indexOf(' ', lineNumber.indexOf(' ') + 1);
        		if(substringEnd > 0) {
        			lineNumber = lineNumber.substring(0, substringEnd);
        		}
        		
        		oebbLineNumberText.setVisibility(View.VISIBLE);
        		oebbLineNumberText.setText(lineNumber);
        		stwLineNumberText.setVisibility(View.GONE);
        	}
		}
		
		@Override
		public boolean areAllItemsEnabled() {
	        return false;
	    }

		@Override
	    public boolean isEnabled(int position) {
	        return false;
	    }
	}
	
	private static class CountdownUpdater implements Runnable {
		
		private Handler mHandler;
		private BusDeparturesActivity mActivity;
		private long mRefreshInterval;
		private long mLastUpdateTime;
		private boolean mEnabled;
		private boolean mActive;
		
		public CountdownUpdater(Handler handler, BusDeparturesActivity activity, 
				long refreshInterval) {
			mHandler = handler;
			mActivity = activity;
			mRefreshInterval = refreshInterval;
		}
		
		@Override
    	public void run() {
    		Log.d(TAG, "countdown refresh");
    		if(mActivity.mDeparturesListAdapter != null && mActivity.mDepartures != null 
    				&& !mActivity.mDepartures.getDepartures().isEmpty() && mLastUpdateTime > 0) {
    			int minutesDelta = Math.round((float)(System.currentTimeMillis() - mLastUpdateTime) / 60000);
    			signalUpdate();
    			for(Departure departure : mActivity.mDepartures.getDepartures()) {
					// update the countdown (subtract 1 minute)
					departure.setCountdown(departure.getCountdown() - minutesDelta);
				}
				mActivity.mDeparturesListAdapter.notifyDataSetChanged();
    		}
    		mHandler.postDelayed(this, mRefreshInterval);
    	}
		
		public void setEnabled(boolean enabled) {
			mEnabled = enabled;
		}
		
		public void start() {
			if(mEnabled && !mActive) {
				mHandler.postDelayed(this, mRefreshInterval);
				mActive = true;
				Log.d(TAG, "countdown started");
			} else {
				Log.d(TAG, "countdown start blocked");
			}
		}
		public void stop() {
			mHandler.removeCallbacks(this);
			mActive = false;
			Log.d(TAG, "countdown stopped");
		}
		
		public void signalUpdate() {
			mLastUpdateTime = System.currentTimeMillis();
		}
		
		public long getLastUpdateTime() {
			return mLastUpdateTime;
		}
	}
	
	private static class Filter {
		
		private boolean mDateSet;
		private boolean mTimeSet;
		
		private Calendar mCalendar;
		
		public Filter() {
			mCalendar = Calendar.getInstance();
		}
		
		public boolean isDateSet() {
			return mDateSet;
		}
		
		public void resetDate() {
			mDateSet = false;
		}
		
		public boolean isTimeSet() {
			return mTimeSet;
		}
		
		public void resetTime() {
			mTimeSet = false;
		}
		
		public void updateUnsetDateTime() {
			Calendar c = Calendar.getInstance();
			if(!isDateSet()) {
				setDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
				resetDate();
			}
			if(!isTimeSet()) {
				setTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
				resetTime();
			}
		}
		
		public int getYear() {
			return mCalendar.get(Calendar.YEAR);
		}
		
		public int getMonth() {
			return mCalendar.get(Calendar.MONTH);
		}
		
		public int getDay() {
			return mCalendar.get(Calendar.DAY_OF_MONTH);
		}
		
		public int getHour() {
			return mCalendar.get(Calendar.HOUR_OF_DAY);
		}
		
		public int getMinute() {
			return mCalendar.get(Calendar.MINUTE);
		}
		
		public void setDate(int year, int month, int day) {
			mCalendar.set(Calendar.YEAR, year);
			mCalendar.set(Calendar.MONTH, month );
			mCalendar.set(Calendar.DAY_OF_MONTH, day);
			mDateSet = true;
		}
		
		public void setTime(int hour, int minute) {
			mCalendar.set(Calendar.HOUR_OF_DAY, hour);
			mCalendar.set(Calendar.MINUTE, minute);
			mTimeSet = true;
		}
		
		public Date getDateTime() {
			return mCalendar.getTime();
		}
	}
}
