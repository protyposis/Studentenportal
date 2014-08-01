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

import org.apache.http.cookie.Cookie;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.MapUtils;
import at.ac.uniklu.mobile.sportal.util.Utils;
import at.ac.uniklu.mobile.sportal.util.MoodleDownloadHelper;

public class WebViewActivity extends Activity implements ProgressNotificationToggle {
	
	private static final String TAG = "WebView";

    public static final String URL = "url";
    public static final String TITLE = "title";
    public static final String MOODLE_HACK = "moodlehack"; // moodle redirect history clear hack
    public static final String SSO = "sso";
    
    private ActionBarHelper mActionBar;
    private WebView mWebView;
    private boolean mSSO;
    private boolean mExecuteMoodleHack;
    private boolean mIsFirstPage;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String targetUrl = getIntent().getStringExtra(URL);
        if(targetUrl == null || targetUrl.length() == 0) {
        	// if there's no URL, close the activity
        	finish();
        }
        
        setContentView(R.layout.webview);
        mActionBar = new ActionBarHelper(this)
        		.setupHeader()
        		.addActionRefresh();
        
        // set header title or hide header if no title is given
        String title = getIntent().getStringExtra(TITLE);
        if(title != null) {
        	((TextView)findViewById(R.id.view_title)).setText(title);
        } else {
        	findViewById(R.id.actionbar).setVisibility(View.GONE);
        }
        
        // Moodle 2.0 uses SSO/CAS authentication
        mSSO = getIntent().getBooleanExtra(SSO, false);
        
        // the moodle hack is only needed until Android 2.3 (or maybe 3.x? - not tested)
        // Android 4.0 has the redirect history entry problem solved
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
        	mExecuteMoodleHack = getIntent().getBooleanExtra(MOODLE_HACK, false);
        }
        
        mWebView = (WebView) findViewById(R.id.web_view);
        //mWebView.setBackgroundColor(Color.BLACK); // black color messes up the CAS login page
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY); // http://stackoverflow.com/questions/3998916/android-webview-leaves-space-for-scrollbar
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setLightTouchEnabled(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setUseWideViewPort(true);
        
        // setup custom webview client that shows a progress dialog while loading
        mWebView.setWebViewClient(new WebViewClient() {
        	@Override
        	public boolean shouldOverrideUrlLoading(WebView view, String url) {
        		if(url.startsWith("https://sso.uni-klu.ac.at") 
        				|| url.startsWith("https://sso.aau.at") 
    					|| url.startsWith("https://campus.aau.at") 
    					|| url.startsWith("https://moodle.aau.at")) {
        			return false;
        		} else if(url.startsWith("http://campus-gis.aau.at/")) {
        			Log.d(TAG, "REDIRECT TO MAP");
        			String roomParameter = "curRouteTo=";
        			int index = url.indexOf(roomParameter);
        			if(index > -1) {
        				MapUtils.openMapAndShowRoom(WebViewActivity.this, 
        						url.substring(index + roomParameter.length()));
        			}
        			return true;
        		}
        		
    			// open external websites in browser
    			Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
        	}
        	@Override
        	public void onPageStarted(WebView view, String url, Bitmap favicon) {
        		progressNotificationOn();
        		super.onPageStarted(view, url, favicon);
        	}
        	@Override
        	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        		super.onReceivedError(view, errorCode, description, failingUrl);
        		progressNotificationOff();
        	}
        	@Override
        	public void onPageFinished(WebView view, String url) {
        		super.onPageFinished(view, url);
        		progressNotificationOff();
        		
        		/*
        		 *  the first page of moodle opens through a redirect so we need to clear the first history
        		 *  entry to avoid execution of the redirect when going back through the history with the back button
        		 */
        		if(mExecuteMoodleHack && mWebView.canGoBack()) {
        			mWebView.clearHistory();
        			mExecuteMoodleHack = false;
        		} else {
        			mIsFirstPage = false;
        		}
        	}
        });
        
        mWebView.setDownloadListener(new DownloadListener() {
			public void onDownloadStart(String url, String userAgent,
                    String contentDisposition, String mimetype,
                    long contentLength) {
				Analytics.onEvent(Analytics.EVENT_WEB_COURSEMOODLE_DOWNLOAD);
            	Uri uri = Uri.parse(url);
            	MoodleDownloadHelper.download(WebViewActivity.this, uri, 
            			Utils.getContentDispositionOrUrlFilename(contentDisposition, uri));
            }
        });
        
        // set session cookie
        // http://stackoverflow.com/questions/1652850/android-webview-cookie-problem
        // http://android.joao.jp/2010/11/cookiemanager-and-removeallcookie.html
        if (Studentportal.getSportalClient().isSessionCookieAvailable()) {
        	CookieSyncManager.createInstance(this);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            //cookieManager.removeSessionCookie(); // NOTE when calling this method the cookies get removed after the next setCookie() gets called
            
            Cookie sessionCookie = Studentportal.getSportalClient().getSessionCookie();
        	cookieManager.setCookie(sessionCookie.getDomain(), Utils.cookieHeaderString(sessionCookie));
        	
        	if(mSSO) {
        		// set SSO/CAS cookie
        		Cookie ssoCookie = Studentportal.getSportalClient().getCookie("CASTGC");
        		if(ssoCookie != null) {
        			cookieManager.setCookie(ssoCookie.getDomain(), Utils.cookieHeaderString(ssoCookie));
        		}
        	}
        	
            CookieSyncManager.getInstance().sync();
        }  
        
        mIsFirstPage = true;
        mWebView.loadUrl(targetUrl);
    }

    @Override
    protected void onStart() {
    	super.onStart();
    	Analytics.onActivityStart(this, Analytics.ACTIVITY_WEBVIEW);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	Analytics.onActivityStop(this);
    }
    
    @Override
    public void onBackPressed() {
    	// navigate back in the history or close the activity
    	if(mWebView != null && mWebView.canGoBack()) {
    		mWebView.goBack();
    	} else {
    		super.onBackPressed();
    	}
    }

	@Override
	public void progressNotificationOn() {
		mActionBar.progressNotificationOn();
		if(mIsFirstPage) {
			findViewById(R.id.progress).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void progressNotificationOff() {
		mActionBar.progressNotificationOff();
		if(mIsFirstPage) {
			findViewById(R.id.progress).setVisibility(View.GONE);
		}
	}
}