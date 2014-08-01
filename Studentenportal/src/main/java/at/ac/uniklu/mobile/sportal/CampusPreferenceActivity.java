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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import at.ac.uniklu.mobile.sportal.api.Einstellungen;
import at.ac.uniklu.mobile.sportal.model.EinstellungenModel;
import at.ac.uniklu.mobile.sportal.model.ModelService;
import at.ac.uniklu.mobile.sportal.ui.RotationAwareAsyncTask;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.StringUtils;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class CampusPreferenceActivity extends PreferenceActivity {
	
	//private static final String TAG = "CampusPreferenceActivity";
	
	private static final int DIALOG_LOADING = 1;
	private static final int DIALOG_SAVING = 2;
	
	private EinstellungenModel mModel;
	private LoadSettingsTask mLoadSettingsTask;
	private SaveSettingsTask mSaveSettingsTask;
	
	private EditTextPreference mPhoneMobilePreference;
	private EditTextPreference mPhoneHomePreference;
	private CheckBoxPreference mSharePhoneMobilePreference;
	private CheckBoxPreference mSharePhoneHomePreference;
	private CheckBoxPreference mSharePicturePreference;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.campus_preferences);
		
		mPhoneMobilePreference = (EditTextPreference)getPreferenceScreen()
				.findPreference(getString(R.string.preference_campus_phone_mobile_key));
		mPhoneHomePreference = (EditTextPreference)getPreferenceScreen()
				.findPreference(getString(R.string.preference_campus_phone_home_key));
		mSharePhoneMobilePreference = (CheckBoxPreference)getPreferenceScreen()
				.findPreference(getString(R.string.preference_campus_share_phone_mobile_key));
		mSharePhoneHomePreference = (CheckBoxPreference)getPreferenceScreen()
				.findPreference(getString(R.string.preference_campus_share_phone_home_key));
		mSharePicturePreference = (CheckBoxPreference)getPreferenceScreen()
				.findPreference(getString(R.string.preference_campus_share_picture_key));
		
		// automatically set and update the texts of the EditTextPreferences
		OnPreferenceChangeListener editTextPreferenceUpdateTextListener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				/* set the value of the EditTextPreference as user-visible text
				 * on the preference view */
				String s = newValue == null ? null : newValue.toString().trim();
				((EditTextPreference)preference).setSummary(StringUtils.isEmpty(s) ? 
						getString(R.string.no_tel_nr) : s);
				return true;
			}
		};
		mPhoneMobilePreference.setOnPreferenceChangeListener(editTextPreferenceUpdateTextListener);
		mPhoneHomePreference.setOnPreferenceChangeListener(editTextPreferenceUpdateTextListener);

		Object[] retainedData = (Object[]) getLastNonConfigurationInstance();
		if(retainedData != null) {
			mModel = (EinstellungenModel) retainedData[0];
			mLoadSettingsTask = (LoadSettingsTask) retainedData[1];
			mSaveSettingsTask = (SaveSettingsTask) retainedData[2];
			
			if(mLoadSettingsTask != null) mLoadSettingsTask.attach(this);
			if(mSaveSettingsTask != null) mSaveSettingsTask.attach(this);
			
			if(mModel != null) populatePreferences(mModel);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if(mModel == null && mLoadSettingsTask == null) {
			mLoadSettingsTask = new LoadSettingsTask(this);
			mLoadSettingsTask.execute();
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == DIALOG_LOADING || id == DIALOG_SAVING) {
			ProgressDialog d = new ProgressDialog(this);
			d.setMessage(id == DIALOG_LOADING ? getString(R.string.settings_loading) : getString(R.string.settings_saving));
			d.setIndeterminate(true);
			d.setCancelable(false);
			return d;
		}
		return null;
	}
	
	@Override
	public void onBackPressed() {
		gatherPreferences();
		if(mModel != null && mModel.isEinstellungenChanged()) {
			mSaveSettingsTask = new SaveSettingsTask(this, mModel);
			mSaveSettingsTask.execute();
			setResult(RESULT_OK);
		} else {
			super.onBackPressed();
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		if(mLoadSettingsTask != null) mLoadSettingsTask.detach();
		if(mSaveSettingsTask != null) mSaveSettingsTask.detach();
		return new Object[] { mModel, mLoadSettingsTask, mSaveSettingsTask };
	}
	
	private void populatePreferences(EinstellungenModel model) {
		mModel = model;
		
		Einstellungen e = mModel.getEinstellungen();
		mPhoneMobilePreference.setText(e.getMobilTelNr());
		mPhoneHomePreference.setText(e.getStudienFestnetzTelNr());
		mSharePhoneMobilePreference.setChecked(e.isShareMobilTelNr());
		mSharePhoneHomePreference.setChecked(e.isShareStudienFestnetzTelNr());
		mSharePicturePreference.setChecked(e.isShareFoto());
		
		// trigger setting the initial summary texts
		mPhoneMobilePreference.getOnPreferenceChangeListener()
				.onPreferenceChange(mPhoneMobilePreference, mPhoneMobilePreference.getText());
		mPhoneHomePreference.getOnPreferenceChangeListener()
				.onPreferenceChange(mPhoneHomePreference, mPhoneHomePreference.getText());
	}
	
	private void gatherPreferences() {
		Einstellungen e = mModel.getEinstellungen();
		e.setMobilTelNr(StringUtils.trim(mPhoneMobilePreference.getText()));
		e.setStudienFestnetzTelNr(StringUtils.trim(mPhoneHomePreference.getText()));
		e.setShareMobilTelNr(mSharePhoneMobilePreference.isChecked());
		e.setShareStudienFestnetzTelNr(mSharePhoneHomePreference.isChecked());
		e.setShareFoto(mSharePicturePreference.isChecked());
	}
	
	private static class LoadSettingsTask extends RotationAwareAsyncTask<CampusPreferenceActivity> {
		
		private EinstellungenModel mModel;
		
		public LoadSettingsTask(CampusPreferenceActivity activity) {
			super(null, activity);
		}

		@Override
		protected void onPreExecute() {
			mActivity.showDialog(DIALOG_LOADING);
		}

		@Override
		protected void doInBackground() throws Exception {
			Thread.sleep(5000);
			mModel = ModelService.getEinstellungenModel();
		}
		
		@Override
		protected void onPostExecute() {
			mActivity.removeDialog(DIALOG_LOADING);
		}
		
		@Override
		protected void onException(Exception e) {
    		e.printStackTrace();
			UIUtils.showSimpleErrorDialogAndClose(mActivity, e.getMessage());
			Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_BUS_STOPS, e);
		}
		
		@Override
		protected void onSuccess() {
			mActivity.populatePreferences(mModel);
		}
		
	}
	
	private static class SaveSettingsTask extends RotationAwareAsyncTask<CampusPreferenceActivity> {
		
		private EinstellungenModel mModel;
		
		public SaveSettingsTask(CampusPreferenceActivity activity, EinstellungenModel model) {
			super(null, activity);
			mModel = model;
		}

		@Override
		protected void onPreExecute() {
			mActivity.showDialog(DIALOG_SAVING);
		}

		@Override
		protected void doInBackground() throws Exception {
			ModelService.saveEinstellungenModel(mModel);
		}
		
		@Override
		protected void onPostExecute() {
			mActivity.removeDialog(DIALOG_SAVING);
		}
		
		@Override
		protected void onException(Exception e) {
    		e.printStackTrace();
			UIUtils.showSimpleErrorDialogAndClose(mActivity, e.getMessage());
			Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_BUS_STOPS, e);
		}
		
		@Override
		protected void onSuccess() {
			mActivity.finish();
		}
		
	}

}
