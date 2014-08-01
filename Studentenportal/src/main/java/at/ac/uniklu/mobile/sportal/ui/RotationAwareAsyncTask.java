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

package at.ac.uniklu.mobile.sportal.ui;

import android.app.Activity;
import android.util.Log;

public abstract class RotationAwareAsyncTask<T extends Activity> extends ProgressNotificationAsyncTask {
	
	private static final String TAG = RotationAwareAsyncTask.class.getSimpleName();
	
	/**
	 * Should be user by implementing methods instead of directly referencing
	 * the Activity when implemented as inner class.
	 */
	protected T mActivity;

	public RotationAwareAsyncTask(ProgressNotificationToggle toggle, T activity) {
		super(toggle);
		attach(activity);
	}
	
	@Override
	boolean shouldPostExecute() {
		if(mActivity != null) {
			return true;
		} else {
			Log.w(TAG, "activity missing - skipping postExecute");
			return false;
		}
	}
	
	/**
	 * Should be called when rebuilding the activity with retained data.
	 * @return true if the task is still executing, else false
	 */
	public boolean attach(T activity) {
		mActivity = activity;
		if(activity instanceof ProgressNotificationToggle) {
			mToggle = (ProgressNotificationToggle)activity;
		}
		Log.d(TAG, "isExecuting: " + isExecuting());
		if(isExecuting()) {
			progressNotificationOn();
			return true;
		}
		return false;
	}
	
	public void attach(ProgressNotificationToggle toggle) {
		mToggle = toggle;
	}
	
	/**
	 * Should be called in onRetainNonConfigurationInstance
	 */
	public void detach() {
		mActivity = null;
		mToggle = null; // avoid leaks
	}
}
