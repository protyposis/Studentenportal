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

package at.ac.uniklu.mobile.sportal.util;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.CookieManager;
import android.widget.Toast;
import at.ac.uniklu.mobile.sportal.R;

public class MoodleDownloadHelper {
	
	public static void download(Context context, Uri uri, String filename) {
		DownloadHelper dh = null;
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			dh = new HoneycombDownloader();
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			dh = new GingerbreadDownloader();
		} else {
			dh = new DummyDownloader();
		}
		
		dh.download(context, uri, filename);
	}
	
	private static interface DownloadHelper {
		void download(Context context, Uri uri, String filename);
	}
	
	private static class DummyDownloader implements DownloadHelper {

		@Override
		public void download(Context context, Uri uri, String filename) {
			Toast.makeText(context, context.getString(R.string.error_download_not_supported, Build.VERSION.RELEASE), Toast.LENGTH_SHORT).show();
		}
	}
	
	/* sources: 
	 * http://stackoverflow.com/questions/525204/android-download-intent
	 * http://stackoverflow.com/questions/10940223/is-it-possible-to-submit-cookies-in-an-android-downloadmanager
	 */
	
	@TargetApi(9)
	private static class GingerbreadDownloader implements DownloadHelper {

		@Override
		public void download(Context context, Uri uri, String filename) {
			DownloadManager.Request r = null;
        	
        	try {
        		r = new DownloadManager.Request(uri);
        	} catch (IllegalArgumentException e) {
        		/* Unmodified Android 2.3 doesn't allow downloads over HTTPS - check if
        		 * this is an exception related to this */
        		if(e.getMessage() != null && e.getMessage().contains("HTTP URIs")) {
        			Toast.makeText(context, R.string.error_download_not_supported_https, Toast.LENGTH_SHORT).show();
        			return;
        		} else {
        			// rethrow exception if it's not about HTTPS downloads
        			throw e;
        		}
        	}
        	
        	setupRequest(r, uri, filename);

        	// Start download
        	DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        	dm.enqueue(r);
		}
		
		protected void setupRequest(DownloadManager.Request r, Uri uri, String filename) {
			// add Moodle session cookie
			CookieManager cookieManager = CookieManager.getInstance();
			r.addRequestHeader("Cookie", cookieManager.getCookie("https://moodle.aau.at"));
			
			// This put the download in the same Download dir the browser uses
        	r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
		}
	}
	
	@TargetApi(11)
	private static class HoneycombDownloader extends GingerbreadDownloader {

		@Override
		protected void setupRequest(Request r, Uri uri, String filename) {
			super.setupRequest(r, uri, filename);
			
			Log.d("MoodleDownloadHelper", "SR HC");
			
			// When downloading music and videos they will be listed in the player
        	// (Seems to be available since Honeycomb only)
        	r.allowScanningByMediaScanner();

        	// Notify user when download is completed
        	// (Seems to be available since Honeycomb only)
        	r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
	}

}
