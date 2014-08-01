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

import java.util.List;
import java.util.Locale;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.mensa.Mensa;
import at.ac.uniklu.mobile.sportal.mensa.MensaMenus;
import at.ac.uniklu.mobile.sportal.mensa.MenuCategory;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationAsyncTask;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.ui.viewpagerindicator.TitlePageIndicator;
import at.ac.uniklu.mobile.sportal.ui.viewpagerindicator.TitleProvider;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class MensaActivity extends FragmentActivity 
		implements ProgressNotificationToggle, Refreshable, OnItemSelectedListener {
	
	private static final String TAG = "MensaActivity";
	
	private ActionBarHelper mActionBar;
	private View mMenuView;
	private ViewPager mPager;
	private TitlePageIndicator mTitlePageIndicator;
	private TextView mEmptyText;
	private View mProgressView;
	private Spinner mMensaSpinner;

	private MensaDataLoadTask mMensaDataLoadTask;
	private List<Mensa> mMensas;
	private int mCurrentMensaIndex;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mensa);
		
		mActionBar = new ActionBarHelper(this).setupHeader().addActionRefresh();
		mTitlePageIndicator = (TitlePageIndicator)findViewById(R.id.titles);
		mPager = (ViewPager)findViewById(R.id.pager);
		mMensaSpinner = (Spinner)findViewById(R.id.mensen);
		mMenuView = findViewById(R.id.mensamenu);
		mEmptyText = (TextView)findViewById(android.R.id.empty);
		mProgressView = findViewById(R.id.progress);

		mMensaSpinner.setOnItemSelectedListener(this);
		mMensaSpinner.setVisibility(View.GONE);
		
		@SuppressWarnings("unchecked")
		List<Mensa> retained = (List<Mensa>)getLastCustomNonConfigurationInstance();
		if(retained != null) {
			mMensas = retained;
			Log.d(TAG, "retained data");
		}
		
		if(savedInstanceState != null) {
			mCurrentMensaIndex = savedInstanceState.getInt("mCurrentMensaIndex");
			Log.d(TAG, "restored " + mCurrentMensaIndex);
		}

		refresh(false);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Analytics.onActivityStart(this, Analytics.ACTIVITY_MENSA);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Analytics.onActivityStop(this);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("mCurrentMensaIndex", mCurrentMensaIndex);
	}
	
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		if(mMensaDataLoadTask != null) {
			mMensaDataLoadTask.cancel(true);
		}
		return mMensas;
	};
	
	@Override
	public void progressNotificationOn() {
		mProgressView.setVisibility(View.VISIBLE);
		mActionBar.progressNotificationOn();
	}

	@Override
	public void progressNotificationOff() {
		mProgressView.setVisibility(View.GONE);
		mActionBar.progressNotificationOff();
	}
	
	@Override
	public void refresh() {
		Analytics.onEvent(Analytics.EVENT_MENSA_REFRESH, "mensaIndex", mCurrentMensaIndex+"");
		refresh(true);
	}

	public void refresh(boolean force) {
		if(force) {
			mMensas = null;
		}
		
		if(mMensas != null) {
			// Mensa data has already been loaded, just refresh the pager
			refreshActionBar();
			refreshPager();
			return;
		}
		
		Log.d(TAG, "no data available, launching loader");
		
		mMensaDataLoadTask = new MensaDataLoadTask(this);
		mMensaDataLoadTask.execute();
	}
	
	private void refreshActionBar() {
		ArrayAdapter<Mensa> mensaAdapter = new ArrayAdapter<Mensa>(MensaActivity.this, 
				R.layout.actionbar_simple_spinner_item, mMensas);
		mensaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mMensaSpinner.setAdapter(mensaAdapter);
		mMensaSpinner.setSelection(mCurrentMensaIndex);
		mActionBar.setTitleVisibility(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
		mMensaSpinner.setVisibility(View.VISIBLE);
	}
	
	private void refreshPager() {
		if(mMensas == null)  {
			return; // if no data available, there's nothing to display in the pager
		} 
		else {
			Log.d(TAG, "refreshPager: " + mCurrentMensaIndex);
			Mensa m = mMensas.get(mCurrentMensaIndex);
			
			if(m.categories == null) {
				mEmptyText.setText(R.string.mensa_loading_error);
				mEmptyText.setVisibility(View.VISIBLE);
				mMenuView.setVisibility(View.GONE);
			} 
			else if(m.categories.isEmpty()) {
				mEmptyText.setText(R.string.mensa_closed);
				mEmptyText.setVisibility(View.VISIBLE);
				mMenuView.setVisibility(View.GONE);
			} 
			else {
				mMenuView.setVisibility(View.VISIBLE);
				mEmptyText.setVisibility(View.GONE);
				mPager.setCurrentItem(0, false);
				mPager.setAdapter(new MensaPagerAdapter(getSupportFragmentManager(), m));
				mTitlePageIndicator.setViewPager(mPager);
			}
		}
		
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		// do not refresh pager content if the selected mensa is the currently displayed mensa (pointless processing overhead)
		if(position != mCurrentMensaIndex) {
			Log.d(TAG, "selected " + position);
			mCurrentMensaIndex = position;
			refreshPager();
			Analytics.onEvent(Analytics.EVENT_MENSA_SELECT, "mensaIndex", mCurrentMensaIndex+"");
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// nothing to do here
	}
	
	public MenuCategory getMenuCategory(int index) {
		return mMensas.get(mCurrentMensaIndex).categories.get(index);
	}
	
	public boolean isDataAvailable() {
		return mMensas != null;
	}
	
	private class MensaPagerAdapter extends FragmentPagerAdapter 
			implements TitleProvider {
		
		private Mensa mMensa;

		public MensaPagerAdapter(FragmentManager fm, Mensa mensa) {
			super(fm);
			mMensa = mensa;
		}

		@Override
		public int getCount() {
			if(mMensa.categories == null) return 0;
			return mMensa.categories.size();
		}

		@Override
		public Fragment getItem(int position) {
			MensaMenuFragment fragment = new MensaMenuFragment();

			Bundle arguments = new Bundle();
			arguments.putInt(MensaMenuFragment.ARGUMENT_INDEX, position);
			fragment.setArguments(arguments);

			return fragment;
		}

		@Override
		public String getTitle(int position) {
			return mMensa.categories.get(position).title.toUpperCase(Locale.GERMAN);
		}
	}
	
	private class MensaDataLoadTask extends ProgressNotificationAsyncTask {
		
		public MensaDataLoadTask(ProgressNotificationToggle toggle) {
			super(toggle);
		}

		@Override
		protected void doInBackground() throws Exception {
			mMensas = MensaMenus.retrieve();
		}
		
		@Override
		protected void onSuccess() {
			try {
				refreshActionBar();
				/* refreshPager needs to be called twice because a single call after a configuration change
				 * doesn't provoke the recreation, and thus data population, of the menu fragments.
				 * I cannot discover the problem :/
				 */
				refreshPager();
				refreshPager();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
		
		@Override
		protected void onException(Exception e) {
			UIUtils.processActivityRefreshException(e, MensaActivity.this);
			Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_MENSA, e);
		};
	}
}
