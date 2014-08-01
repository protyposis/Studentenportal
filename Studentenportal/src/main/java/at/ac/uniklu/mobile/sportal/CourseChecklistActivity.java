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
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.api.ApiServerException;
import at.ac.uniklu.mobile.sportal.api.Kreuzelliste;
import at.ac.uniklu.mobile.sportal.model.KreuzellisteModel;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.ui.RotationAwareAsyncTask;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class CourseChecklistActivity extends ListActivity
		implements ProgressNotificationToggle, Refreshable {
	
	private static final String TAG = "CourseChecklistActivity";
	private static final int DIALOG_FORBIDDEN = 1;
	
	public static final String CHECKLIST_KEY = "checklist_key";
	public static final String CHECKLIST_NAME = "checklist_name";
	
	private ActionBarHelper mActionBar;
	private View mProgressOverlay;
	
	private int mCourseKey;
	private int mChecklistKey;
	private KreuzellisteModel mModel;
	private LoadChecklistTask mLoadTask;
	private SaveChecklistTask mSaveTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.course_checklist);
        mActionBar = new ActionBarHelper(this)
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
        
        mChecklistKey = getIntent().getIntExtra(CHECKLIST_KEY, -1);
        if(mChecklistKey == -1) {
        	Log.e(TAG, "checklist key missing");
        	finish();
        }
        
        TextView courseTitleText = (TextView)findViewById(R.id.view_subtitle);
        courseTitleText.setText(courseTitle);
        
        mProgressOverlay = findViewById(R.id.progress);
        
        RetainedData retainedData = (RetainedData)getLastNonConfigurationInstance();
		if(retainedData != null) {
			mLoadTask = retainedData.loadChecklistTask;
			if(!mLoadTask.attach(this)) {
				setupActivity(mLoadTask.getModel());
			}
			mSaveTask = retainedData.saveChecklistTask;
			if(mSaveTask != null && !mSaveTask.attach(this)) {
				submitted();
			}
		} else {
			refresh();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Analytics.onActivityStart(this, Analytics.ACTIVITY_COURSE_CHECKLIST);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Analytics.onActivityStop(this);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		RetainedData retainedData = new RetainedData();
		
		mLoadTask.detach();
		retainedData.loadChecklistTask = mLoadTask;
		
		if(mSaveTask != null) {
			mSaveTask.detach();
			retainedData.saveChecklistTask = mSaveTask;
		}
		
		return retainedData;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == DIALOG_FORBIDDEN) {
			return new AlertDialog.Builder(this)
					.setTitle(R.string.error)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setMessage(R.string.course_checklist_error_closed)
					.setPositiveButton(android.R.string.ok, null)
					.create();
		}
		return super.onCreateDialog(id);
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
		mLoadTask = new LoadChecklistTask(this, mCourseKey, mChecklistKey);
        mLoadTask.execute();
	}
	
	private void setupActivity(KreuzellisteModel model) {
		mModel = model;
		
		CourseChecklistsActivity.updateKreuzellisteView(this, 
				mModel.getKreuzelliste(), findViewById(android.R.id.content));
		findViewById(R.id.text_checks).setVisibility(View.GONE);
		findViewById(R.id.checklist_details).setVisibility(View.VISIBLE);

		ChecklistAdapter adapter = new ChecklistAdapter(this, mModel);
		setListAdapter(adapter);
		
		ListView listView = getListView();
		boolean[] checks = mModel.getAufgabenKreuzel();
		for(int i = 0; i < checks.length; i++) {
			listView.setItemChecked(i, checks[i]);
		}
		
		if(mModel.getKreuzelliste().isOffen()) {
			if(mActionBar.findViewById(R.id.actionbar_done) == null) {
				mActionBar.addActionButton(R.id.actionbar_done, 
						R.drawable.ic_action_done, 0, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						submit();
					}
				});
			}
		}
	}
	
	private void submit() {
		if(mSaveTask == null) {
			SparseBooleanArray checks = getListView().getCheckedItemPositions();
			Kreuzelliste submitKL = mModel.prepareForSubmit(checks);
			
			int changes = 0;
			for(int i = 0; i < submitKL.getAufgaben().size(); i++) {
				boolean before = mModel.getKreuzelliste().getAufgaben().get(i).isGekreuzt();
				boolean after = submitKL.getAufgaben().get(i).isGekreuzt();
				if(before != after) {
					changes++;
					Log.d(TAG, "changed: " + i + " -> " + after);
				}
			}
			
			Analytics.onEvent(Analytics.EVENT_COURSE_CHECKLIST_SAVE, 
					"rlvkey", mCourseKey+"",
					"klkey", mChecklistKey+"",
					"changed", (changes > 0)+"");
			
			if(changes > 0) {
				mSaveTask = new SaveChecklistTask(this, mCourseKey, submitKL);
				mSaveTask.execute();
			} else {
				finish();
			}
		}
	}
	
	private void submitted() {
		mSaveTask = null;
		setResult(RESULT_OK);
		finish();
	}
	
	private static class RetainedData {
		public LoadChecklistTask loadChecklistTask;
		public SaveChecklistTask saveChecklistTask;
	}
	
	private static class ChecklistAdapter extends ArrayAdapter<String> {
		
		private KreuzellisteModel mModel;
		private boolean mEnabled;

		public ChecklistAdapter(Context context, KreuzellisteModel model) {
			super(context, R.layout.simple_list_item_checked, 
					model.getAufgabenTitel(context));
			mModel = model;
			mEnabled = mModel.getKreuzelliste().isOffen();
		}
		
		@Override
		public boolean areAllItemsEnabled() {
			return mEnabled;
		}
		
		@Override
		public boolean isEnabled(int position) {
			return mEnabled;
		}
	}

	private static class LoadChecklistTask extends RotationAwareAsyncTask<CourseChecklistActivity> {
		
		private int mLvKey;
		private int mKlKey;
		private KreuzellisteModel mModel;

		public LoadChecklistTask(CourseChecklistActivity activity, int lvkey, int klkey) {
			super(activity, activity);
			mLvKey = lvkey;
			mKlKey = klkey;
		}
		
		public KreuzellisteModel getModel() {
			return mModel;
		}

		@Override
		protected void doInBackground() throws Exception {
			mModel = ModelService.getKreuzellisteModel(mLvKey, mKlKey);
		}
		
		@Override
		protected void onException(Exception e) {
    		e.printStackTrace();
			UIUtils.showSimpleErrorDialogAndClose(mActivity, e.getMessage());
			Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_COURSE_CHECKLIST, e);
		}
		
		@Override
		protected void onSuccess() {
			mActivity.setupActivity(mModel);
		}
	}
	
	private static class SaveChecklistTask extends RotationAwareAsyncTask<CourseChecklistActivity> {
		
		private int mLvKey;
		private Kreuzelliste mKreuzelliste;

		public SaveChecklistTask(CourseChecklistActivity activity, 
				int lvkey, Kreuzelliste kreuzelliste) {
			super(activity, activity);
			mLvKey = lvkey;
			mKreuzelliste = kreuzelliste;
		}

		@Override
		protected void doInBackground() throws Exception {
			Studentportal.getSportalClient().postKreuzelliste(mLvKey, mKreuzelliste);
		}
		
		@Override
		protected void onException(Exception e) {
			if(e instanceof ApiServerException && ((ApiServerException)e).getError().getCode() == 403) {
				mActivity.showDialog(DIALOG_FORBIDDEN);
				mActivity.mSaveTask = null;
				mActivity.refresh();
				mActivity.setResult(RESULT_OK); // parent activity must be refreshed since the list has been closed
				return;
			}
    		e.printStackTrace();
			UIUtils.showSimpleErrorDialogAndClose(mActivity, e.getMessage());
			Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_COURSE_CHECKLIST, e);
		}
		
		@Override
		protected void onSuccess() {
			mActivity.submitted();
		}
	}
}
