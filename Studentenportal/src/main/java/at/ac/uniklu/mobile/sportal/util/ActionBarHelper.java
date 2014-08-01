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

package at.ac.uniklu.mobile.sportal.util;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import at.ac.uniklu.mobile.sportal.DashboardActivity;
import at.ac.uniklu.mobile.sportal.R;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;

public class ActionBarHelper implements ProgressNotificationToggle {
	
	private static final Class<DashboardActivity> sHomeActivityClass = DashboardActivity.class;
	
	private Activity mActivity;
	
	public ActionBarHelper(Activity activity) {
		mActivity = activity;
	}
	
    public ActionBarHelper setupHeader() {
        View appIcon = mActivity.findViewById(R.id.actionbar_home);
        if(appIcon != null) {
        	// setup app icon button
        	appIcon.setClickable(!mActivity.getClass().equals(sHomeActivityClass));
        	if(appIcon.isClickable()) {
	        	View.OnClickListener homeClickListener = new View.OnClickListener() {
	        		@Override
	                public void onClick(View view) {
	                	Analytics.onEvent(Analytics.EVENT_HEADER_HOME);
	                    goBack();
	                }
	            };
	            appIcon.setOnClickListener(homeClickListener);
        	}
        }
        return this;
    }
    
    public void setTitleVisibility(boolean visible) {
    	mActivity.findViewById(R.id.view_title).setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    
    public ActionBarHelper addActionRefresh(int textResId) {
    	View refreshButton = mActivity.findViewById(R.id.actionbar_refresh);
    	if(refreshButton == null) {
        	refreshButton = addActionButtonInternal(R.drawable.ic_action_refresh, textResId, null);
        	refreshButton.setId(R.id.actionbar_refresh);
        }
    	if(mActivity instanceof Refreshable) {
    		View.OnClickListener refreshClickListener = new View.OnClickListener() {
                public void onClick(View view) {
                	((Refreshable)mActivity).refresh();
                }
            };
            refreshButton.setOnClickListener(refreshClickListener);
    	} else {
    		refreshButton.setClickable(false);
    	}
    	return this;
    }
    
    public ActionBarHelper addActionRefresh() {
    	return addActionRefresh(0);
    }
    
    public ActionBarHelper addActionMenu() {
    	View v = addActionButtonInternal(R.drawable.ic_action_overflow, 0, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.openOptionsMenu();
			}
        });
    	v.setId(R.id.actionbar_overflow);
    	return this;
    }

    public void goHome() {
        if (mActivity.getClass().equals(sHomeActivityClass)) {
            return;
        }

        Intent intent = new Intent(mActivity, sHomeActivityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mActivity.startActivity(intent);
        mActivity.overridePendingTransition(R.anim.home_enter, R.anim.home_exit);
    }
    
    public void goBack() {
    	mActivity.finish();
    }
    
    public ActionBarHelper addActionButton(int iconResId, int textResId, View.OnClickListener clickListener) {
    	addActionButtonInternal(iconResId, textResId, clickListener);
    	return this;
    }
    
    public ActionBarHelper addActionButton(int id, int iconResId, int textResId, View.OnClickListener clickListener) {
    	View button = addActionButtonInternal(iconResId, textResId, clickListener);
    	button.setId(id);
    	return this;
    }
    
    public View findViewById(int id) {
    	ViewGroup actionBar = (ViewGroup)mActivity.findViewById(R.id.actionbar);
    	if(actionBar != null) {
    		return actionBar.findViewById(id);
    	}
    	return null;
    }
    
    private View addActionButtonInternal(int iconResId, int textResId, View.OnClickListener clickListener) {
        ViewGroup actionBar = (ViewGroup)mActivity.findViewById(R.id.actionbar);
        if (actionBar == null) {
            return null;
        }

        // Create the button
        ImageButton actionButton = new ImageButton(mActivity, null, R.attr.actionbarButtonStyle);
        actionButton.setLayoutParams(new ViewGroup.LayoutParams(
                (int) mActivity.getResources().getDimension(R.dimen.actionbar_height),
                ViewGroup.LayoutParams.FILL_PARENT));
        actionButton.setImageResource(iconResId);
        //actionButton.setScaleType(ImageView.ScaleType.CENTER);
        if(textResId != 0) actionButton.setContentDescription(mActivity.getResources().getString(textResId));
        actionButton.setOnClickListener(clickListener);

        actionBar.addView(actionButton);
        
        if(iconResId == R.drawable.ic_action_refresh) {
        	ProgressBar indicator = new ProgressBar(mActivity, null, R.attr.actionbarProgressIndicatorStyle);
        	indicator.setLayoutParams(new ViewGroup.LayoutParams(
                    (int) mActivity.getResources().getDimension(R.dimen.actionbar_height),
                    ViewGroup.LayoutParams.FILL_PARENT));
            indicator.setId(R.id.actionbar_progress);
            actionBar.addView(indicator);
        }

        return actionButton;
    }

	@Override
	public void progressNotificationOn() {
		mActivity.findViewById(R.id.actionbar_refresh).setVisibility(View.GONE);
		mActivity.findViewById(R.id.actionbar_progress).setVisibility(View.VISIBLE);
	}

	@Override
	public void progressNotificationOff() {
		mActivity.findViewById(R.id.actionbar_refresh).setVisibility(View.VISIBLE);
		mActivity.findViewById(R.id.actionbar_progress).setVisibility(View.GONE);
	}
}
