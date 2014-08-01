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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class CalendarActivity extends FragmentActivity implements Refreshable {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);
        new ActionBarHelper(this).setupHeader();
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	Analytics.onActivityStart(this, Analytics.ACTIVITY_CALENDAR);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Analytics.onActivityStop(this);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	UIUtils.executeActivityRefresh(requestCode, this);
    }
	
	@Override
	public void refresh() {
		FragmentManager fm = getSupportFragmentManager();
		Refreshable f = (Refreshable)fm.findFragmentById(R.id.calendar_fragment);
		f.refresh();
	}

}
