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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.api.Note;
import at.ac.uniklu.mobile.sportal.model.GradeModel;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.ui.GenericListAdapter;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationAsyncTask;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class GradeListActivity extends ListActivity implements ProgressNotificationToggle, Refreshable {
	
	private class GradeListAdapter extends GenericListAdapter<Note> {
		
		private class ViewHolder {
			public TextView title;
			public TextView type;
			public TextView details;
			public TextView grade;
			public View waiting;
			public TextView examtype;
		}
		
		private int[] mGradeColors;

		public GradeListAdapter(ListActivity context, List<Note> list) {
			super(context, list, R.layout.grade_list_item);
			mGradeColors = new int[] {
	            	getResources().getColor(R.color.grade_1),
	            	getResources().getColor(R.color.grade_2),
	            	getResources().getColor(R.color.grade_3),
	            	getResources().getColor(R.color.grade_4),
	            	getResources().getColor(R.color.grade_5)
            };
		}

		@Override
		protected void updateView(int position, View itemView) {
			// http://danroundhill.com/2010/04/02/10-tips-for-developing-android-apps/
			// http://stackoverflow.com/questions/1320478/how-to-load-the-listview-smoothly-in-android
			ViewHolder viewHolder;
			if(itemView.getTag() == null) {
				viewHolder = new ViewHolder();
				viewHolder.title = (TextView) itemView.findViewById(R.id.text_title);
				viewHolder.type = (TextView) itemView.findViewById(R.id.text_type);
	            viewHolder.details = (TextView) itemView.findViewById(R.id.text_details);
	            viewHolder.grade = (TextView) itemView.findViewById(R.id.text_grade);
	            viewHolder.waiting = itemView.findViewById(R.id.grade_in_progress);
	            viewHolder.examtype = (TextView) itemView.findViewById(R.id.text_examtype);
	            itemView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder)itemView.getTag();
			}
			
            Note grade = mList.get(position);
            int gradeColorIndex = 0;
            if(grade.getNote().length() == 1) {
            	gradeColorIndex = Integer.parseInt(grade.getNote()) - 1;
            }
            else if(grade.getNote().equals("MET")) {
            	gradeColorIndex = 0;
            }
            else if(grade.getNote().equals("OET")) {
            	gradeColorIndex = 4;
            }
            
            viewHolder.title.setText(grade.getLv().getName());
            viewHolder.type.setText(grade.getLv().getTyp());
            viewHolder.details.setText(getString(R.string.grade_details, grade.getDatum(), grade.getStunden(), grade.getEcts()));
            viewHolder.grade.setText(grade.getNote());
            viewHolder.grade.setTextColor(mGradeColors[gradeColorIndex]);
            
            viewHolder.title.setEnabled(grade.isUebernommen());
            viewHolder.waiting.setVisibility(grade.isUebernommen() ? View.GONE : View.VISIBLE);
            viewHolder.examtype.setVisibility(grade.getTyp() == Note.Typ.FP ? View.VISIBLE : View.GONE);
		}
		
		@Override
		public boolean isEnabled(int position) {
			return mList.get(position).getDetailsUrl() != null;
		}
	}

    private GradeListAdapter mGradeListAdapter;
    private GradeModel mGradeModel;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grade_list);
        new ActionBarHelper(this).setupHeader();
        
        // if this activity has just been recreated, get retained data
        Object retainedData = getLastNonConfigurationInstance();
        if(retainedData != null) {
        	mGradeModel = (GradeModel)retainedData;
        	mGradeListAdapter = new GradeListAdapter(this, mGradeModel.getGrades());
	        setListAdapter(mGradeListAdapter);
        }
        else {
			refresh();
        }
    }
	
	@Override
    protected void onStart() {
    	super.onStart();
    	Analytics.onActivityStart(this, Analytics.ACTIVITY_GRADES);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Analytics.onActivityStop(this);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	Note grade = mGradeModel.getGrades().get(position);
    	Analytics.onEvent(Analytics.EVENT_WEB_GRADEDETAILS);
    	startActivity(new Intent(this, WebViewActivity.class)
    			.putExtra(WebViewActivity.URL, grade.getDetailsUrl())
    			.putExtra(WebViewActivity.TITLE, getString(R.string.grade_details_title)));
    	super.onListItemClick(l, v, position, id);
    }
    
    @Override
	public Object onRetainNonConfigurationInstance() {
		// hand the model over to the recreated activity
		return mGradeModel;
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	UIUtils.executeActivityRefresh(requestCode, this);
    }
    
    @Override
	public void progressNotificationOn() {
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
	}

	@Override
	public void progressNotificationOff() {
		findViewById(R.id.progress).setVisibility(View.GONE);
	}
	
	@Override
	public void refresh() {
		new ProgressNotificationAsyncTask(this) {

			@Override
			protected void doInBackground() throws Exception {
				mGradeModel = ModelService.getGradeModel(getApplicationContext());
			}
			
			@Override
			protected void onSuccess() {
				mGradeListAdapter = new GradeListAdapter(GradeListActivity.this, mGradeModel.getGrades());
				setListAdapter(mGradeListAdapter);
			};
			
			@Override
			protected void onException(Exception e) {
				UIUtils.processActivityRefreshException(e, GradeListActivity.this);
				Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_GRADES, e);
			};
			
		}.execute();
	}
}
