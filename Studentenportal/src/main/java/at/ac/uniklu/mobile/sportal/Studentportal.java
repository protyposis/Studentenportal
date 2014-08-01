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

import java.io.File;

import org.apache.http.auth.UsernamePasswordCredentials;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.api.UnikluSportalApiClient;
import at.ac.uniklu.mobile.sportal.persistence.StudentPortalDB;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.AppInfo;
import at.ac.uniklu.mobile.sportal.util.Preferences;
import at.ac.uniklu.mobile.sportal.util.Utils;

public class Studentportal {
	
	public static final int NOTIFICATION_MS_INFO = 1;
	public static final int NOTIFICATION_MS_ERROR = 2;
	public static final int NOTIFICATION_GCM_NOTIFICATION = 100; // reserve all numbers until 199 for notification types!!
	public static final int NOTIFICATION_GCM_BROADCASTMESSAGE = 200;
	
	public static final String EXTRA_REFETCH_DATA = "reload-data-from-server";
	
	private static final String TAG = "Global";
	private static final String API_SERVER = "https://campus.aau.at";
	
	private static Studentportal instance;

	private AppInfo mAppInfo;
	private boolean mDebugBuild;
	private UnikluSportalApiClient mUnikluSportalClient;
	private StudentPortalDB mStudentPortalDB;
	
	private Studentportal(Context applicationContext) {
		// setup internal stuff
		mAppInfo = getAppInfo(applicationContext);
		mDebugBuild = isDebugBuild(applicationContext);
		
		instance = this;
		
		// setup stuff that depends on an already existing instance
		mUnikluSportalClient = new UnikluSportalApiClient(
				"Studentenportal/" + mAppInfo.getVersionCode() + 
				" (" + mAppInfo.getVersionName() + "; Android " + Build.VERSION.SDK_INT + ")",
				mDebugBuild, applicationContext, true);
		
		mStudentPortalDB = new StudentPortalDB(applicationContext);
	}
	
	private static AppInfo getAppInfo(Context context) {
		AppInfo appInfo = new AppInfo();
		appInfo.setAppName(context.getString(R.string.app_name));
		
        try {
    		// get the app version infos
    		PackageInfo packageInfo = context.getPackageManager()
    				.getPackageInfo(context.getPackageName(), 0);
    		appInfo.setVersionCode(packageInfo.versionCode);
    		appInfo.setVersionName(packageInfo.versionName);
    		appInfo.setPackageName(packageInfo.packageName);
        } catch (NameNotFoundException e) {
            Log.d("SportalApplication", "could not retrieve version info");
            Analytics.onError(Analytics.ERROR_VERSIONINFO, e);
            appInfo.setVersionCode(-1);
    		appInfo.setVersionName("n/a");
        }
        
        return appInfo;
    }
	
	public static Studentportal initialize(Context applicationContext) {
		instance = new Studentportal(applicationContext);
		
		if(isDebugBuild()) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
			String serverURL = Preferences.getDebugServerURL(applicationContext, preferences);
			instance.mUnikluSportalClient.setBaseUrl(serverURL != null ? serverURL : API_SERVER);
			Log.d(TAG, "DEBUG BUILD");
		} else {
			instance.mUnikluSportalClient.setBaseUrl(API_SERVER);
			Log.d(TAG, "RELEASE BUILD");
		}

		return instance;
	}
	
	/**
	 * Tells if the application has been built in debug mode.
	 * code taken and adapted from: http://stackoverflow.com/questions/3029819/android-automatically-choose-debug-release-maps-api-key
	 * @param context
	 * @return
	 */
	private static boolean isDebugBuild(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
			return ((pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
		} catch (Exception e) {
		}
		return false;
	}

	private static Studentportal getInstance() {
		if(instance == null) {
			Log.e(TAG, "NOT INITIALIZED!");
		}
		return instance;
	}
	
	public static UnikluSportalApiClient getSportalClient() {
		return getInstance().mUnikluSportalClient;
	}
	
	public static StudentPortalDB getStudentPortalDB() {
		return getInstance().mStudentPortalDB;
	}
	
	public static boolean isDebugBuild() {
		return getInstance().mDebugBuild;
	}
	
	public static AppInfo getAppInfo() {
		return getInstance().mAppInfo;
	}
	
	public static void clearCaches(Context applicationContext) {
		getSportalClient().clearCache();
    	getStudentPortalDB().mutingPeriods_clear();
    	
    	// delete cached profile pictures
    	File privateFilesDir = applicationContext.getFilesDir();
    	for(File privateFile : privateFilesDir.listFiles()) {
    		privateFile.delete();
    	}
    	
    	// delete application cache (webview etc...)
    	Utils.clearAppCache(applicationContext);
	}
	
	public static UsernamePasswordCredentials getUsernamePasswordCredentials() {
		return StudentportalApplication.getInstance().getUsernamePasswordCredentials();
	}
}
