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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.api.Pruefung;
import at.ac.uniklu.mobile.sportal.api.Semester;
import at.ac.uniklu.mobile.sportal.model.ExamModel;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.ui.FragmentRefreshable;
import at.ac.uniklu.mobile.sportal.ui.GenericListFragmentAdapter;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationAsyncTask;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.MapUtils;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class ExamListFragment extends ListFragment 
		implements ProgressNotificationToggle, FragmentRefreshable {
	
	public static final String ARGUMENT_INDEX = "index";

    private ExamListAdapter mExamListAdapter;
    private ExamModel mExamModel;
    private int mIndex;
    private View mProgressView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIndex = getArguments() != null ? getArguments().getInt(ARGUMENT_INDEX) : 0;
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.exam_list_fragment, container, false);
		mProgressView = v.findViewById(R.id.progress);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		refresh();
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Analytics.onEvent(Analytics.EVENT_WEB_EXAMDETAILS);
		Pruefung exam = mExamModel.getExams().get(position);
    	Intent examDetails = new Intent(getActivity(), WebViewActivity.class)
				.putExtra(WebViewActivity.URL, exam.getDetailsUrl())
				.putExtra(WebViewActivity.TITLE, getString(R.string.exam_details_title));
		startActivity(examDetails);
    }
    
    @Override
	public void progressNotificationOn() {
    	mProgressView.setVisibility(View.VISIBLE);
	}

	@Override
	public void progressNotificationOff() {
		mProgressView.setVisibility(View.GONE);
	}
	
	@Override
	public void refresh() {
    	if(mExamModel != null) {
    		// the model is existing, just load the exams for the appropriate semester
			refresh(mExamModel.getSemester().get(mIndex));
		} else {
			/* The model doesn't exist. This happens on the creation of the fragment
			 * through a FragmentPagerAdapter, or if the runtime killed this fragment 
			 * and rebuilt it later on.
			 */
			ExamListActivity activity = (ExamListActivity)getActivity();
			mExamModel = activity.getExamModel();
			if(mExamModel != null) {
				/* The model exists in the parent activity, just recall this method to
				 * execute the refresh for the according semester.
				 */
				refresh();
			} else {
				/* The parent activity hasn't loaded the model with the base data yet, so
				 * it is necessary to wait until the parent activity has loaded all needed
				 * data.
				 */
				progressNotificationOn();
				activity.registerFragmentForRefresh(this);
			}
		}
	}
	
	public void refresh(final Semester semester) {
		new ProgressNotificationAsyncTask(this) {

			@Override
			protected void doInBackground() throws Exception {
				/*
				 * The course data for index==0 (current semester) doesn't need to be loaded
				 * separately since it is already contained in the initial model loaded by
				 * the activity
				 */
				if(mExamModel == null || mIndex != 0) {
					mExamModel = ModelService.getExamModel(mExamModel, semester);
				}
				mExamListAdapter = new ExamListAdapter(ExamListFragment.this, mExamModel.getExams());
			}
			
			@Override
			protected void onSuccess() {
				setListAdapter(mExamListAdapter);
			};
			
			@Override
			protected void onException(Exception e) {
				UIUtils.processActivityRefreshException(e, getActivity());
				Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.FRAGMENT_EXAMS, e);
			};
			
		}.execute();
	}
	
	private class ExamListAdapter extends GenericListFragmentAdapter<Pruefung> {
		
		private class ViewHolder {
			public View written;
			public View oral;
			public View oralAndWritten;
			public View bookOpen;
			public View bookClosed;
			public View bookPartly;
			public TextView title;
			public TextView details;
			public TextView status;
			public TextView pointsLabel;
			public TextView pointsValue;
		}

		public ExamListAdapter(ListFragment context, List<Pruefung> list) {
			super(context, list, R.layout.exam_list_item);
		}

		@Override
		protected void updateView(int position, View itemView) {
			ViewHolder viewHolder;
			if(itemView.getTag() == null) {
				viewHolder = new ViewHolder();
				viewHolder.written = itemView.findViewById(R.id.written);
				viewHolder.oral = itemView.findViewById(R.id.oral);
	            viewHolder.oralAndWritten = itemView.findViewById(R.id.oral_and_written);
	            viewHolder.bookOpen = itemView.findViewById(R.id.book_open);
	            viewHolder.bookClosed = itemView.findViewById(R.id.book_closed);
	            viewHolder.bookPartly = itemView.findViewById(R.id.book_partly);
				viewHolder.title = (TextView) itemView.findViewById(R.id.text_title);
	            viewHolder.details = (TextView) itemView.findViewById(R.id.text_details);
	            viewHolder.status = (TextView) itemView.findViewById(R.id.text_status);
	            viewHolder.pointsLabel = (TextView) itemView.findViewById(R.id.points_label);
	            viewHolder.pointsValue = (TextView) itemView.findViewById(R.id.points_value);
	            itemView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder)itemView.getTag();
			}
			
            Pruefung exam = mList.get(position);
            
            viewHolder.written.setVisibility(View.GONE);
            viewHolder.oral.setVisibility(View.GONE);
            viewHolder.oralAndWritten.setVisibility(View.GONE);
            
            switch(exam.getModus()) {
            case MUENDLICH: viewHolder.oral.setVisibility(View.VISIBLE); break;
            case SCHRIFTLICH: viewHolder.written.setVisibility(View.VISIBLE); break;
            case MUENDLICH_SCHRIFTLICH: viewHolder.oralAndWritten.setVisibility(View.VISIBLE); break;
            }
            
            viewHolder.bookOpen.setVisibility(View.GONE);
            viewHolder.bookClosed.setVisibility(View.GONE);
            viewHolder.bookPartly.setVisibility(View.GONE);
            
            switch(exam.getUnterlagen()) {
            case MIT: viewHolder.bookOpen.setVisibility(View.VISIBLE); break;
            case OHNE: viewHolder.bookClosed.setVisibility(View.VISIBLE); break;
            case TEILWEISE: viewHolder.bookPartly.setVisibility(View.VISIBLE); break;
            }
            
            viewHolder.title.setText(exam.getLv().getTyp() + " " + exam.getLv().getName());
            
            StringBuilder details = new StringBuilder();
            if(exam.getDatum() != null) {
            	details.append(getString(R.string.exam_details, exam.getDatum()));
            	if(exam.getVon() != null) {
                	details.append(" ").append(exam.getVon());
                	if(exam.getBis() != null) {
                		details.append(" - ").append(exam.getBis());
                	}
                }
                if(exam.getRaum() != null) {
                	details.append(", ").append(exam.getRaum());
                }
            }
            if(details.length() > 0) {
            	viewHolder.details.setVisibility(View.VISIBLE);
            	viewHolder.details.setText(details);
            	MapUtils.linkifyRooms(viewHolder.details);
            } else {
            	viewHolder.details.setVisibility(View.GONE);
            }

            if(exam.getPunkte() != null) {
            	viewHolder.pointsLabel.setVisibility(View.VISIBLE);
            	viewHolder.pointsValue.setVisibility(View.VISIBLE);
            	viewHolder.pointsValue.setText(getString(R.string.exam_points_num, exam.getPunkte()));
            } else {
            	viewHolder.pointsLabel.setVisibility(View.GONE);
            	viewHolder.pointsValue.setVisibility(View.GONE);
            	viewHolder.pointsValue.setText(null);
            }

            String status = "";
            switch(exam.getStatus()) {
            case NICHT_ANGEMELDET:
            	status = getString(R.string.exam_status_notregistered);
            	break;
            case ANGEMELDET:
            	status = getString(R.string.exam_status_registered);
            	break;
            case ANGETRETEN:
            	status = getString(R.string.exam_status_taken);
            	break;
            case BENOTET:
            	status = getString(R.string.exam_status_graded);
            	break;
            case FREIGEGEBEN:
            	status = getString(R.string.exam_status_approved);
            	break;
            case NICHT_ANGETRETEN:
            	status = getString(R.string.exam_status_nottaken);
            	break;
            case ABGEWIESEN:
            	status = getString(R.string.exam_status_rejected);
            	if(exam.getBemerkung() != null) {
            		status += " (" + exam.getBemerkung() + ")";
            	}
            	break;
            }
            viewHolder.status.setText(status);
		}
		
		@Override
		public boolean isEnabled(int position) {
			return mList.get(position).getDetailsUrl() != null;
		}
	}
}
