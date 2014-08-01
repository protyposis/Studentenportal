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

import java.util.Date;

import org.apache.http.auth.UsernamePasswordCredentials;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.publictransport.stw.StopDB;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.Preferences;

public class StudentportalApplication extends Application {
	
	private static StudentportalApplication sInstance;
	
	/*
	 * keep a reference to the global state to avoid it being killed 
	 * before the application gets killed
	 */
	private Studentportal mStudentportal;

	@Override
	public void onCreate() {
		super.onCreate(); // docs say this is needed
		
		// load default preferences but don't overwrite user set ones
        PreferenceManager.setDefaultValues(this, R.xml.debug_preferences, false);
		
		mStudentportal = Studentportal.initialize(this);
		sInstance = this;
		
		Analytics.init(this);
		upgradeHousekeeping();
	}
	
	public static StudentportalApplication getInstance() {
		return sInstance;
	}
	
	public Studentportal getStudentportal() {
		return mStudentportal;
	}
	
	public UsernamePasswordCredentials getUsernamePasswordCredentials() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return new UsernamePasswordCredentials(
				Preferences.getUsername(preferences),
				Preferences.getPassword(preferences));
	}
	
	/**
	 * Housekeeping of preferences on version upgrades.
	 */
	private void upgradeHousekeeping() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		int previousVersion = Preferences.getVersioncheckVersion(preferences);
		int currentVersion = Studentportal.getAppInfo().getVersionCode();
		
		if(currentVersion == previousVersion) {
			return; // no upgrade has happened, nothing to do
		}
		
		Log.d("StudentenportalApplication", "Version upgrade detected: " + 
				previousVersion + " -> " + currentVersion);
		
		SharedPreferences.Editor editor = preferences.edit();
		
		if(previousVersion < 18) {
			/* With V18, content of userid has been changed from username/cardid 
			 * to userhash, so it needs to be reset for the login procedure to set
			 * the hash correctly.
			 */
			Preferences.setUserId(preferences, null);
		}
		
		if(previousVersion < 19) {
			/* STW has changed their system and the stops got new IDs, so the DB 
			 * must be cleared from those invalid stops and to trigger the download
			 * mechanism of the tops when the BusDeparturesActivity is launched.
			 */
			StopDB stopDB = new StopDB(this);
			stopDB.clearStops();
			stopDB.close();
		}
		
		/* housekeeping finished, finally update the versioncheck code to the 
		 * current version to avoid housekeeping until the next version upgrade
		 */
		Preferences.setVersioncheckDate(editor, new Date());
		Preferences.setVersioncheckVersion(editor, currentVersion);
		
		// persist changes
		editor.commit();
	}
}
