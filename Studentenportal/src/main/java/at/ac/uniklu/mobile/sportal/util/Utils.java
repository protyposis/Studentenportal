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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.R;
import at.ac.uniklu.mobile.sportal.Studentportal;
import at.ac.uniklu.mobile.sportal.api.ApiServerException;

public class Utils {
	
	public static final long MILLIS_PER_HOUR = 1000 * 60 * 60;
	public static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * 24;
	public static final long MILLIS_PER_WEEK = MILLIS_PER_DAY * 7;
	
	private static final String TAG = "Utils";

	/**
	 * Downloads a file from the network.
	 * 
	 * This function can fail in some cases (e.g. slow network), so if you really need the file, it
	 * is recommended to do a few retries.
	 * source: http://code.google.com/p/android/issues/detail?id=6066
	 *
	 * @param sourceUrl the url where the file is located
	 * @param retries the number of retries in case the download fails
	 * @return the stream if successful, else null
	 */
	public static byte[] downloadFile(String sourceUrl, int retries) {
		byte[] data = null;
		
		boolean successful = false;
		retries++; // add the first try
		do {
			try {
				retries--;
				URL url = new URL(sourceUrl);
				URLConnection urlConnection = url.openConnection();
				
				// set session cookie for authorization purposes (loading id pictures without being logged in is now [2011-07-04] disabled)
				Cookie cookie = Studentportal.getSportalClient().getSessionCookie();
				if (cookie != null) {
		        	urlConnection.addRequestProperty("Cookie", cookieHeaderString(cookie));
		        } 
				
				InputStream in = new BufferedInputStream(urlConnection.getInputStream(), 1024 * 32);
				data = streamToArray(in);
				in.close();
				successful = true;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// if the file doesn't exist, it's pointless to retry
				Log.d(TAG, "file does not exist: " + sourceUrl);
				retries = 0;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while(!successful && retries > 0);
		
		return data;
	}
	
	/**
	 * @see #downloadFile(String, int)
	 */
	public static byte[] downloadFile(String sourceUrl) {
		return downloadFile(sourceUrl, 0);
	}
	
	/**
	 * Loads a bitmap for the network or from a cached file, if it has already been downloaded before. If the file
	 * is loaded for the first time, it is written to the cache to be available next time.
	 * 
	 * @param context
	 * @param sourceUrl
	 * @param cacheFileName
	 * @return a bitmap or null if something went wrong
	 */
	public static Bitmap downloadBitmapWithCache(Context context, String sourceUrl, String cacheFileName) throws Exception {
		Bitmap bitmap = null;
		File cacheFile = new File(context.getCacheDir(), cacheFileName);
		
		boolean cached = cacheFile.exists();
		boolean cachedValid = cached && cacheFile.lastModified() > (System.currentTimeMillis() - MILLIS_PER_WEEK);
		
		if(cached && cachedValid) {
			// the requested file is cached, load it
			Log.d(TAG, "loading cached file: " + cacheFileName);
			bitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
		} else {
			// the requested file is not cached, download it from the network
			Log.d(TAG, "file " + (cachedValid ? "too old" : "not cached") + ", downloading: " + sourceUrl);
			byte[] data = downloadFile(sourceUrl, 3);
			if(data != null) {
				bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				if(bitmap != null) {
					// the download succeeded, cache the file so it is available next time
					FileOutputStream out = new FileOutputStream(cacheFile);
					out.write(data, 0, data.length);
					out.flush();
					out.close();
				} else {
					Log.e(TAG, "failed to download bitmap, may be an unsupported format: " + sourceUrl);
				}
			}
		}
		
		return bitmap;
	}
	
	public static byte[] streamToArray(InputStream is) throws IOException {
		/* http://stackoverflow.com/questions/5333908/fileinputstream-to-byte-array-in-android-application */
		/* http://stackoverflow.com/questions/2436385/android-getting-from-a-uri-to-an-inputstream-to-a-byte-array
		 * this method does not work as is.available() always returns 0 for streams from an url connection */
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] b = new byte[1024];
		int bytesRead = 0;
		while ((bytesRead = is.read(b)) != -1) {
		   bos.write(b, 0, bytesRead);
		}
		return bos.toByteArray();
	}
	
    /**
     * Compares two dates and returns true if they are on the same day, else it returns false.
     * source: http://stackoverflow.com/questions/2517709/java-comparing-two-dates-to-see-if-they-are-in-the-same-day
     * TODO this method should handle timezone offsets
     */
    public static boolean isSameDay(Date date1, Date date2) {
        // Strip out the time part of each date.
        long julianDayNumber1 = date1.getTime() / MILLIS_PER_DAY;
        long julianDayNumber2 = date2.getTime() / MILLIS_PER_DAY;
        
        // If they now are equal then it is the same day.
        return julianDayNumber1 == julianDayNumber2;
    }
    
    public static boolean isToday(Date date) {
    	return DateUtils.isToday(date.getTime());
    }
    
    public static boolean isTomorrow(Date date) {
    	return DateUtils.isToday(date.getTime() - MILLIS_PER_DAY);
    }
    
    /**
     * Returns the midnight that starts the day (00:00)
     */
    public static Date getMidnight(Date date) {
    	Calendar c = Calendar.getInstance();
    	c.setTime(date);
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.HOUR_OF_DAY, 0);
    	return c.getTime();
    }
    
