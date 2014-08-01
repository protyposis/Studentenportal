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

import java.lang.ref.WeakReference;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Can be used to handle and show a progress dialog over activity recreation steps by using
 * onRetainNonConfigurationInstance(). This class avoids memory leaks and other context related
 * exceptions.
 * 
 * To save the dialog in onRetainNonConfigurationInstance() call: {@link #clean()}
 * To restore it in onCreate after accessing it through getLastNonConfigurationInstance() call: {@link #restore(Context)}
 * 
 * @see http://android-developers.blogspot.com/2009/02/faster-screen-orientation-change.html
 */
public class ProgressDialogDefinition {
	
	private CharSequence mTitle;
	private CharSequence mMessage;
	private boolean mIndeterminate;
	private boolean mCancelable;
	
	private WeakReference<ProgressDialog> mProgressDialog;
	
	public ProgressDialogDefinition(CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable) {
		mTitle = title;
		mMessage = message;
		mIndeterminate = indeterminate;
		mCancelable = cancelable;
	}
	
	public void show(Context context) {
		mProgressDialog = new WeakReference<ProgressDialog>(ProgressDialog.show(context, mTitle, mMessage, mIndeterminate, mCancelable));
	}
	
	/**
	 * Returns true if the dialog has been cleaned although it should actually be visible to the user.
	 * @return true if the dialog should be visible to the user, else false
	 */
	public boolean shouldBeShowing() {
		return mProgressDialog != null;
	}
	
	public boolean isShowing() {
		return !(mProgressDialog == null || mProgressDialog.get() == null || !mProgressDialog.get().isShowing());
	}
	
	public void dismiss() {
		clean();
		mProgressDialog = null;
	}
	
	/**
	 * Removes the progress dialog from the context when the activity is going to reload, e.g. on a orientation change.
	 */
	public ProgressDialogDefinition clean() {
		if(isShowing()) {
			mProgressDialog.get().dismiss();
		}
		return this;
	}
	
	/**
	 * Brings back the progress dialog after it has been clean from an activity.
	 * @param context
	 */
	public void restore(Context context) {
		if(shouldBeShowing()) {
			show(context);
		}
	}

}
