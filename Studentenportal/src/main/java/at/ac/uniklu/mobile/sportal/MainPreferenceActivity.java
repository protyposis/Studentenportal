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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;
import at.ac.uniklu.mobile.sportal.notification.GCMStatusBroadcastCommunicator;
import at.ac.uniklu.mobile.sportal.notification.GCMUtils;
import at.ac.uniklu.mobile.sportal.service.MutingService;
import at.ac.uniklu.mobile.sportal.service.ServiceToActivityBroadcastReceiver;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.Preferences;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class MainPreferenceActivity extends PreferenceActivity 
		implements OnSharedPreferenceChangeListener {
	
	private ServiceToActivityBroadcastReceiver mServiceBroadcastReceiver;
	private GCMStatusBroadcastCommunicator mGCMCommunicator;
	private ComponentName mDepMonLauncherComponentName;
	private AlertDialog mDepMonLauncherInfoDialog;
	private AlertDialog mPasswordInfoDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.main_preferences);
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		
		// init departure monitor launcher icon switching stuff
		mDepMonLauncherComponentName = new ComponentName(getApplicationContext(), BusDeparturesLaunchActivity.class);
		PackageManager pm = getApplicationContext().getPackageManager();
		CheckBoxPreference departureMonitorLauncherCheckBox = (CheckBoxPreference)getPreferenceScreen()
				.findPreference(getString(R.string.preference_departuremonitor_launchericon_key));
		departureMonitorLauncherCheckBox.setChecked(
				pm.getComponentEnabledSetting(mDepMonLauncherComponentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
		departureMonitorLauncherCheckBox.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean isChecked = (Boolean)newValue;
				PackageManager pm = getApplicationContext().getPackageManager();
				pm.setComponentEnabledSetting(mDepMonLauncherComponentName,
				        isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				        PackageManager.DONT_KILL_APP);
				mDepMonLauncherInfoDialog = UIUtils.showSimpleAlertDialog(MainPreferenceActivity.this,
						android.R.drawable.ic_dialog_info,
						R.string.attention, 
						R.string.preference_departuremonitor_launchericon_change_notice);
				return true;
			}
		});
		
		mServiceBroadcastReceiver = new ServiceToActivityBroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				super.onReceive(context, intent);
				if(intent.hasExtra(MutingService.ACTION_RESPONSE_SHUTDOWN)) {
					// uncheck the automute checkbox if the service has been shut down
					CheckBoxPreference automuteCheckBox = (CheckBoxPreference)getPreferenceScreen()
							.findPreference(getString(R.string.preference_automute_key));
					automuteCheckBox.setChecked(false);
				}
			}
		};
		
		mGCMCommunicator = new GCMStatusBroadcastCommunicator() {
			@Override
			public void onReceive(Context context, Intent intent) {
				CheckBoxPreference notificationsCheckBox = (CheckBoxPreference)getPreferenceScreen()
						.findPreference(getString(R.string.preference_notifications_key));
				notificationsCheckBox.setChecked(intent.getBooleanExtra(GCMStatusBroadcastCommunicator.EXTRA_GCM_UP, false));
			}
		};
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Analytics.onActivityStart(this, Analytics.ACTIVITY_PREFERENCES);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mServiceBroadcastReceiver.registerReceiver(this);
		mGCMCommunicator.registerReceiver(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals(getString(R.string.preference_automute_key))) {
			// start/stop the automute service depending on the chosen preference
			// only start the service if the password is saved (since it needs to connect to the server occasionally)
			boolean isAutomuteTurnedOn = sharedPreferences.getBoolean(key, false);
			boolean isPasswordSaved = Preferences.isSavePassword(sharedPreferences);
			if(isAutomuteTurnedOn && !isPasswordSaved) {
				mPasswordInfoDialog = UIUtils.showSimpleAlertDialog(this, android.R.drawable.ic_dialog_info, getString(R.string.info), getString(R.string.error_password_not_saved));
				CheckBoxPreference automuteCheckBox = (CheckBoxPreference)getPreferenceScreen().findPreference(key);
				sharedPreferences.edit().putBoolean(key, false).commit();
				automuteCheckBox.setChecked(false);
			} else {
				startService(new Intent(this, MutingService.class)
						.putExtra(MutingService.ACTION, isAutomuteTurnedOn ? MutingService.ACTION_TURN_ON : MutingService.ACTION_TURN_OFF));
			}
		} 
		else if(key.equals(getString(R.string.preference_notifications_key))) {
			boolean isNotificationsTurnedOn = sharedPreferences.getBoolean(key, false);
			boolean isPasswordSaved = Preferences.isSavePassword(sharedPreferences);
			
			if(isNotificationsTurnedOn && !isPasswordSaved) {
				mPasswordInfoDialog = UIUtils.showSimpleAlertDialog(this, android.R.drawable.ic_dialog_info, getString(R.string.info), getString(R.string.error_password_not_saved));
				CheckBoxPreference notificationsCheckBox = (CheckBoxPreference)getPreferenceScreen().findPreference(key);
				sharedPreferences.edit().putBoolean(key, false).commit();
				notificationsCheckBox.setChecked(false);
			} else {
				if(isNotificationsTurnedOn) {
					GCMUtils.register(this);
				} else {
					GCMUtils.unregister(this);
				}
			}
		}
	}
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if(preference != null) {
			if(getString(R.string.preference_purgecache_key).equals(preference.getKey())) {
				Studentportal.clearCaches(getApplicationContext());
				Toast.makeText(this, getString(R.string.preference_purgecache_toast), Toast.LENGTH_SHORT).show();
			} else if(getString(R.string.preference_campus_key).equals(preference.getKey())) {
				startActivity(new Intent(this, CampusPreferenceActivity.class));
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mServiceBroadcastReceiver.unregisterReceiver(this);
		mGCMCommunicator.unregisterReceiver(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Analytics.onActivityStop(this);
		if(mDepMonLauncherInfoDialog != null) {
			mDepMonLauncherInfoDialog.dismiss();
		}
		if(mPasswordInfoDialog != null) {
			mPasswordInfoDialog.dismiss();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}
}
