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

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;
import at.ac.uniklu.mobile.sportal.api.ApiClientException;
import at.ac.uniklu.mobile.sportal.api.LoginStatus;
import at.ac.uniklu.mobile.sportal.notification.GCMUtils;
import at.ac.uniklu.mobile.sportal.service.MutingService;
import at.ac.uniklu.mobile.sportal.ui.AsyncTask;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.NfcLogin;
import at.ac.uniklu.mobile.sportal.util.Preferences;
import at.ac.uniklu.mobile.sportal.util.UIUtils;

public class LoginActivity extends Activity implements ProgressNotificationToggle {
	
	public static final String MODE = "mode";
	public static final int MODE_DEFAULT = 0;
	/* used if the session has expired and a new login is needed
	 * (this mode doesn't open the main activity but just closes itself)
	 */
	public static final int MODE_RELOGIN = 1;
	
	private static final String TAG = "LoginActivity";
	
	private static final int DEBUG_PREFERENCES = 1;
	
	private EditText mUsernameText;
	private EditText mPasswordText;
	private CheckBox mSavePasswordCheckbox;
	private Button mLoginButton;
	private ImageView mRfidImage;
	private View mLogo;
	
	private NfcLogin mNfcLogin;
	private int mMode = MODE_DEFAULT;
	private boolean mAnimateLogin;
	private AsyncTask mLoginTask;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        // hide the scrollbars but still allow scrolling (doesn't work via xml config)
        // http://sree.cc/uncategorized/android-hide-scrollbar-in-scrollview
        ScrollView scrollContainer = (ScrollView)findViewById(R.id.login_container);
        scrollContainer.setVerticalScrollBarEnabled(false);
        scrollContainer.setHorizontalScrollBarEnabled(false);
        
        // reference UI controls
        mUsernameText = (EditText)findViewById(R.id.username_text);
        mPasswordText = (EditText)findViewById(R.id.password_text);
        mSavePasswordCheckbox = (CheckBox)findViewById(R.id.password_save_checkbox);
        mLoginButton = (Button)findViewById(R.id.login_button);
        mRfidImage = (ImageView)findViewById(R.id.rfid_icon);
        mLogo = findViewById(R.id.logo);
        
        // transfer preferences into UI
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUsernameText.setText(Preferences.getUsername(preferences));
        mPasswordText.setText(Preferences.getPassword(preferences));
        mSavePasswordCheckbox.setChecked(Preferences.isSavePassword(preferences));
        
        // create UI handlers
        mLoginButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// hide the keyboard
				// http://stackoverflow.com/questions/3400028/close-virtual-keyboard-on-button-press
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	            imm.hideSoftInputFromWindow(mUsernameText.getWindowToken(), 0);
	            imm.hideSoftInputFromWindow(mPasswordText.getWindowToken(), 0);
	            
