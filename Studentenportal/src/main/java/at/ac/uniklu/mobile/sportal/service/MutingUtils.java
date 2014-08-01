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

package at.ac.uniklu.mobile.sportal.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.net.wifi.ScanResult;
import android.preference.PreferenceManager;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.Studentportal;
import at.ac.uniklu.mobile.sportal.api.Termin;
import at.ac.uniklu.mobile.sportal.util.Preferences;

public class MutingUtils {

	/**
	 * Defines a circular area by given coordinates and a radius.
	 */
	public static class MutingRegion extends Location {
		private String name;
		private float radius; // in meters
		
		public MutingRegion(String name, double latitude, double longitude, float radius) {
			super("");
			setLatitude(latitude);
			setLongitude(longitude);
			setName(name);
			setRadius(radius);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public float getRadius() {
			return radius;
		}

		public void setRadius(float radius) {
			this.radius = radius;
		}
	}
	
	private static final String TAG = "MutingUtils";
	
	/**
	 * A list of regions in which the phone should be muted during courses.
	 */
	private static final MutingRegion[] mMutingRegions;
	
	/**
	 * A list of wifi networks that will trigger muting.
	 */
	private static final String[] mMutingWifiNetworks;
	
	static {
		// set up the muting locations
		mMutingRegions = new MutingRegion[] {
				new MutingRegion("University", 46.616321, 14.264899, 200),
				new MutingRegion("University Vorstufe", 46.616947, 14.263247, 50),
				new MutingRegion("University Mensa", 46.618207, 14.268569, 50),
				new MutingRegion("Lakeside Park", 46.614626, 14.263086, 100)//,
				//new MutingRegion("Sterneckstraße", 46.626416, 14.299929, 50)
		};
		
		// define muting networks
		mMutingWifiNetworks = new String[] {
				"zid-connect",
				"eduroam"
		};
	}
	
	/**
	 * Returns a muting region if an overlap with the given location has been detected, else null.
	 * @param location
	 * @return
	 */
	public static MutingRegion findOverlappingMutingRegion(Location location) {
		for(MutingRegion mutingRegion : mMutingRegions) {
			float distance = location.distanceTo(mutingRegion);
			if(distance < location.getAccuracy() + mutingRegion.getRadius()) {
				return mutingRegion;
			}
		}
		return null;
	}
	
	/**
	 * Returns true if there's a muting region that overlaps with the given location.
	 * @param location
	 * @return true if the location overlaps with a muting region, else false
	 */
	public static boolean isOverlappingWithMutingRegion(Location location) {
		return findOverlappingMutingRegion(location) != null;
	}
	
	/**
	 * Returns true if the given SSID equals to one of the muting network SSIDs.
	 * @param ssid
	 * @return true if the SSID equals to a muting SSID, else false
	 */
	public static boolean isMutingWifiNetwork(String ssid) {
		for(String mutingSsid : mMutingWifiNetworks) {
			if(mutingSsid.equals(ssid)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns a network that is detected as being a muting network.
	 * @param networks
	 * @return a network detected as a muting network, or null if no corresponding muting network is existing
	 */
	public static ScanResult findMutingWifiNetwork(List<ScanResult> networks) {
		if(networks != null) {
			for(ScanResult scanResult : networks) {
				if(isMutingWifiNetwork(scanResult.SSID)) {
					return scanResult;
				}
			}
		}
		return null;
	}
	
	/**
	 * Mutes the ringtone if it isn't already muted.
	 * @param context
	 */
	public static void ringtoneTurnOff(Context context) {
		AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		// mute the ringtone if it isn't already muted
		if(audioManager.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE) {
			ignoreNextRingerModeChangedBroadcast(context);
			audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
			// reset ringtone override property
			Preferences.setRingtoneOverride(preferences, false);
		} else {
			// enable ringtone override to avoid the ringtone being turned on by the app since it is turned off anyway
			Preferences.setRingtoneOverride(preferences, true);
		}
		Log.d(TAG, "ringtoneTurnOff() (override: " + Preferences.isRingtoneOverride(preferences) + ")");
	}
	
	/**
	 * Call the method if the ringtone has been muted by the app and the user has changed the ringtone setting manually.
	 * If called, the ringtone setting won't be changed by a {@link #ringtoneOn(Context)} call.
	 * @param context
	 */
	public static void ringtoneUserOverride(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Preferences.setRingtoneOverride(preferences, false);
		Log.d(TAG, "ringtoneUserOverride()");
	}
	
	/**
	 * Turns the phone's ringtone on if the user hasn't changed the ringtone setting manually
	 * during a muting period that was initiated by the app.
	 * @param context
	 */
	public static void ringtoneTurnOn(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Log.d(TAG, "ringtoneTurnOn() (override: " + Preferences.isRingtoneOverride(preferences) + ")");
		if( Preferences.isRingtoneOverride(preferences)) {
			// ringtone override is true, that means the user has changed the ringtone setting manually (and the app won't turn it back on)
			// reset ringtone override
			Preferences.setRingtoneOverride(preferences, false);
		} else {
			AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
			ignoreNextRingerModeChangedBroadcast(context);
			audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		}
	}
	
	public static void ignoreNextRingerModeChangedBroadcast(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Preferences.setIgnoreNextRingerbroadcast(preferences, true);
	}
	
	public static boolean isRingerModeChangedBroadcastToIgnore(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		boolean ignore = Preferences.isIgnoreNextRingerbroadcast(preferences);
		Preferences.setIgnoreNextRingerbroadcast(preferences, false);
		return ignore;
	}
	
	public static void scheduleDebugPeriod(Context context) {
		long now = System.currentTimeMillis();
		List<Termin> debugTermine = new ArrayList<Termin>();
		Termin t = new Termin();
		t.setDatum(new Date(now + 1000 * 10));
		t.setDauer(1);
		t.setTitel("Debugtermin");
		t.setTyp("DT");
		debugTermine.add(t);
		Studentportal.getStudentPortalDB().mutingPeriods_insertNew(debugTermine);
		
		context.startService(new Intent(context, MutingService.class)
				.putExtra(MutingService.ACTION, MutingService.ACTION_SCHEDULE));
	}
}
