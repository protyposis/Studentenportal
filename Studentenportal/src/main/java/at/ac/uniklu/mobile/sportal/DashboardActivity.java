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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.PopupMenu;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.notification.GCMUtils;
import at.ac.uniklu.mobile.sportal.service.MutingService;
import at.ac.uniklu.mobile.sportal.service.ServiceToActivityBroadcastReceiver;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class DashboardActivity extends FragmentActivity {
	
	@SuppressWarnings("unused")
	private static final String TAG = "DashboardActivity2";
	private static final int REQUEST_SETTINGS = 1;
	
	private ServiceToActivityBroadcastReceiver mServiceBroadcastReceiver;
	private TextView mAutomuteButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);
        new ActionBarHelper(this)
        		.setupHeader()
        		.addActionMenu();
        
        mAutomuteButton = (TextView)findViewById(R.id.automute_toggle);
        mAutomuteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Analytics.onEvent(Analytics.EVENT_AUTOMUTE_TOGGLESWITCH);
				openSettings();
			}
        });
        
        mServiceBroadcastReceiver = new ServiceToActivityBroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				super.onReceive(context, intent);
				if(intent.hasExtra(MutingService.ACTION_RESPONSE_ISRUNNING)) {
					refreshAutomuteButton(intent.getExtras().getBoolean(MutingService.ACTION_RESPONSE_ISRUNNING));
				}
			}
		};
		
		// register for cloud messaging
		GCMUtils.OnMainActivityCreate(this);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	Analytics.onActivityStart(this, Analytics.ACTIVITY_DASHBOARD);
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		mServiceBroadcastReceiver.registerReceiver(this);
		startService(new Intent(this, MutingService.class)
				.putExtra(MutingService.ACTION, MutingService.ACTION_REQUEST_ISRUNNING));
		mAutomuteButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.automute_indicator_working, 0);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mServiceBroadcastReceiver.unregisterReceiver(this);
	}
    
    @Override
	protected void onStop() {
		super.onStop();
		Analytics.onActivityStop(this);
	}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	GCMUtils.OnMainActivityDestroy(this);
    }
    
    @TargetApi(11)
	@Override
	public void openOptionsMenu() {
		// Honeycomb and ICS tablet menu workaround
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			/* Android >= 3 with holo theme doesn't support the options menu, so 
			 * display a popup menu below the overflow action button. */
			PopupMenu m = new PopupMenu(this, findViewById(R.id.actionbar_overflow));
			m.getMenuInflater().inflate(R.menu.dashboard_menu, m.getMenu());
			m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					return onOptionsItemSelected(item);
				}
			});
			m.show();
		} else {
			// show default options menu on Android 2.x
			super.openOptionsMenu();
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }
    
    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Analytics.onEvent(Analytics.EVENT_MENU_DASHBOARD);
		return super.onPrepareOptionsMenu(menu);
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.about:
	    	displayAboutBox();
	        return true;
	    case R.id.logout:
	    	logout();
	    	return true;
	    case R.id.settings:
	    	openSettings();
	    	return true;
	    case R.id.map:
	    	startActivity(new Intent(this, MapActivity.class));
	    	return true;
	    case R.id.mensa:
	    	startActivity(new Intent(this, MensaActivity.class));
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		UIUtils.executeActivityRefresh(resultCode, 
				(Refreshable)getSupportFragmentManager().findFragmentById(R.id.upcomingdates));
	}
    
    private void displayAboutBox() {
    	new DashboardAboutDialogFragment().show(getSupportFragmentManager(), "aboutDialog");
		Analytics.onEvent(Analytics.EVENT_ABOUT);
    }
    
    private void openSettings() {
		startActivityForResult(new Intent(this, MainPreferenceActivity.class), REQUEST_SETTINGS);
	}
    
    private void logout() {
    	new DashboardLogoutDialogFragment().show(getSupportFragmentManager(), "logoutDialog");
	}
    
    private void refreshAutomuteButton(boolean running) {
		if(running) {
			mAutomuteButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.automute_indicator_on, 0);
		} else {
			mAutomuteButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.automute_indicator_off, 0);
		}
	}
}
