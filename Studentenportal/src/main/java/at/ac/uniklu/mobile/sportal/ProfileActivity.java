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

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.api.Semester;
import at.ac.uniklu.mobile.sportal.api.Student;
import at.ac.uniklu.mobile.sportal.api.Studium;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.model.ProfileModel;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationAsyncTask;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class ProfileActivity extends Activity implements ProgressNotificationToggle, Refreshable {
	
	private ProfileModel mProfileModel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.profile);
		new ActionBarHelper(this).setupHeader();
		
		// if this activity has just been recreated, get retained data
        Object retainedData = getLastNonConfigurationInstance();
        if(retainedData != null) {
        	mProfileModel = (ProfileModel)retainedData;
        	populateProfile();
        }
        else {
			refresh();
        }
	}
	
	@Override
    protected void onStart() {
    	super.onStart();
    	Analytics.onActivityStart(this, Analytics.ACTIVITY_PROFILE);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Analytics.onActivityStop(this);
    }
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		// hand the model over to the recreated activity
		return mProfileModel;
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	UIUtils.executeActivityRefresh(requestCode, this);
    }
	
	@Override
	public void refresh() {
		new ProgressNotificationAsyncTask(this) {
			
			@Override
			protected void doInBackground() throws Exception {
				mProfileModel = ModelService.getProfileModel(ProfileActivity.this);
			}

			@Override
			protected void onSuccess() {
				populateProfile();
			}

			@Override
			protected void onException(Exception e) {
				UIUtils.processActivityRefreshException(e, ProfileActivity.this);
				Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_PROFILE, e);
			}
			
		}.execute();
	}
	
	private void populateProfile() {
		if(mProfileModel.getStudent() != null) {
			Student student = mProfileModel.getStudent();
			SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_format));
			
    		TextView matNrText = (TextView)findViewById(R.id.matnumber);
			TextView firstNameText = (TextView)findViewById(R.id.firstname);
			TextView lastNameText = (TextView)findViewById(R.id.lastname);
			TextView titlePreText = (TextView)findViewById(R.id.title_prefix);
			TextView titleSufText = (TextView)findViewById(R.id.title_suffix);
			TextView usernameText = (TextView)findViewById(R.id.username);
			TextView nicknameText = (TextView)findViewById(R.id.nickname);
			TextView emailText = (TextView)findViewById(R.id.email_address);
			TextView birthdayText = (TextView)findViewById(R.id.birthday);
			TextView validthruText = (TextView)findViewById(R.id.validthru);
			TextView currentSemesterNameText = (TextView)findViewById(R.id.current_semester_name);
			TextView currentSemesterDatespanText = (TextView)findViewById(R.id.current_semester_datespan);
			
			matNrText.setText(student.getMnr());
			firstNameText.setText(student.getVorname().toUpperCase(Locale.GERMAN));
			lastNameText.setText(student.getNachname().toUpperCase(Locale.GERMAN));
			titlePreText.setText(student.getTitelVorgestellt());
			titleSufText.setText(student.getTitelNachgestellt());
			usernameText.setText(student.getUsername());
			nicknameText.setText(student.getNickname());
			emailText.setText(student.getEmail());
			birthdayText.setText(student.getGeburtsdatum() != null ? dateFormat.format(student.getGeburtsdatum()) : "");
			validthruText.setText(student.isHasCard() && student.getCardGueltigBis() != null ? 
					dateFormat.format(student.getCardGueltigBis()) : getString(R.string.unknown));
			
			Semester currentSemester = mProfileModel.getCurrentSemester();
			currentSemesterNameText.setText(UIUtils.getTermName(currentSemester.getKey(), ProfileActivity.this));
			currentSemesterDatespanText.setText(getString(R.string.date_span, 
					currentSemester.getBeginn(), currentSemester.getEnde()));
			
			if(mProfileModel.getStudentPortrait() != null) {
				ImageView portraitImageView = (ImageView)findViewById(R.id.portrait_image);
				portraitImageView.setImageBitmap(mProfileModel.getStudentPortrait());
			}
			
			// hide empty text views
			if(student.getTitelVorgestellt() == null) {
				findViewById(R.id.title_prefix_row).setVisibility(View.GONE);
			}
			if(student.getTitelNachgestellt() == null) {
				findViewById(R.id.title_suffix_row).setVisibility(View.GONE);
			}
			if(student.getNickname() == null) {
				findViewById(R.id.nickname_row).setVisibility(View.GONE);
			}
			
			// load study views
			if(mProfileModel.getStudies() != null) {
				LinearLayout profileDetailsView = (LinearLayout)findViewById(R.id.profile_details);
				for(Studium study : mProfileModel.getStudies()) {
					View studyView = getLayoutInflater().inflate(R.layout.profile_study, profileDetailsView, false);
					TextView studyTitleText = (TextView)studyView.findViewById(R.id.text_title);
					TextView studyDetailsText = (TextView)studyView.findViewById(R.id.text_details);
					TextView studyC1Text = (TextView)studyView.findViewById(R.id.link_curriculum1);
					TextView studyC2Text = (TextView)studyView.findViewById(R.id.link_curriculum2);
					
					studyTitleText.setText(study.getName());
					studyDetailsText.setText(study.getSkz() + " / " + getString(study.isBeendet() ? 
							R.string.date_span : R.string.date_since, study.getBeginn(), study.getEnde()));
					
					if(study.getCurriculum1() != null) {
						studyC1Text.setMovementMethod(LinkMovementMethod.getInstance());
						studyC1Text.setText(Html.fromHtml(getString(R.string.curriculum_link, 
								study.getCurriculum1().getUrl(), study.getCurriculum1().getName())));
					} else {
						studyC1Text.setVisibility(View.GONE);
					}
					
					if(study.getCurriculum2() != null) {
						studyC2Text.setMovementMethod(LinkMovementMethod.getInstance());
						studyC2Text.setText(Html.fromHtml(getString(R.string.curriculum_link, 
								study.getCurriculum2().getUrl(), study.getCurriculum2().getName())));
					} else {
						studyC2Text.setVisibility(View.GONE);
					}
					
					profileDetailsView.addView(studyView);
				}
			}
		}
	}

	@Override
	public void progressNotificationOn() {
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
	}

	@Override
	public void progressNotificationOff() {
		findViewById(R.id.progress).setVisibility(View.GONE);
	}
}
