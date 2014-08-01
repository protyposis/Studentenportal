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

package at.ac.uniklu.mobile.sportal.ui;

import android.graphics.Bitmap;
import android.os.Handler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewTimeoutClient extends WebViewClient {
	
	private long mTimeoutMillis;
	private boolean timeout;
	
	// http://stackoverflow.com/questions/7772409/set-loadurltimeoutvalue-on-webview
	private Handler mTimeoutHandler = new Handler();
	private Runnable mTimeoutTrigger = new Runnable() {
        public void run() {
            if(timeout) {
                onTimeout();
            }
        }
    };
	
	public WebViewTimeoutClient(long timeoutMillis) {
		mTimeoutMillis = timeoutMillis;
	}
	
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		super.onPageStarted(view, url, favicon);
		timeout = true;
		mTimeoutHandler.postDelayed(mTimeoutTrigger, mTimeoutMillis);
	}
	
	@Override
	public void onPageFinished(WebView view, String url) {
		super.onPageFinished(view, url);
		timeout = false;
	}
	
	public void onTimeout() {
		
	}
}