				login(mUsernameText.getText().toString().trim(), mPasswordText.getText().toString().trim(), null, false);
			}
        });
        
        mLogo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openOptionsMenu();
			}
		});
        
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1 && Studentportal.getSportalClient().isMifareLoginSupported()) {
        	mNfcLogin = new NfcLogin(this);
        	mNfcLogin.onCreate(savedInstanceState);
        }
        Log.d(TAG, "NFC " + (mNfcLogin == null ? "disabled" : "enabled") + " (API level " + Build.VERSION.SDK_INT + ")");
        
        mMode = getIntent().getIntExtra(MODE, MODE_DEFAULT);
        if(mMode == MODE_RELOGIN) {
        	Toast.makeText(this, R.string.error_session_expired, Toast.LENGTH_LONG).show();
        }
        
        if(Preferences.isSavePassword(preferences)) {
        	// login credentials should be available, try an automatic login
        	mAnimateLogin = false;
        	login(Preferences.getUsername(preferences), Preferences.getPassword(preferences), null, true);
        } else {
        	Studentportal.getSportalClient().clearSessionCookie();
        	mAnimateLogin = true;
        }
    }
	
	@Override
	protected void onStart() {
		super.onStart();
		Analytics.onActivityStart(this, Analytics.ACTIVITY_LOGIN);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(mNfcLogin != null && mNfcLogin.isAvailable()) {
			mNfcLogin.onResume();
			mRfidImage.setVisibility(mNfcLogin.isEnabled() ? View.VISIBLE : View.GONE);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(mNfcLogin != null) {
			mNfcLogin.onPause();
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Analytics.onActivityStop(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(Studentportal.isDebugBuild()) {
			getMenuInflater().inflate(R.menu.login_debugmenu, menu);
		} else {
			getMenuInflater().inflate(R.menu.login_menu, menu);
		}
	    return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Analytics.onEvent(Analytics.EVENT_MENU_LOGIN);
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.debug_preferences:
	        startActivityForResult(new Intent(this, DebugPreferenceActivity.class), DEBUG_PREFERENCES);
	        return true;
	    case R.id.debug_clear_cache:
	    	Studentportal.clearCaches(this);
	    	Analytics.onEvent(Analytics.EVENT_CLEAR_CACHE);
	    	return true;
	    case R.id.debug:
	    	startActivity(new Intent(this, DebugActivity.class));
	    	return true;
	    case R.id.logout:
	    	try {
				Studentportal.getSportalClient().logout();
			} catch (Exception e) {
				UIUtils.buildExceptionDialog(getApplicationContext(), e).show(LoginActivity.this);
			}
	    	return true;
	    case R.id.clear_cookies:
	    	Studentportal.getSportalClient().clearSessionCookie();
	    	return true;
	    case R.id.clear_notificationscheckdate:
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    	Preferences.setNotificationsLastCheckDate(this, prefs, new Date(0));
	    	return true;
	    case R.id.get_notifications:
	    	GCMUtils.notifyUser(this);
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == DEBUG_PREFERENCES) {
			// reinitialize the global context to adapt changed debug preferences
			Studentportal.initialize(getApplicationContext());
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if(mNfcLogin != null) {
			String id = mNfcLogin.onNewIntent(intent);
			if(id != null) {
				login(null, null, id, false);
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		if(mMode == MODE_RELOGIN) {
			moveTaskToBack(true);
		} else {
			if(mLoginTask != null) {
				mLoginTask.cancel(true);
			}
			super.onBackPressed();
		}
	}
	
	private void login(final String username, final String password, 
			final String cardId, final boolean autoLogin) {
		mLoginTask = new AsyncTask() {
			
			private LoginStatus mLoginStatus;
			
			protected void onPreExecute() {
				progressNotificationOn();
			};
			
			@Override
			protected void doInBackground() throws Exception {
				mLoginStatus = Studentportal.getSportalClient()
						.login(username, password, cardId);
				
				if(!mLoginStatus.isLoggedIn()) {
					throw new ApiClientException(ApiClientException.Code.LOGIN_FAILED);
				} 
				else if(!"STUDENT".equalsIgnoreCase(mLoginStatus.getGroup())) {
					throw new ApiClientException(ApiClientException.Code.LOGIN_FAILED_STAFF);
				}
			}
			
			@Override
			protected void onException(Exception e) {
				e.printStackTrace();
				Analytics.onError(Analytics.ERROR_LOGIN_FAILED, e);
				UIUtils.buildExceptionDialog(getApplicationContext(), e).show(LoginActivity.this);
				mAnimateLogin = true;
				progressNotificationOff();
			}
			
			@Override
			protected void onSuccess() {
				handleLoginSuccessful(mLoginStatus, autoLogin, cardId != null);
			}
			
		};
		mLoginTask.execute();
	}
	
	private void handleLoginSuccessful(LoginStatus loginStatus, boolean autoLogin, boolean nfcLogin) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor preferencesEditor = preferences.edit();
		
		boolean userchange = !loginStatus.getUserhash().equals(Preferences.getUserId(preferences));
		boolean relogin = (mMode == MODE_RELOGIN);
		
		if(userchange) {
			/* the current user that logged in isn't the same person as the 
			 * previous user that logged in, cached data must be cleared
			 * OR the hash of a user has been changed
			 */
			Studentportal.clearCaches(this);
			Preferences.setUserId(preferencesEditor, loginStatus.getUserhash());
		}
		
		/* This block only needs to be executed when the login button has been
		 * clicked by the user. Auto-logins should skip this block.
		 */
		if(!autoLogin) {
			// store user login credentials
			Preferences.setUsername(preferencesEditor, mUsernameText.getText().toString().trim());
			Preferences.setPassword(preferencesEditor, mSavePasswordCheckbox.isChecked() ? 
					mPasswordText.getText().toString().trim() : null);
			Preferences.setSavePassword(preferencesEditor, mSavePasswordCheckbox.isChecked());

			Analytics.onEvent(Analytics.EVENT_LOGIN, 
					"savePassword", mSavePasswordCheckbox.isChecked()+"", 
					"nfcEnabled", (mNfcLogin != null && mNfcLogin.isEnabled())+"", 
					"nfcLogin", nfcLogin+"",
					"userchange", userchange+"");
		}
		
		/* Notifications are enabled for a new user by default, if he saves the password. If he
		 * doesn't save the password, they are disabled since no logged-in session can be created
		 * when a notification tickle comes in. 
		 * Since notifications could have been deactivated by a previous user, they need to be 
		 * re-activated to trigger the registration when the dashboard activity starts, if the password
		 * is stored. 
		 * Notifications get enabled at every manual login, since a user could first login without
		 * saving the password and then later decide to save the password, and if it gets saved later,
		 * notifications would be disabled after a re-login without a userchange. */
		if(userchange || (!autoLogin && !relogin)) {
			Preferences.setNotificationsEnabled(this, preferencesEditor, mSavePasswordCheckbox.isChecked());
			GCMUtils.clearRegistrationId(this);
		}
		
		preferencesEditor.commit();
		
		// launch dashboard and remove current activity from stack (no need to come back here)
		// or just close current activity if it is a re-login after a lost session
		//this.finish();
		if(!relogin) {
			startActivity(new Intent(this, DashboardActivity.class));
		}
		this.finish();
		
		/* if the preferences say that the service should be on, turn it on in case 
		 * it isn't already running
		 * (if it's already running, it won't start another time)
		 */
		if(Preferences.isAutomuteEnabled(this, preferences)) {
			startService(new Intent(this, MutingService.class)
					.putExtra(MutingService.ACTION, MutingService.ACTION_TURN_ON));
		}
	}

	@Override
	public void progressNotificationOn() {
		final View loginView = findViewById(R.id.login_form);
		final View progressView = findViewById(R.id.login_progress);

		if(mAnimateLogin) {
			Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
			Animation fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
			fadeOutAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationEnd(Animation animation) {
					progressView.setVisibility(View.VISIBLE);
					loginView.setVisibility(View.INVISIBLE);
				}
				@Override
				public void onAnimationRepeat(Animation animation) {
				}
				@Override
				public void onAnimationStart(Animation animation) {
				}
			});
	
			progressView.setVisibility(View.VISIBLE);
			progressView.startAnimation(fadeInAnimation);
			loginView.startAnimation(fadeOutAnimation);
		} else {
			loginView.setVisibility(View.INVISIBLE);
			progressView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void progressNotificationOff() {
		final View loginView = findViewById(R.id.login_form);
		final View progressView = findViewById(R.id.login_progress);

		if(mAnimateLogin) {
			Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
			Animation fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
			fadeOutAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationEnd(Animation animation) {
					progressView.setVisibility(View.GONE);
					loginView.setVisibility(View.VISIBLE);
				}
				@Override
				public void onAnimationRepeat(Animation animation) {
				}
				@Override
				public void onAnimationStart(Animation animation) {
				}
			});
	
			loginView.setVisibility(View.VISIBLE);
			loginView.startAnimation(fadeInAnimation);
			progressView.startAnimation(fadeOutAnimation);
		} else {
			progressView.setVisibility(View.GONE);
			loginView.setVisibility(View.VISIBLE);
		}
	}
}