    public static String cookieHeaderString(Cookie sessionCookie) {
    	return sessionCookie.getName() + "=" + sessionCookie.getValue();
    }
    
    public static Cookie getCookie(DefaultHttpClient httpClient, String cookieName) {
    	List<Cookie> cookies = httpClient.getCookieStore().getCookies();
		if (!cookies.isEmpty()) {
		    for (Cookie cookie : cookies) {
		    	if(cookie.getName().equals(cookieName)) {
		    		return cookie;
		    	}
		    }
		}
		return null;
    }
    
    /**
     * Reads the contents of an {@link InputStream} into a string.
     * @param inputStream
     * @param bufferSize
     * @return
     * @throws IOException
     */
    public static String readString(InputStream inputStream, int bufferSize) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), bufferSize);
		StringBuilder sb = new StringBuilder();

		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		
		return sb.toString();
    }
    
    public static boolean isMissingAuthenticationException(Exception e) {
    	return e instanceof ApiServerException && 
    			((ApiServerException)e).getError().getCode() == 401;
    }
    
    public static String getCSV(Collection<String> collection, String separator) {
    	if(collection == null || collection.isEmpty()) {
    		return null;
    	}
    	StringBuffer sb = new StringBuffer();
    	for(String s : collection) {
    		sb.append(separator).append(s);
    	}
    	return sb.toString();
    }
    
	public static String buildQuery(NameValuePair... queryParams) {
		if(queryParams == null || queryParams.length == 0) {
			return "";
		}
		
		String query = "";
		for(NameValuePair param : queryParams) {
			if(query.length() > 0) {
				query += "&";
			}
			
			String name = param.getName();
			String value = param.getValue();
			
			try {
				value = value != null ? URLEncoder.encode(value, HTTP.UTF_8) : "";
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Unsupported encoding", e);
			}
			
			query += name + "=" + value;
		}
		
		return "?" + query;
	}
	
	/**
	 * Deletes all content from the application's cache directory.
	 * source: http://groups.google.com/group/android-developers/browse_thread/thread/e9eb0a17a3c7c768
	 * 
	 * @param context
	 */
	public static void clearAppCache(Context context) { 
        try { 
            File dir = context.getCacheDir(); 
            if (dir != null && dir.isDirectory()) { 
                deleteDir(dir); 
            } 
        } catch (Exception e) { 
            // TODO: handle exception 
        } 
    }
	
	/**
	 * Recursively delete a directory and all of its content.
	 * source: http://groups.google.com/group/android-developers/browse_thread/thread/e9eb0a17a3c7c768
	 * @param dir
	 * @return
	 */
    public static boolean deleteDir(File dir) { 
        if (dir!=null && dir.isDirectory()) { 
            String[] children = dir.list(); 
            for (int i = 0; i < children.length; i++) { 
                boolean success = deleteDir(new File(dir, children[i])); 
                if (!success) { 
                    return false; 
                } 
            } 
        } 
        // The directory is now empty so delete it 
        return dir.delete(); 
    }
    
    public static String getContentDispositionFilename(String contentDisposition) {
    	String prefix = "filename=\"";
    	String suffix = "\"";
    	
    	if(contentDisposition != null && contentDisposition.contains(prefix)) {
    		int start = contentDisposition.indexOf(prefix);
    		int end = contentDisposition.lastIndexOf(suffix);
    		if(start > -1 && end > -1 && start < end) {
    			return contentDisposition.substring(start + prefix.length(), end);
    		}
    	}
    	
    	return null;
    }
    
    public static String getContentDispositionOrUrlFilename(String contentDisposition, Uri uri) {
    	String filename = getContentDispositionFilename(contentDisposition);
    	if(filename == null) {
    		filename = uri.getLastPathSegment();
    	}
    	return filename;
    }
    
    /**
     * Returns the value of the EXTRA_REFETCH_DATA flag of an intent, and removes it
     * if it is set. This flag tells an Activity if it should load the data freshly from the
     * server by invalidating the cache.
     */
    public static boolean shouldRefetchData(Intent i) {
    	boolean refetch = i.getBooleanExtra(Studentportal.EXTRA_REFETCH_DATA, false);
    	if(refetch) {
    		i.removeExtra(Studentportal.EXTRA_REFETCH_DATA);
    	}
    	return refetch;
    }
    
    /**
     * Creates an intent to open the app page on facebook if the fb-app is installed,
     * else opens the page in the browser.
     * taken from: http://stackoverflow.com/questions/4810803/open-facebook-page-from-android-app
     */
    public static Intent getOpenFacebookIntent(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            return new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.credits_facebookpage_appurl)));
        } catch (Exception e) {
            return new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.credits_facebookpage_fullurl)));
        }
    }
}
