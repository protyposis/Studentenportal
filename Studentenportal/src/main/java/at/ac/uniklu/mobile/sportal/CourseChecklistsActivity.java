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

import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.api.Kreuzelliste;
import at.ac.uniklu.mobile.sportal.model.KreuzellistenModel;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.ui.GenericListAdapter;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.ui.RotationAwareAsyncTask;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class CourseChecklistsActivity extends ListActivity 
		implements ProgressNotificationToggle, Refreshable {
	
	private static final String TAG = "CourseChecklistsActivity";
	
	private View mProgressOverlay;
	
	private int mCourseKey;
	private KreuzellistenModel mModel;
	private LoadChecklistsTask mLoadChecklistsTask;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.course_checklists);
        new ActionBarHelper(this)
        		.setupHeader();
        
        mCourseKey = getIntent().getIntExtra(CourseListActivity.COURSE_KEY, -1);
        if(mCourseKey == -1) {
        	Log.e(TAG, "course key missing");
        	finish();
        }
        
        String courseTitle = getIntent().getStringExtra(CourseListActivity.COURSE_NAME);
        if(courseTitle == null) {
        	Log.e(TAG, "course title missing");
        	finish();
        }
        
        TextView courseTitleText = (TextView)findViewById(R.id.view_subtitle);
        courseTitleText.setText(courseTitle);
        
        mProgressOverlay = findViewById(R.id.progress);
        
        mLoadChecklistsTask = (LoadChecklistsTask)getLastNonConfigurationInstance();
		if(mLoadChecklistsTask != null) {
			if(!mLoadChecklistsTask.attach(this)) {
				setupActivity(mLoadChecklistsTask.getModel());
			}
		} else {
	        refresh();
		}
    }
	
	@Override
	protected void onStart() {
		super.onStart();
		Analytics.onActivityStart(this, Analytics.ACTIVITY_COURSE_CHECKLISTS);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Analytics.onActivityStop(this);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Kreuzelliste kreuzelliste = mModel.getKreuzellisten().get(position);
		Log.d(TAG, "selected checklist: " + 
				kreuzelliste.getName() + "/" + kreuzelliste.getKey());
		
		Analytics.onEvent(Analytics.EVENT_COURSE_CHECKLIST, "klkey", kreuzelliste.getKey()+"");
		Intent checklists = new Intent(this, CourseChecklistActivity.class)
				.putExtras(getIntent().getExtras())
				.putExtra(CourseChecklistActivity.CHECKLIST_KEY, kreuzelliste.getKey())
				.putExtra(CourseChecklistActivity.CHECKLIST_NAME, kreuzelliste.getName());
		startActivityForResult(checklists, 0);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK) {
			// checklist has been saved, reload checklists
			refresh();
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		mLoadChecklistsTask.detach();
		return mLoadChecklistsTask;
	}

	@Override
	public void progressNotificationOn() {
		mProgressOverlay.setVisibility(View.VISIBLE);
	}

	@Override
	public void progressNotificationOff() {
		mProgressOverlay.setVisibility(View.GONE);
	}
	
	@Override
	public void refresh() {
		mLoadChecklistsTask = new LoadChecklistsTask(this, mCourseKey);
        mLoadChecklistsTask.execute();
	}
	
	private void setupActivity(KreuzellistenModel model) {
		mModel = model;
		ChecklistsListAdapter adapter = new ChecklistsListAdapter(this, 
				mModel.getKreuzellisten(), R.layout.course_checklists_item);
		setListAdapter(adapter);
		
		// compute total sum and percentage of checks
		int numChecks = 0;
		int numChecksChecked = 0;
		for(Kreuzelliste kl : mModel.getKreuzellisten()) {
			if(kl.getAnzahl() != null) {
				numChecks += kl.getAnzahl();
				numChecksChecked += kl.getKreuzel();
			}
		}
		float percentChecked = (float)numChecksChecked / numChecks * 100;
		TextView totalChecksText = (TextView)findViewById(R.id.total_checks);
		totalChecksText.setText(getString(R.string.course_checklist_totalchecks, 
				numChecksChecked, numChecks, percentChecked));
		findViewById(R.id.total_checks_stats).setVisibility(View.VISIBLE);
	}
	
	public static void updateKreuzellisteView(Context context, 
			Kreuzelliste kreuzelliste, View itemView) {
		TextView titleText = (TextView)itemView.findViewById(R.id.text_title);
		TextView labelText = (TextView)itemView.findViewById(R.id.text_label);
		TextView expiresText = (TextView)itemView.findViewById(R.id.text_expires);
		TextView discussionText = (TextView)itemView.findViewById(R.id.text_discussion);
		TextView checksText = (TextView)itemView.findViewById(R.id.text_checks);
		
		titleText.setText(kreuzelliste.getName());
		if(kreuzelliste.isOffen()) {
			labelText.setText(R.string.course_checklist_open);
			labelText.setBackgroundResource(R.color.date_running);
		} else {
			labelText.setText(R.string.course_checklist_closed);
			labelText.setBackgroundResource(R.color.date_canceled);
		}
		expiresText.setText(context.getText(R.string.course_checklist_expires) + ": " +
				context.getString(R.string.calendar_date, kreuzelliste.getAblaufdatum()) + " " +
				context.getString(R.string.calendar_time, kreuzelliste.getAblaufdatum()));
		discussionText.setText(context.getText(R.string.course_checklist_discussion) + ": " +
				context.getString(R.string.calendar_date, kreuzelliste.getBesprechungsdatum()));
		checksText.setText(context.getString(R.string.course_checklist_checks, 
				kreuzelliste.getKreuzel(), kreuzelliste.getAnzahl()));
	}

	private static class LoadChecklistsTask 
			extends RotationAwareAsyncTask<CourseChecklistsActivity> {
		
		private int mLvKey;
		private KreuzellistenModel mModel;

		public LoadChecklistsTask(CourseChecklistsActivity activity, int lvkey) {
			super(activity, activity);
			mLvKey = lvkey;
		}
		
		public KreuzellistenModel getModel() {
			return mModel;
		}

		@Override
		protected void doInBackground() throws Exception {
			mModel = ModelService.getKreuzellistenModel(mLvKey);
		}
		
		@Override
		protected void onException(Exception e) {
    		e.printStackTrace();
			UIUtils.showSimpleErrorDialogAndClose(mActivity, e.getMessage());
			Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_COURSE_CHECKLISTS, e);
		}
		
		@Override
		protected void onSuccess() {
			mActivity.setupActivity(mModel);
		}
	}
	
	private static class ChecklistsListAdapter extends GenericListAdapter<Kreuzelliste> {

		public ChecklistsListAdapter(ListActivity context,
				List<Kreuzelliste> list, int viewResourceId) {
			super(context, list, viewResourceId);
		}

		@Override
		protected void updateView(int position, View itemView) {
			Kreuzelliste kreuzelliste = mList.get(position);
			updateKreuzellisteView(mContext, kreuzelliste, itemView);
		}
	}
}
