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
import java.util.List;

import android.app.Activity;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.service.MutingUtils;
import at.ac.uniklu.mobile.sportal.service.MutingUtils.MutingRegion;
import at.ac.uniklu.mobile.sportal.util.Analytics;

public class DebugActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug);
    }
    
    @Override
	protected void onStart() {
		super.onStart();
		Analytics.onActivityStart(this, Analytics.ACTIVITY_DEBUG);
	}
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	Button debugPeriodAddButton = (Button)findViewById(R.id.debug_period_add);
    	debugPeriodAddButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MutingUtils.scheduleDebugPeriod(DebugActivity.this);
			}
    	});
    	
    	Button debugLocationInfoButton = (Button)findViewById(R.id.debug_load_location_info);
    	debugLocationInfoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loadLocationInfos();
			}
    	});
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Analytics.onActivityStop(this);
    }
    
    private void loadLocationInfos() {
    	TextView wifiEnabledText = (TextView)findViewById(R.id.debug_wifi_enabled);
    	TextView wifiSsidText = (TextView)findViewById(R.id.debug_wifi_ssids);
    	TextView wifiMutingSsidText = (TextView)findViewById(R.id.debug_wifi_ssid_muting);
    	
    	TextView locationEnabledText = (TextView)findViewById(R.id.debug_location_enabled);
    	TextView locationLongitudeText = (TextView)findViewById(R.id.debug_location_longitude);
    	TextView locationLatitudeText = (TextView)findViewById(R.id.debug_location_latitude);
    	TextView locationAccuracyText = (TextView)findViewById(R.id.debug_location_accuracy);
    	TextView locationTimeText = (TextView)findViewById(R.id.debug_location_time);
    	TextView locationMuteText = (TextView)findViewById(R.id.debug_location_muting);
    	
    	LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
		
		if(wifiManager.isWifiEnabled()) {
			wifiEnabledText.setText("true");
			
			String ssids = "";
			List<ScanResult> scanResults = wifiManager.getScanResults();
			for(ScanResult scanResult : scanResults) {
				ssids += scanResult.SSID + " (" + scanResult.frequency + "), ";
			}
			wifiSsidText.setText(ssids);
			
			ScanResult mutingNetwork = MutingUtils.findMutingWifiNetwork(scanResults);
			if(mutingNetwork != null) {
				wifiMutingSsidText.setText(mutingNetwork.SSID);
			}
		} else {
			wifiEnabledText.setText("false");
		}
		
		if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			locationEnabledText.setText("true");
			
			Location currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if(currentLocation != null) {
				locationLongitudeText.setText(currentLocation.getLongitude()+"");
				locationLatitudeText.setText(currentLocation.getLatitude()+"");
				locationAccuracyText.setText("+-" + currentLocation.getAccuracy());
				locationTimeText.setText(new Date(currentLocation.getTime()).toString());
				
				MutingRegion mutingRegion = MutingUtils.findOverlappingMutingRegion(currentLocation);
				if(mutingRegion != null) {
					float distance = currentLocation.distanceTo(mutingRegion);
					locationMuteText.setText(mutingRegion.getName() + " (distance: " + distance + "m)");
				}
			}
		} else {
			locationEnabledText.setText("false");
		}
    }
}
