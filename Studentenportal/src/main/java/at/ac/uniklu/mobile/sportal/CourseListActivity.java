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
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import at.ac.uniklu.mobile.sportal.CourseListFragment.CourseListFragmentEventListener;
import at.ac.uniklu.mobile.sportal.model.CourseModel;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.ui.FragmentPagerSupport;
import at.ac.uniklu.mobile.sportal.ui.FragmentRefreshable;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationAsyncTask;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.ui.viewpagerindicator.TitlePageIndicator;
import at.ac.uniklu.mobile.sportal.ui.viewpagerindicator.TitleProvider;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class CourseListActivity extends FragmentActivity 
		implements ProgressNotificationToggle, Refreshable, FragmentPagerSupport,
		CourseListFragmentEventListener {
	
	public static final String COURSE_KEY = "course_key";
	public static final String COURSE_NAME = "course_name";
	
	private static final String TAG = "CourseListActivity";
	private static final boolean DEBUG = false;
	private static final String IS_CURRENT_PAGE_INDEX = "currentPageIndex";

	private CourseModel mCourseModel;
	private List<String> mFragmentRefreshRegistry;
	private SemesterPagerAdapter mAdapter;
	private ViewPager mPager;
	private TitlePageIndicator mTitlePageIndicator;
	private View mProgressView;
	private int mCurrentPageIndex;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.course_list);
        new ActionBarHelper(this).setupHeader();
        
        mFragmentRefreshRegistry = new ArrayList<String>();

        mAdapter = new SemesterPagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
		mTitlePageIndicator = (TitlePageIndicator)findViewById(R.id.titles);
		mTitlePageIndicator.setViewPager(mPager);
		mTitlePageIndicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				mCurrentPageIndex = position;
			}
		});
		mProgressView = findViewById(R.id.progress);

		if(savedInstanceState != null) {
			mCurrentPageIndex = savedInstanceState.getInt(IS_CURRENT_PAGE_INDEX);
		}

		refresh();
    }
	
	@Override
	public void onStart() {
		if(DEBUG) Log.d(TAG, "onStart");
		super.onStart();
		Analytics.onActivityStart(this, Analytics.ACTIVITY_COURSES);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Analytics.onActivityStop(this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	UIUtils.executeActivityRefresh(requestCode, this);
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
		new ProgressNotificationAsyncTask(this) {

			@Override
			protected void doInBackground() throws Exception {
				mCourseModel = ModelService.getCourseModel();
			}
			
			@Override
			protected void onSuccess() {
				if(DEBUG) Log.d(TAG, "model loaded");
				mAdapter.notifyDataSetChanged();
				mTitlePageIndicator.notifyDataSetChanged();
		        mPager.setCurrentItem(mCurrentPageIndex, false);
		        refreshFragments();
			};
			
			@Override
			protected void onException(Exception e) {
				UIUtils.processActivityRefreshException(e, CourseListActivity.this);
				Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_COURSES, e);
			};
			
		}.execute();
	}
	
	public CourseModel getCourseModel() {
		return mCourseModel;
	}
	
	@Override
	public void registerFragmentForRefresh(FragmentRefreshable fragment) {
		if(DEBUG) Log.d(TAG, "fragment registered: " + fragment.getTag());
		/* Store the tag of the fragment so it can be loaded later from the
		 * FragmentManager. I don't know if it would be save to directly save
		 * references to the fragment (or maybe WeakReference is needed) so this
		 * isn't the most effective way but save to not produce any exceptions.
		 */
		mFragmentRefreshRegistry.add(fragment.getTag());
	}
	
	@Override
	public void refreshFragments() {
		FragmentManager fm = getSupportFragmentManager();
		for(String tag : mFragmentRefreshRegistry) {
			Fragment f = fm.findFragmentByTag(tag);
			if(DEBUG) Log.d(TAG, "fragment refresh: " + tag);
			if(f != null) ((FragmentRefreshable)f).refresh();
		}
		mFragmentRefreshRegistry.clear();
	}
	
	@Override
	public int getCurrentItemIndex() {
		return mCurrentPageIndex;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(IS_CURRENT_PAGE_INDEX, mCurrentPageIndex);
	}
	
	private class SemesterPagerAdapter extends FragmentPagerAdapter 
			implements TitleProvider {

		public SemesterPagerAdapter(FragmentManager fm) {
			super(fm);
			if(DEBUG) FragmentManager.enableDebugLogging(true);
		}
		
		@Override
		public int getCount() {
			return mCourseModel != null ? mCourseModel.getSemester().size() : 0;
		}
		
		@Override
		public Fragment getItem(int position) {
			if(DEBUG) Log.d("SemesterPagerAdapter", "getItem " + position);
			CourseListFragment fragment = new CourseListFragment();
			
			Bundle arguments = new Bundle();
			arguments.putInt(CourseListFragment.ARGUMENT_INDEX, position);
            fragment.setArguments(arguments);
            
			return fragment;
		}

		@Override
		public String getTitle(int position) {
			return UIUtils.getTermName(
					mCourseModel.getSemester().get(position).getKey(), 
					CourseListActivity.this).toUpperCase(Locale.GERMAN);
		}
	}

	@Override
	public void onBlacklistChanged() {
		Studentportal.getSportalClient().clearTermine();
		setResult(Refreshable.REQUEST_REFRESH); // too late to call it in OnPause
	}
}
