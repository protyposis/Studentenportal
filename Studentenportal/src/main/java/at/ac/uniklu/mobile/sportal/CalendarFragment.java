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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import at.ac.uniklu.mobile.sportal.api.Termin;
import at.ac.uniklu.mobile.sportal.model.CalendarModel;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.ui.AsyncTask;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.ui.SectionedListAdapter;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.MapUtils;
import at.ac.uniklu.mobile.sportal.util.UIUtils;
import at.ac.uniklu.mobile.sportal.util.Utils;

public class CalendarFragment extends ListFragment implements ProgressNotificationToggle, Refreshable, OnScrollListener {
	
	private class CalendarSLA extends SectionedListAdapter<Termin, Date> {
    	
		private Date mYearSwitch;
		private CharSequence mLabelNow;
		private CharSequence mLabelCancelled;
		
		private class HeaderViewHolder {
			public TextView text1;
			public TextView text2;
		}
		
		private class ItemViewHolder {
			public TextView timeText;
			public TextView roomText;
			public TextView titleText;
			public TextView labelText;
		}
		
		public CalendarSLA(Context context, List<Termin> list) {
			super(context, list, new CalendarSLA.HeaderDataExtractor<Termin, Date>() {
				private Calendar mCalendar = Calendar.getInstance();
				@Override
				public Date extract(Termin itemData) {
					mCalendar.setTime(itemData.getDatum());
					mCalendar.set(Calendar.HOUR_OF_DAY, 0);
					mCalendar.set(Calendar.MINUTE, 0);
					mCalendar.set(Calendar.SECOND, 0);
					mCalendar.set(Calendar.MILLISECOND, 0);
					return mCalendar.getTime();
				}
	    	}, R.layout.calendar_section_header, R.layout.calendar_date);
			
			Calendar c = Calendar.getInstance();
			c.set(c.get(Calendar.YEAR) + 1, Calendar.JANUARY, 1, 0, 0, 0);
			mYearSwitch = c.getTime();
			
			mLabelNow = context.getString(R.string.dates_now).toUpperCase(Locale.GERMAN);
			mLabelCancelled = context.getString(R.string.dates_cancelled).toUpperCase(Locale.GERMAN);
		}

		@Override
		public void updateHeaderView(View headerView, Date headerData) {
			HeaderViewHolder viewHolder;
			if(headerView.getTag() == null) {
				viewHolder = new HeaderViewHolder();
				viewHolder.text1 = (TextView)headerView.findViewById(R.id.text1);
				viewHolder.text2 = (TextView)headerView.findViewById(R.id.text2);
				headerView.setTag(viewHolder);
			} else {
				viewHolder = (HeaderViewHolder)headerView.getTag();
			}
			
			String day = getString(R.string.calendar_date_dayname, headerData);
			if(Utils.isToday(headerData)) {
				day = getString(R.string.dates_today);
			} else if(Utils.isTomorrow(headerData)) {
				day = getString(R.string.dates_tomorrow);
			}

			viewHolder.text1.setText((headerData.after(mYearSwitch) ? 
					getString(R.string.calendar_date_dmy, headerData) : 
						getString(R.string.calendar_date_dm, headerData)).toUpperCase(Locale.GERMAN));
			viewHolder.text2.setText(day.toUpperCase(Locale.GERMAN));
		}

		@Override
		public void updateItemView(View itemView, Termin itemData) {
			ItemViewHolder viewHolder;
			if(itemView.getTag() == null) {
				viewHolder = new ItemViewHolder();
				viewHolder.timeText = (TextView)itemView.findViewById(R.id.text_time);
				viewHolder.roomText = (TextView)itemView.findViewById(R.id.text_room);
				viewHolder.titleText = (TextView)itemView.findViewById(R.id.text_title);
				viewHolder.labelText = (TextView)itemView.findViewById(R.id.text_label);
	            itemView.setTag(viewHolder);
			} else {
				viewHolder = (ItemViewHolder)itemView.getTag();
			}
            
			Date from = itemData.getDatum();
			Date to = itemData.getEndDate();
			
			viewHolder.timeText.setText(getString(R.string.calendar_timespan, from, to));
			viewHolder.roomText.setText(itemData.getRaum());
			MapUtils.linkifyRooms(viewHolder.roomText);
			viewHolder.titleText.setText(itemData.getTitleWithType());
			viewHolder.titleText.setEnabled(!itemData.isPast());
			
			if(itemData.isStorniert()) {
				viewHolder.labelText.setText(mLabelCancelled);
				viewHolder.labelText.setBackgroundResource(R.color.date_canceled);
				viewHolder.labelText.setVisibility(View.VISIBLE);
			} else if(itemData.isNow()) {
				viewHolder.labelText.setText(mLabelNow);
				viewHolder.labelText.setBackgroundResource(R.color.date_running);
				viewHolder.labelText.setVisibility(View.VISIBLE);
			} else {
				viewHolder.labelText.setVisibility(View.GONE);
			}
		}
    	
    }
	
