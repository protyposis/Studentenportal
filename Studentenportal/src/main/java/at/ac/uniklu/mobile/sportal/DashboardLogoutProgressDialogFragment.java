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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;
import at.ac.uniklu.mobile.sportal.notification.GCMStatusBroadcastCommunicator;
import at.ac.uniklu.mobile.sportal.notification.GCMUtils;
import at.ac.uniklu.mobile.sportal.service.MutingService;
import at.ac.uniklu.mobile.sportal.ui.AsyncTask;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.Preferences;

public class DashboardLogoutProgressDialogFragment extends DialogFragment {
	
	private GCMStatusBroadcastCommunicator mGCMCommunicator;
	private AsyncTask mLogoutTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true); // see onDestroyView as well
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		ProgressDialog d = new ProgressDialog(getActivity());
		d.setIndeterminate(true);
		d.setCancelable(false);
		d.setMessage(getString(R.string.work_logout));
		return d;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if(mGCMCommunicator == null) {
			mGCMCommunicator = new GCMStatusBroadcastCommunicator() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if(intent.hasExtra(GCMStatusBroadcastCommunicator.EXTRA_GCM_UP)) {
						runLogoutTask();
					}
				}
			};
			/* Unregister from GCM.
			 * If there's no GCM registration, immediately start the logout task... otherwise
			 * wait for GCM to finish the unregistering process and then start the task (see above).
			 */
			if(!GCMUtils.unregisterIfRegistered(getActivity().getApplicationContext())) {
				runLogoutTask();
			}
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mGCMCommunicator.registerReceiver(getActivity());
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mGCMCommunicator.unregisterReceiver(getActivity());
	}
	
	@Override
	public void onDestroyView() {
		// http://stackoverflow.com/questions/8235080/fragments-dialogfragment-and-screen-rotation
		// http://code.google.com/p/android/issues/detail?id=17423
		if (getDialog() != null && getRetainInstance()) getDialog().setDismissMessage(null);
		super.onDestroyView();
	}
	
	private void runLogoutTask() {
		if(mLogoutTask != null) return; // the task is already running
		
		// http://stackoverflow.com/a/11509669
		mLogoutTask = new AsyncTask() {

			@Override
			protected void doInBackground() throws Exception {
				
				// give the user the impression that something elaborate is going on :)
				Thread.sleep(2000);
				
				Context appContext = getActivity().getApplicationContext();
				
				Studentportal.getSportalClient().logout();

				// clear data
				Studentportal.clearCaches(appContext);
				Studentportal.getSportalClient().clearSessionCookie();
				
				// reset preferences
				Editor editor = PreferenceManager.getDefaultSharedPreferences(appContext).edit();
				Preferences.setSavePassword(editor, false);
				Preferences.setPassword(editor, null);
				Preferences.setAutomuteEnabled(appContext, editor, false);
				editor.commit();
				
				// stop mutingservice
				getActivity().startService(new Intent(getActivity(), MutingService.class)
						.putExtra(MutingService.ACTION,  MutingService.ACTION_TURN_OFF));
			}
			
			@Override
			protected void onPostExecute() {
				dismiss();
			}
			
			@Override
			protected void onException(Exception e) {
				Analytics.onError(Analytics.ERROR_LOGOUT_FAILED, e);
				Toast.makeText(getActivity(), R.string.error_logout_failed, Toast.LENGTH_SHORT).show();
			}
			
			@Override
			protected void onSuccess() {
				Analytics.onEvent(Analytics.EVENT_LOGOUT);
				getActivity().finish();
			}
		};
		mLogoutTask.execute();
	}
}
