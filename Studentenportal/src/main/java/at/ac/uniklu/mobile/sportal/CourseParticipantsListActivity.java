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

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import at.ac.uniklu.mobile.sportal.api.Student;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.model.TeilnehmerlisteModel;
import at.ac.uniklu.mobile.sportal.ui.AsyncTask;
import at.ac.uniklu.mobile.sportal.ui.GenericListAdapter;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.ui.RotationAwareAsyncTask;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;
import at.ac.uniklu.mobile.sportal.util.Utils;

public class CourseParticipantsListActivity extends ListActivity
		implements ProgressNotificationToggle, Refreshable {
	
	private static final String TAG = "CourseParticipantsListActivity";
	
	private ActionBarHelper mActionBar;
	private View mProgressOverlay;
	private TextView mListHeaderNotice;
	
	private int mCourseKey;
	private TeilnehmerlisteModel mModel;
	private LoadParticipantsTask mLoadParticipantsTask;
	private AsyncImageLoader mImageLoader;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.course_participants);
        mActionBar = new ActionBarHelper(this).setupHeader();
        mActionBar.addActionButton(R.drawable.ic_action_settings, R.string.preference_campus, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(CourseParticipantsListActivity.this, CampusPreferenceActivity.class), 0);
			}
		});
        
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
		
		int picturePixelWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
		mImageLoader = new AsyncImageLoader(picturePixelWidth);
		
		View listHeader = getLayoutInflater().inflate(R.layout.course_participants_list_header, null);
        getListView().addHeaderView(listHeader, null, false);
        mListHeaderNotice = (TextView)listHeader.findViewById(R.id.course_participants_shared_data_notice);
        
        mLoadParticipantsTask = (LoadParticipantsTask)getLastNonConfigurationInstance();
		if(mLoadParticipantsTask != null) {
			if(!mLoadParticipantsTask.attach(this)) {
				setupActivity(mLoadParticipantsTask.getModel());
			}
		} else {
	        refresh();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Analytics.onActivityStart(this, Analytics.ACTIVITY_COURSE_PARTICIPANTS);
		registerForContextMenu(getListView());
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Analytics.onActivityStop(this);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		l.showContextMenuForChild(v);
    }
	
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        int position = ((AdapterContextMenuInfo) menuInfo).position - 1; // -1 = list header view offset
        getMenuInflater().inflate(R.menu.courseparticipants_context_menu, menu);
        
        Student student = mModel.getTeilnehmer().get(position);
        menu.setHeaderTitle(student.getFullName());
        menu.getItem(0).setVisible(student.getEmail() != null);
        menu.getItem(1).setVisible(student.getMobileTelNr() != null);
        menu.getItem(2).setVisible(student.getMobileTelNr() != null);
        menu.getItem(3).setVisible(student.getTelNr() != null);
        
        Analytics.onEvent(Analytics.EVENT_CONTEXTMENU_COURSE_PARTICIPANT);
    }
	
	@Override
    public boolean onContextItemSelected(MenuItem item) {
		Student student = mModel.getTeilnehmer().get(((AdapterContextMenuInfo) item.getMenuInfo()).position - 1); // -1 = list header view offset
    
        switch (item.getItemId()) {
        
        	case R.id.send_email:
        		Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO);
        		emailIntent.setData(Uri.parse("mailto:" + student.getEmail()));
        		startActivity(Intent.createChooser(emailIntent, null)); 
        		return true;
        		
        	case R.id.send_message:
        		Intent messageIntent = new Intent(android.content.Intent.ACTION_SENDTO);
        		messageIntent.setData(Uri.parse("smsto:" + student.getMobileTelNr()));
        		startActivity(Intent.createChooser(messageIntent, null));
        		return true;
        		
        	case R.id.call_home:
        	    Intent phoneHomeIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + student.getTelNr()));
        	    startActivity(phoneHomeIntent);
        	    return true;
        	    
        	case R.id.call_mobile:
        	    Intent phoneMobileIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + student.getMobileTelNr()));
        	    startActivity(phoneMobileIntent);
        	    return true;
        		
        	case R.id.add_to_contacts:
        		Intent addContactIntent = new Intent(Intent.ACTION_INSERT);
        		addContactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        		addContactIntent.putExtra(ContactsContract.Intents.Insert.NAME, student.getFullName());
        		addContactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, student.getMobileTelNr());
        		addContactIntent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, student.getTelNr());
        		addContactIntent.putExtra(ContactsContract.Intents.Insert.EMAIL, student.getEmail());
        		startActivity(addContactIntent);
        		return true;
        		
        }
        
        return super.onContextItemSelected(item);
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK) {
			refresh();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		mLoadParticipantsTask.detach();
		return mLoadParticipantsTask;
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
		mLoadParticipantsTask = new LoadParticipantsTask(this, mCourseKey);
		mLoadParticipantsTask.execute();
	}
	
	private void setupActivity(TeilnehmerlisteModel model) {
		mModel = model;
		ParticipantsListAdapter adapter = new ParticipantsListAdapter(this, 
				mModel.getTeilnehmer(), R.layout.course_participants_item);
		setListAdapter(adapter);
		refreshListHeaderNotice();
	}
	
	private void refreshListHeaderNotice() {
		String notice = getString(R.string.course_participants_shared_data_notice);
		if(mModel.getEinstellungen().isShareMobilTelNr()) {
			notice += ", " + getString(R.string.preference_campus_phone_mobile);
		}
		if(mModel.getEinstellungen().isShareStudienFestnetzTelNr()) {
			notice += ", " + getString(R.string.preference_campus_phone_home);
		}
		if(mModel.getEinstellungen().isShareFoto()) {
			notice += ", " + getString(R.string.preference_campus_picture);
		}
		mListHeaderNotice.setText(notice);
	}
	
	private static class LoadParticipantsTask extends
			RotationAwareAsyncTask<CourseParticipantsListActivity> {

		private int mLvKey;
		private TeilnehmerlisteModel mModel;

		public LoadParticipantsTask(CourseParticipantsListActivity activity, int lvkey) {
			super(activity, activity);
			mLvKey = lvkey;
		}

		public TeilnehmerlisteModel getModel() {
			return mModel;
		}

		@Override
		protected void doInBackground() throws Exception {
			mModel = ModelService.getTeilnehmerlisteModel(mLvKey);
		}

		@Override
		protected void onException(Exception e) {
			e.printStackTrace();
			UIUtils.showSimpleErrorDialogAndClose(mActivity, e.getMessage());
			Analytics.onError(Analytics.ERROR_GENERIC + "/"
					+ Analytics.ACTIVITY_COURSE_PARTICIPANTS, e);
		}

		@Override
		protected void onSuccess() {
			mActivity.setupActivity(mModel);
		}
	}

	private static class ParticipantsListAdapter extends
			GenericListAdapter<Student> {
		
		private class ViewHolder {
			public TextView name;
			public TextView email;
			public TextView phone;
			public TextView mobilephone;
			public ImageView picture;
		}

		public ParticipantsListAdapter(ListActivity context,
				List<Student> list, int viewResourceId) {
			super(context, list, viewResourceId);
		}

		@Override
		protected void updateView(int position, View itemView) {
			Student teilnehmer = mList.get(position);
			
			ViewHolder viewHolder;
			if(itemView.getTag() == null) {
				viewHolder = new ViewHolder();
				viewHolder.name = (TextView)itemView.findViewById(R.id.text_name);
				viewHolder.email = (TextView)itemView.findViewById(R.id.text_email);
				viewHolder.phone = (TextView)itemView.findViewById(R.id.text_phone);
				viewHolder.mobilephone = (TextView)itemView.findViewById(R.id.text_mobilephone);
				viewHolder.picture = (ImageView)itemView.findViewById(R.id.picture);
				itemView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder)itemView.getTag();
			}
			
			viewHolder.picture.setImageBitmap(null);
			
			viewHolder.name.setText(teilnehmer.getFullName());
			viewHolder.email.setText(teilnehmer.getEmail());
			
			if(teilnehmer.getTelNr() != null) {
				viewHolder.phone.setText(teilnehmer.getTelNr());
				viewHolder.phone.setVisibility(View.VISIBLE);
			} else {
				viewHolder.phone.setVisibility(View.GONE);
			}
			
			if(teilnehmer.getMobileTelNr() != null) {
				viewHolder.mobilephone.setText(teilnehmer.getMobileTelNr());
				viewHolder.mobilephone.setVisibility(View.VISIBLE);
			} else {
				viewHolder.mobilephone.setVisibility(View.GONE);
			}
			
			if(teilnehmer.isProfilePicture()) {
				CourseParticipantsListActivity a = (CourseParticipantsListActivity) mContext;
				a.mImageLoader.load(mContext, teilnehmer.getKey(), viewHolder.picture);
				viewHolder.picture.setVisibility(View.VISIBLE);
			} else {
				viewHolder.picture.setVisibility(View.GONE);
			}
		}
	}
	
	@SuppressLint("UseSparseArrays")
	private static class AsyncImageLoader {
		
		private static final int MAX_TASKS = 3;
		private static final int MAX_BITMAPS_IN_MEMORY = 20;
		
		private int mPictureWidth;
		private Queue<Integer> mWorkQueue;
		private Map<Integer, ImageView> mKeyToView;
		private Map<ImageView, Integer> mViewToKey;
		private Map<Integer, SoftReference<Bitmap>> mBitmapCache;
		private volatile int mTasksRunning;
		
		/**
		 * Keeps hard references to MAX_BITMAPS_IN_MEMORY bitmaps to avoid them being
		 * removed from memory by the GC.
		 */
		private Queue<Bitmap> mHardBitmapCache;
		
		public AsyncImageLoader(int pictureWidth) {
			mPictureWidth = pictureWidth;
			mWorkQueue = new ConcurrentLinkedQueue<Integer>();
			mKeyToView = new HashMap<Integer, ImageView>();
			mViewToKey = new HashMap<ImageView, Integer>();
			mBitmapCache = new HashMap<Integer, SoftReference<Bitmap>>();
			mTasksRunning = 0;
			mHardBitmapCache = new ConcurrentLinkedQueue<Bitmap>();
		}
		
		public void load(Activity context, int key, ImageView imageView) {
			SoftReference<Bitmap> cachedBitmapRef = mBitmapCache.get(key);
			Bitmap cachedBitmap = cachedBitmapRef != null ? cachedBitmapRef.get() : null;
			if(cachedBitmap != null) {
				imageView.setImageBitmap(cachedBitmap);
			} else {
				Integer oldKey = mViewToKey.get(imageView);
				mWorkQueue.remove(oldKey);
				mKeyToView.remove(oldKey);
				
				mWorkQueue.add(key);
				mKeyToView.put(key, imageView);
				mViewToKey.put(imageView, key);
				
				if(mTasksRunning < MAX_TASKS) {
					mTasksRunning++;
					new LoadPictureTask(context, mWorkQueue.poll(), mPictureWidth).execute();
				}
			}
		}
		
		private class LoadPictureTask extends AsyncTask {
			
			private Activity mContext;
			private int mKey;
			private int mPictureWidth;
			private Bitmap mBitmap;
			
			public LoadPictureTask(Activity context, int key, int pictureWidth) {
				mContext = context;
				mKey = key;
				mPictureWidth = pictureWidth;
			}

			@Override
			protected void doInBackground() throws Exception {
				mBitmap = Utils.downloadBitmapWithCache(mContext, Studentportal.getSportalClient().getBaseUrl() + 
						"/api/sportal/student/" + mKey + "/foto?width=" + mPictureWidth, 
						"foto" + mKey + ".jpg");
			}
			
			@Override
			protected void onSuccess() {
				ImageView imageView = mKeyToView.get(mKey);
				if(imageView != null) {
					imageView.setImageBitmap(mBitmap);
				}
				mBitmapCache.put(mKey, new SoftReference<Bitmap>(mBitmap));
				
				if(mBitmap != null) {
					if(mHardBitmapCache.size() >= MAX_BITMAPS_IN_MEMORY) {
						mHardBitmapCache.poll();
					}
					mHardBitmapCache.add(mBitmap);
				}
			}
			
			@Override
			protected void onException(Exception e) {
				e.printStackTrace();
				UIUtils.showSimpleErrorDialogAndClose(mContext, e.getMessage());
				Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_COURSE_PARTICIPANTS, e);
			}
			
			@Override
			protected void onPostExecute() {
				if(!mWorkQueue.isEmpty()) {
					new LoadPictureTask(mContext, mWorkQueue.poll(), mPictureWidth).execute();
				} else {
					mTasksRunning--;
				}
			}
			
		}
	}

}