	private CalendarSLA mCalendarAdapter;
    private CalendarModel mCalendarModel;
    private boolean mLoading = false;
    private boolean mDashboardMode = false;
    
    private View mFooterView;
	private View mProgressView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.calendar_fragment, container, false);
		mProgressView = v.findViewById(R.id.progress);
		mFooterView = inflater.inflate(R.layout.list_footer_loadingindicator, null);
		
		ListView listView = (ListView)v.findViewById(android.R.id.list);
		listView.addFooterView(mFooterView, null, false);
		listView.setOnScrollListener(this);
		
		/* change reference to inner progress indicator since hiding the footer 
         * container itself doesn't resize the layout (black box stays visible) */
		mFooterView = mFooterView.findViewById(R.id.list_progress);

		return v;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if(getActivity() instanceof DashboardActivity) {
			mDashboardMode = true;
		}
		
		if(mCalendarModel == null)
			refresh();
		else if(!mCalendarModel.isMoreDatesToLoad())
			mFooterView.setVisibility(View.GONE);
	}

	@Override
	public void progressNotificationOn() {
    	mProgressView.setVisibility(View.VISIBLE);
	}

	@Override
	public void progressNotificationOff() {
		mProgressView.setVisibility(View.GONE);
	}
	
	@Override
	public void refresh() {
		refresh(false);
	}
	
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		/* don't do anything if the list is empty - there can't be nothing to load (yet) */
		if(totalItemCount == 0) 
			return;
		
		int lastInScreen = firstVisibleItem + visibleItemCount;
		if((lastInScreen == totalItemCount) && mLoading == false
				&& mCalendarModel != null
				&& mCalendarModel.isMoreDatesToLoad()
				&& mFooterView.getVisibility() == View.VISIBLE) {
			refresh(true);
		}
	}
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {}
	
	public void refresh(final boolean more) {
		mLoading = true;
		new AsyncTask() {
			
			protected void onPreExecute() {
				if(!more) {
					progressNotificationOn();
					mCalendarAdapter = null;
				}
			};
        	
        	@Override
			protected void doInBackground() throws Exception {
        		if(mDashboardMode) {
        			mCalendarModel = ModelService.getDashboardModel(getActivity());
        		} else {
        			mCalendarModel = ModelService.getCalendarModel(getActivity(), more ? mCalendarModel : null);
        		}
			}
        	
        	protected void onPostExecute() {
        		mLoading = false;
        		if(!more) progressNotificationOff();
        	};
        	
        	@Override
			protected void onException(Exception e) {
        		if(isAdded()) {
        			UIUtils.processActivityRefreshException(e, getActivity());
        		}
        		Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_CALENDAR, e);
			}
			
			@Override
			protected void onSuccess() {
				if(!isAdded()) {
		    		/* If the parent activity is already closed the fragment isn't added any more,
		    		 * and further processing is not only pointless overhead, but results in a NPE. */
		    		// http://stackoverflow.com/questions/8289345/fragments-being-replaced-while-asynctask-is-executed-nullpointerexception-on-g#comment10547325_8290847
		    		return;
		    	}
				if(!mCalendarModel.isMoreDatesToLoad()) {
					mFooterView.setVisibility(View.GONE);
				}
				initListAdapter();
			}
			
		}.execute();
	}
	
    private void initListAdapter() {
    	if(mCalendarAdapter == null) {
    		mCalendarAdapter = new CalendarSLA(getActivity(), mCalendarModel.getDates());
    		setListAdapter(mCalendarAdapter);
    	} else {
    		mCalendarAdapter.notifyDataSetChanged();
    	}
    }
}
