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
import android.app.ProgressDialog;

public class ProgressNotificationDialog implements ProgressNotificationToggle {

	private Activity mActivity;
	private int mMessage;
	private ProgressDialog mProgressDialog;
	
	public ProgressNotificationDialog(Activity activity, int message) {
		mActivity = activity;
		mMessage = message;
	}
	
	@Override
	public void progressNotificationOn() {
		mProgressDialog = ProgressDialog.show(mActivity, null, mActivity.getString(mMessage), true, false);
	}
	
	@Override
	public void progressNotificationOff() {
		mProgressDialog.dismiss();
	}

}
