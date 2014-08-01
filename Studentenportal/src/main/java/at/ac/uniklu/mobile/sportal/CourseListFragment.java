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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import at.ac.uniklu.mobile.sportal.api.Lehrveranstaltung;
import at.ac.uniklu.mobile.sportal.api.Semester;
import at.ac.uniklu.mobile.sportal.model.CourseModel;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.persistence.StudentPortalDB;
import at.ac.uniklu.mobile.sportal.ui.FragmentPagerSupport;
import at.ac.uniklu.mobile.sportal.ui.FragmentRefreshable;
import at.ac.uniklu.mobile.sportal.ui.GenericListFragmentAdapter;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationAsyncTask;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;

import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class CourseListFragment extends ListFragment 
		implements ProgressNotificationToggle, FragmentRefreshable {
	
	public interface CourseListFragmentEventListener {
		void onBlacklistChanged();
	}
	
	public static final String ARGUMENT_INDEX = "index";
	
	private static final String TAG = "CourseListFragment";
	private static final boolean DEBUG = true;
	
	private CourseListFragmentEventListener mListener;
    private CourseListAdapter mCourseListAdapter;
    private CourseModel mCourseModel;
    private StudentPortalDB mDB;
    private int mIndex;
    private View mProgressView;
    
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);

		try {
			mListener = (CourseListFragmentEventListener) activity;
		} catch (ClassCastException e) {
			Log.d(TAG, "Listener not implemented");
		}
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIndex = getArguments() != null ? getArguments().getInt(ARGUMENT_INDEX) : 0;
        if(DEBUG) Log.d(TAG, "onCreate " + mIndex);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.course_list_fragment, container, false);
		mProgressView = v.findViewById(R.id.progress);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		refresh();
	}
	
	@Override
	public void onStart() {
		if(DEBUG) Log.d(TAG, "onStart " + mIndex);
		super.onStart();
		registerForContextMenu(getListView());
	}
    
    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		l.showContextMenuForChild(v);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        int position = ((AdapterContextMenuInfo) menuInfo).position;
        getActivity().getMenuInflater().inflate(R.menu.course_context_menu, menu);
        
        Lehrveranstaltung course = mCourseModel.getCourse(position);
        menu.setHeaderTitle(course.getTyp() + " " + course.getName());
        menu.getItem(1).setVisible(course.getWebsiteUrl() != null);
        menu.getItem(2).setVisible(course.getMoodleUrl() != null);
        menu.getItem(3).setVisible(course.getStatus().equals("aufgenommen"));
        menu.getItem(4).setVisible(course.isKreuzellisten());
        if(mIndex > 0) {
        	// hide the blacklist menu item for past semesters
        	menu.getItem(5).setVisible(false);
        } else if(!mDB.courseBlacklist_isListed(course.getKey())) {
        	menu.getItem(5).setChecked(true);
        }
        //http://stackoverflow.com/questions/5440601/android-how-to-enable-disable-option-menu-item-on-button-click
        
        Analytics.onEvent(Analytics.EVENT_CONTEXTMENU_COURSE);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	if(DEBUG) Log.d(TAG, "onContextItemSelected " + getTag());
    	/* WORKAROUND 
    	 * The onContextItemSelected event is received by all "visible" fragments, in this
    	 * case it is the currently displayed fragment in the ViewPager, and fragments beside
    	 * this displayed fragment that are preloaded and thus "visible" but not really visible
    	 * since they are outside the screen.
    	 * Anyway, since this event is received by all "visible" fragments, I need to make sure
    	 * that only the event handler of the currently displayed fragment gets executed.
    	 */
    	if(((FragmentPagerSupport)getActivity()).getCurrentItemIndex() != mIndex) {
    		return false;
    	}
    	
        Lehrveranstaltung course = mCourseModel
        		.getCourse(((AdapterContextMenuInfo) item.getMenuInfo()).position);
    
        switch (item.getItemId()) {
	        case R.id.details:
	        	Analytics.onEvent(Analytics.EVENT_WEB_COURSEDETAILS);
	        	Intent courseDetails = new Intent(getActivity(), WebViewActivity.class)
    					.putExtra(WebViewActivity.URL, course.getDetailsUrl())
    					.putExtra(WebViewActivity.TITLE, getString(R.string.course_details_title));
        		startActivity(courseDetails);
	            return true;
	        case R.id.participants:
	        	Analytics.onEvent(Analytics.EVENT_COURSE_PARTICIPANTS, "rlvkey", course.getKey()+"");
	        	Intent participants = new Intent(getActivity(), CourseParticipantsListActivity.class)
						.putExtra(CourseListActivity.COURSE_KEY, course.getKey())
						.putExtra(CourseListActivity.COURSE_NAME, course.getTyp() + " " + course.getName());
	        	startActivity(participants);
	        	return true;
	        case R.id.website:
	        	Analytics.onEvent(Analytics.EVENT_WEB_COURSEWEBSITE);
	        	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(course.getWebsiteUrl())));
	        	return true;
	        case R.id.moodle:
	        	Analytics.onEvent(Analytics.EVENT_WEB_COURSEMOODLE);
	        	Intent moodle = new Intent(getActivity(), WebViewActivity.class)
						.putExtra(WebViewActivity.URL, course.getMoodleUrl())
						.putExtra(WebViewActivity.TITLE, getString(R.string.moodle))
						.putExtra(WebViewActivity.MOODLE_HACK, true)
						.putExtra(WebViewActivity.SSO, true);
	        	startActivity(moodle);
	        	return true;
	        case R.id.checklists:
	        	Analytics.onEvent(Analytics.EVENT_COURSE_CHECKLISTS, "rlvkey", course.getKey()+"");
	        	Intent checklists = new Intent(getActivity(), CourseChecklistsActivity.class)
						.putExtra(CourseListActivity.COURSE_KEY, course.getKey())
						.putExtra(CourseListActivity.COURSE_NAME, course.getTyp() + " " + course.getName());
	        	startActivity(checklists);
	        	return true;
	        case R.id.ignore:
	        	Analytics.onEvent(Analytics.EVENT_IGNORE_COURSE);
	        	if(item.isChecked()) {
	        		mDB.courseBlacklist_insert(course.getKey());
	        	} else {
	        		mDB.courseBlacklist_delete(course.getKey());
	        	}
	        	if(DEBUG) Log.d(TAG, "notifyDataSetChanged " + getTag());
	        	mCourseListAdapter.notifyDataSetChanged();
	        	mListener.onBlacklistChanged();
	        	return true;
        }
        
        return super.onContextItemSelected(item);
    }
    
    @Override
    public void refresh() {
    	if(mCourseModel != null) {
    		// the model is existing, just load the courses for the appropriate semester
    		if(DEBUG) Log.d(TAG, mCourseModel.getSemester().get(mIndex).getKey() + " refresh " + getTag());
			refresh(mCourseModel.getSemester().get(mIndex));
		} else {
			/* The model doesn't exist. This happens on the creation of the fragment
			 * through a FragmentPagerAdapter, or if the runtime killed this fragment 
			 * and rebuilt it later on.
			 */
			CourseListActivity activity = (CourseListActivity)getActivity();
			mDB = Studentportal.getStudentPortalDB();
			mCourseModel = activity.getCourseModel();
			if(mCourseModel != null) {
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
				if(mCourseModel == null || mIndex != 0) {
					mCourseModel = ModelService.getCourseModel(mCourseModel, semester);
				}
				initListAdapter();
			}
			
			@Override
			protected void onException(Exception e) {
				mCourseModel = null;
				CourseListActivity a = (CourseListActivity)getActivity();
				// show the error dialog only if the fragment is currently displayed
				if(mIndex == a.getCurrentItemIndex()) {
					UIUtils.processActivityRefreshException(e, a);
				} else {
					e.printStackTrace();
				}
				Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.FRAGMENT_COURSES, e);
			}
			
			@Override
			protected void onSuccess() {
				if(DEBUG) Log.d(TAG, "refresh success " + getTag());
				setListAdapter(mCourseListAdapter);
			}
        	
        }.execute();
    }
    
    private void initListAdapter() {
    	mCourseListAdapter = new CourseListAdapter(this, mCourseModel.getCourses());
    }
    
    @Override
	public void progressNotificationOn() {
    	mProgressView.setVisibility(View.VISIBLE);
	}

	@Override
	public void progressNotificationOff() {
		mProgressView.setVisibility(View.GONE);
	}
	
	private class CourseListAdapter extends GenericListFragmentAdapter<Lehrveranstaltung> {

		public CourseListAdapter(ListFragment context, List<Lehrveranstaltung> list) {
			super(context, list, R.layout.course_list_item);
		}

		@Override
		protected void updateView(int position, View itemView) {
			TextView typeText = (TextView) itemView.findViewById(R.id.text_type);
            TextView titleText = (TextView) itemView.findViewById(R.id.text_title);
            TextView detailsText = (TextView) itemView.findViewById(R.id.text_details);
            View checklistImage = itemView.findViewById(R.id.checklist);
            
            Lehrveranstaltung lv = mList.get(position);
            typeText.setText(lv.getTyp());
            titleText.setText(lv.getName());
            detailsText.setText(getString(R.string.course_details, lv.getNummer(), lv.getStunden(), lv.getStatus()));
            checklistImage.setVisibility(lv.isKreuzellisten() ? View.VISIBLE : View.GONE);
            
            titleText.setEnabled(!mDB.courseBlacklist_isListed(lv.getKey()));
		}
	}
}
