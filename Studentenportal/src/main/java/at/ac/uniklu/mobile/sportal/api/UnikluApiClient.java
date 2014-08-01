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

package at.ac.uniklu.mobile.sportal.api;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import android.content.Context;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.Studentportal;
import at.ac.uniklu.mobile.sportal.persistence.Cache;
import at.ac.uniklu.mobile.sportal.util.Utils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class UnikluApiClient {
	
	private static final String TAG = "ApiClient";
	
	private static final int BUFFER_SIZE = 1024 * 32;
	private static final int REQUIRED_API_VERSION_MAJOR = 1;
	private static final int REQUIRED_API_VERSION_MINOR_MIN = 0;
	
    private static final String CLIENT_VERSION_HEADER = "User-Agent";
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";
	
	private static final String URL_API_VERSIONINFO = "/api/";
	private static final String URL_API_LOGIN = "/api/login";
	private static final String URL_API_LOGOUT = "/api/logout";
	
	/**
	 * The time a session is valid on the campus server. Currently it's 30 minutes.
	 * 30 minutes - 2 minutes safety buffer
	 */
	private static final long SESSION_COOKIE_DURATION = 1000 * 60 * 28;
	
	private String mApiBaseUrl;
	private String mClientVersion;
	private DefaultHttpClient mHttpClient;
	private Gson mGson;
	
	private boolean mCacheAvailable;
	private Cache mCache;
	
	private boolean mDebugBuild;
	private LoginStatus mLoginStatus;
	private long mSessionCookieTime;
	
	public UnikluApiClient(String clientVersionInfo, boolean debug) {
		init();
		mCacheAvailable = false;
		mDebugBuild = false;
		mLoginStatus = new LoginStatus();
		mDebugBuild = debug;
		mClientVersion = clientVersionInfo;
	}
	
	public UnikluApiClient(String clientVersionInfo, boolean debug, Context applicationContext, boolean useCache) {
		this(clientVersionInfo, debug);
		
		if(useCache) {
			mCache = new Cache(applicationContext);
			mCacheAvailable = true;
			logDebug("api client cache enabled");
		}
	}
	
	private void init() {
		// init http client
		int timeout = 20000;
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
		HttpConnectionParams.setSoTimeout(httpParams, timeout);
		mHttpClient = new DefaultHttpClient(httpParams);
		mHttpClient.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
			@Override
			public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
				logDebug("request retry " + executionCount + ": " + exception.getClass().getName());
				if(executionCount < 3) {
					if(exception instanceof SSLException) {
						return true;
					}
					if(exception instanceof SocketTimeoutException) {
						return true;
					}
					if(exception instanceof ConnectTimeoutException) {
						return true;
					}
				}
				return false;
			}
		});
		
		// init gson & configure it to match server's output format
		mGson = new GsonBuilder()
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
				.registerTypeAdapter(Notification.Type.class, new GsonNotificationTypeDeserializer())
				.create();
	}
	
	public String getBaseUrl() {
		return mApiBaseUrl;
	}

	public void setBaseUrl(String apiBaseUrl) {
		this.mApiBaseUrl = apiBaseUrl;
	}

	/**
	 * Calls a JSON-method on the server and returns its response or throws an exception if something goes wrong.
	 * @param apiMethod the method to call
	 * @param useCache specifies if the local cache should be used (reads from the cache if valid data is available, else writes the server response to the cache)
	 * @return the result of the JSON-method
	 * @throws ApiClientException if something goes wrong on the client side
	 * @throws ApiServerException if something goes wrong on the server side
	 */
	private synchronized String get(String apiMethod, boolean useCache, 
			NameValuePair... queryParams) throws ApiClientException, ApiServerException {
		String result = null;
		long startTime = System.currentTimeMillis();
		apiMethod = apiMethod + Utils.buildQuery(queryParams);
		
		// try to read the requested data from the cache
		if(mCacheAvailable && useCache && mLoginStatus.isLoggedIn()) {
			String cacheResult = mCache.get(apiMethod, mLoginStatus.getId());
			logDebug("cache lookup " + (cacheResult == null ? "MISS" : "HIT") + ": " + apiMethod);
			if(cacheResult != null) {
				return cacheResult;
			}
		}
		
		try {
			HttpGet httpGet = new HttpGet((apiMethod.startsWith("http") ? "" : mApiBaseUrl) + apiMethod);
			httpGet.addHeader(CLIENT_VERSION_HEADER, mClientVersion);
			String requestUrl = httpGet.getURI().toString();
			if(requestUrl != null && requestUrl.contains("password=")) {
				requestUrl = requestUrl.replaceFirst("password=[^&]*", "password=*****");
			}
			logDebug("GET " + requestUrl);
			
			// call the remote API method
			HttpResponse httpResponse = mHttpClient.execute(httpGet);
			handleResponse(httpResponse);
	
			// read the method response into a string
			result = readResponseString(httpResponse);
		}
		catch (Exception e) {
			processException(e);
		}
		
		if(mLoginStatus.isLoggedIn()) {
			// put the requested data into the cache
			if(mCacheAvailable && useCache) {
				mCache.put(apiMethod, mLoginStatus.getId(), result);
				logDebug("cached: " + apiMethod);
			}
			
			// after a successful (logged in) request, the cookie time can be reset
			mSessionCookieTime = System.currentTimeMillis();
		} else {
			// if not logged in, invalidate the cookie time to avoid not-logged-in requests
			mSessionCookieTime = 0;
		}
		
		logDebug("get string: " + (System.currentTimeMillis() - startTime) + "ms");
		
		return result;
	}
	
	private synchronized void post(String apiMethod, String data, 
			NameValuePair... queryParams) throws ApiClientException, ApiServerException {
		long startTime = System.currentTimeMillis();
		apiMethod = apiMethod + Utils.buildQuery(queryParams);
		
		try {
			HttpPost httpPost = new HttpPost((apiMethod.startsWith("http") ? "" : mApiBaseUrl) + apiMethod);
			httpPost.addHeader(CLIENT_VERSION_HEADER, mClientVersion);
			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setEntity(new StringEntity(data, HTTP.UTF_8));
			logDebug("POST " + httpPost.getURI().toString());

			// call the remote API method
			HttpResponse httpResponse = mHttpClient.execute(httpPost);
			handleResponse(httpResponse);
		} catch (Exception e) {
			processException(e);
		}
		
		logDebug("post string: " + (System.currentTimeMillis() - startTime) + "ms");
	}
	
	private synchronized void delete(String apiMethod, 
			NameValuePair... queryParams) throws ApiClientException, ApiServerException {
		long startTime = System.currentTimeMillis();
		apiMethod = apiMethod + Utils.buildQuery(queryParams);
		
		try {
			HttpDelete httpDelete = new HttpDelete((apiMethod.startsWith("http") ? "" : mApiBaseUrl) + apiMethod);
			httpDelete.addHeader(CLIENT_VERSION_HEADER, mClientVersion);
			logDebug("DELETE " + httpDelete.getURI().toString());

			// call the remote API method
			HttpResponse httpResponse = mHttpClient.execute(httpDelete);
			handleResponse(httpResponse);
		} catch (Exception e) {
			processException(e);
		}
		
		logDebug("delete: " + (System.currentTimeMillis() - startTime) + "ms");
	}
	
	private String readResponseString(HttpResponse httpResponse) throws 
			IllegalStateException, IOException {
		// NOTE this can be optimized by directly handing the input stream over to the JSON parser
		HttpEntity entity = httpResponse.getEntity();
		if (entity != null) {
			InputStream inputStream = entity.getContent();
			String result = Utils.readString(inputStream, BUFFER_SIZE);
			logDebug("response data: " + result);
			inputStream.close();
			return result;
		}
		return null;
	}
	
	private void handleResponse(HttpResponse httpResponse) throws 
			JsonSyntaxException, IllegalStateException, 
			ApiServerException, IOException {
		StatusLine httpResponseStatus = httpResponse.getStatusLine();
		logDebug("response code: " + httpResponseStatus.toString());
		
		if(httpResponseStatus.getStatusCode() != HttpStatus.SC_OK) {
			// if the status code isn't 200 OK, something went wrong
			Header[] contentTypeHeaders = httpResponse.getHeaders("Content-Type");
			if(contentTypeHeaders != null && contentTypeHeaders.length > 0 
					&& !contentTypeHeaders[0].getValue().contains("application/json")) {
				// server didn't respond with JSON (which means the call didn't even reach the server API)
				Error error = new Error();
				error.setCode(httpResponseStatus.getStatusCode());
				error.setMessage("illegal data format received");
				throw new ApiServerException(error);
			}
			else {
				// server answered with a JSON error object
				throw new ApiServerException(mGson.fromJson(readResponseString(httpResponse), Error.class));
			}
		}
	}
	
	private void processException(Exception e) throws ApiClientException, ApiServerException {
		if (e instanceof ApiServerException) {
			ApiServerException e2 = (ApiServerException)e;
			if(e2.getError() != null && e2.getError().getCode() == 401) {
				// invalidate cookie on 401 insufficient rights exception
				mSessionCookieTime = 0;
			}
			// relay the exception to the caller
			throw e2;
		}
		else if (e instanceof SocketException) { // includes ConnectException
			throw new ApiClientException(ApiClientException.Code.UNKNOWNHOST, e);
		}
		else if (e instanceof UnknownHostException) {
			throw new ApiClientException(ApiClientException.Code.UNKNOWNHOST, e);
		}
		else if (e instanceof SocketTimeoutException) {
			throw new ApiClientException(ApiClientException.Code.TIMEOUT, e);
		}
		else if (e instanceof ConnectTimeoutException) {
			throw new ApiClientException(ApiClientException.Code.TIMEOUT, e);
		}
		else if (e instanceof SSLException) {
			e.printStackTrace();
			throw new ApiClientException(ApiClientException.Code.SSL, e);
		}
		else if (e instanceof IOException) {
			e.printStackTrace();
			throw new ApiClientException(ApiClientException.Code.READING_RESPONSE, e);
		}
		else if (e instanceof IllegalStateException) {
			e.printStackTrace();
			throw new ApiClientException("Illegal state!?!", e);
		}
		else if (e instanceof Exception) {
			e.printStackTrace();
			throw new ApiClientException("Unknown Exception: " + e.getMessage(), e);
		}
	}
	
	private synchronized String get(String apiMethod, boolean useCache, boolean needsAuthentication, 
			NameValuePair... queryParams) throws ApiClientException, ApiServerException {
        try {
            // execute an ordinary request
            return get(apiMethod, useCache, queryParams);
        } catch(ApiServerException e) {
            /* if the request fails, and the reason is a missing authorization, 
             * and the method needs authorization, log in and retry the request
             */
            if(e.getError().getCode() == 401 && needsAuthentication) {
                UsernamePasswordCredentials credentials = Studentportal.getUsernamePasswordCredentials();
                logDebug("re-login...");
                if(login(credentials.getUserName(), credentials.getPassword(), null).isLoggedIn()) {
                    return get(apiMethod, useCache, queryParams);
                }
            }
            // if the exception has another reason, continue with the exception
            throw e;
        }
    }
	
	private synchronized void post(String apiMethod, String data, boolean needsAuthentication, 
			NameValuePair... queryParams) throws ApiClientException, ApiServerException {
        try {
            // execute an ordinary request
            post(apiMethod, data, queryParams);
        } catch(ApiServerException e) {
            /* if the request fails, and the reason is a missing authorization, 
             * and the method needs authorization, log in and retry the request
             */
            if(e.getError().getCode() == 401 && needsAuthentication) {
                UsernamePasswordCredentials credentials = Studentportal.getUsernamePasswordCredentials();
                logDebug("re-login...");
                if(login(credentials.getUserName(), credentials.getPassword(), null).isLoggedIn()) {
                    post(apiMethod, data, queryParams);
                }
            }
            // if the exception has another reason, continue with the exception
            throw e;
        }
    }
	
	public synchronized void delete(String apiMethod, boolean needsAuthentication, 
			NameValuePair... queryParams) throws ApiClientException, ApiServerException {
        try {
            // execute an ordinary request
            delete(apiMethod, queryParams);
        } catch(ApiServerException e) {
            /* if the request fails, and the reason is a missing authorization, 
             * and the method needs authorization, log in and retry the request
             */
            if(e.getError().getCode() == 401 && needsAuthentication) {
                UsernamePasswordCredentials credentials = Studentportal.getUsernamePasswordCredentials();
                logDebug("re-login...");
                if(login(credentials.getUserName(), credentials.getPassword(), null).isLoggedIn()) {
                    delete(apiMethod, queryParams);
                }
            }
            // if the exception has another reason, continue with the exception
            throw e;
        }
    }
	
	/**
	 * Calls a JSON-method on the server and returns its response as an object 
	 * instance or throws an exception if something goes wrong.
	 * @see #get(String, boolean)
	 */
	public <T> T get(String method, boolean useCache, boolean needsAuthentication, Class<T> classOfT, 
			NameValuePair... queryParams) throws ApiClientException, ApiServerException {
		long getStartTime = System.currentTimeMillis();
		String getResult = get(method, useCache, needsAuthentication, queryParams);
		long gsonStartTime = System.currentTimeMillis();
		T result = mGson.fromJson(getResult, classOfT);
		long endTime = System.currentTimeMillis();
		logDebug("get object class: " + (endTime - gsonStartTime) + "ms");
		logDebug("get total: " + (endTime - getStartTime) + "ms");
		return result;
	}
	
	public <T> T get(String method, boolean useCache, Class<T> classOfT, NameValuePair... queryParams) 
			throws ApiClientException, ApiServerException {
		return get(method, useCache, true, classOfT, queryParams);
	}
	
	/**
	 * Calls a JSON-method on the server and returns its response as an object 
	 * instance or throws an exception if something goes wrong.
	 * If the cache is enabled, it will be used.
	 * @see #get(String, boolean)
	 */
	public <T> T get(String method, Class<T> classOfT, NameValuePair... queryParams) 
			throws ApiClientException, ApiServerException {
		return get(method, mCacheAvailable, true, classOfT, queryParams);
	}
	
	/**
	 * Calls a JSON-method on the server and returns its response as an object 
	 * instance (list) or throws an exception if something goes wrong.
	 * @see #get(String, boolean)
	 */
	public <T> T get(String method, boolean useCache, Type typeOfT, NameValuePair... queryParams) 
			throws ApiClientException, ApiServerException {
		long getStartTime = System.currentTimeMillis();
		String getResult = get(method, useCache, true, queryParams);
		long gsonStartTime = System.currentTimeMillis();
		T result = mGson.fromJson(getResult, typeOfT);
		long endTime = System.currentTimeMillis();
		logDebug("get object type: " + (endTime - gsonStartTime) + "ms");
		logDebug("get total: " + (endTime - getStartTime) + "ms");
		return result;
	}
	
	/**
	 * Calls a JSON-method on the server and returns its response as an object 
	 * instance (list) or throws an exception if something goes wrong.
	 * If the cache is enabled, it will be used.
	 * @see #get(String, boolean)
	 */
	public <T> T get(String method, Type typeOfT, NameValuePair... queryParams) 
			throws ApiClientException, ApiServerException {
		return get(method, mCacheAvailable, typeOfT, queryParams);
	}
	
	public void post(String method, Object data, boolean needsAuthentication, 
			NameValuePair... queryParams) throws ApiClientException, ApiServerException {
		post(method, mGson.toJson(data), needsAuthentication, queryParams);
	}
	
	public VersionInfo getVersionInfo() throws ApiClientException, ApiServerException {
		return get(URL_API_VERSIONINFO, false, false, VersionInfo.class);
	}

	public LoginStatus login(String username, String password, String cardId) throws ApiClientException, ApiServerException {
		// if the session cookie is still valid, skip the server communication to speed up the login process
		if(System.currentTimeMillis() - mSessionCookieTime < SESSION_COOKIE_DURATION && getLoginStatus().isLoggedIn()) {
			logDebug("skipping remote login: " + getLoginStatus().isLoggedIn());
			return getLoginStatus();
		}
		
		NameValuePair[] params = null;
		if(username != null && password != null) {
			params = new BasicNameValuePair[] {
					new BasicNameValuePair("username", username),
					new BasicNameValuePair("password", password)
			};
		} else if(cardId != null) {
			params = new BasicNameValuePair[] {
					new BasicNameValuePair("cardid", cardId)
			};
		}
		
		/* LOGIN
		 * for a "normal" login, which is a username/password combination without 
		 * a xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx, we can authenticate directly 
		 * at the CAS server which is an advantage since protected webpages will be 
		 * accessible by webviews without the need of a separate manual login
		 */
		boolean skipCas = Studentportal.isDebugBuild() && !mApiBaseUrl.startsWith("https");
		LoginStatus loginStatus = null;
		if(!skipCas && username != null && password != null && !username.contains(":")) {
			/* authenticate at the CAS server 
			 * (service must be there to establish a session and gather a session cookie)
			 */
			params = new NameValuePair[] {
					new BasicNameValuePair("service", mApiBaseUrl + "/j_spring_cas_security_check"),
					params[0], // username
					params[1] // password
			};
			get("https://sso.uni-klu.ac.at/cas/login", false, false, params);
			/* just read the login status 
			 * if the CAS login succeeded it returns a valid logged-in status
			 */
			loginStatus = login();
		} else {
			loginStatus = get(URL_API_LOGIN, false, false, LoginStatus.class, params);
		}
		
		mLoginStatus = loginStatus;
		
		/* At login, the cookie time can't be set in the get() call since the login status isn't decoded
		 * there yet and it would need a subsequent request for the time to be set; e.g. the upcoming dates,
		 * which only works if they aren't taken from the cache. Setting the time here ensures independence
		 * of subsequent requests. */
		mSessionCookieTime = loginStatus.isLoggedIn() ? System.currentTimeMillis() : 0;
		
		return loginStatus;
	}
	
	public LoginStatus login() throws ApiClientException, ApiServerException {
		return login(null, null, null);
	}
	
	public LoginStatus getLoginStatus() {
		return mLoginStatus;
	}
	
	public LoginStatus logout() throws ApiClientException, ApiServerException {
		mLoginStatus = get(URL_API_LOGOUT, false, false, LoginStatus.class);
		return mLoginStatus;
	}
	
	public boolean isSessionCookieAvailable() {
		return getSessionCookie() != null;
	}
	
	public Cookie getCookie(String name) {
		// http://stackoverflow.com/questions/1652850/android-webview-cookie-problem
		Cookie cookie = Utils.getCookie(mHttpClient, name);
		if(cookie != null) {
			logDebug(name + " cookie: " + cookie.toString());
		} else {
			logDebug("no " + name + " cookie available");
		}
		return cookie;
	}
	
	public Cookie getSessionCookie() {
		return getCookie(SESSION_COOKIE_NAME);
	}
	
	public void clearSessionCookie() {
        mHttpClient.getCookieStore().clear();
        mLoginStatus = new LoginStatus();
    }
	
	public void clearCache() {
		if(mCacheAvailable) {
			mCache.clear();
		}
	}

	public void clearCache(String method) {
		if(mCacheAvailable) {
			mCache.clear(method + "%", mLoginStatus.getId());
		}
	}
	
	public void checkApiVersion() throws ApiClientException, ApiServerException {
		VersionInfo versionInfo = getVersionInfo();
		
		String serverApiVersion = versionInfo.getMajor() + "." + versionInfo.getMinor();
		String requiredApiVersion = REQUIRED_API_VERSION_MAJOR + "." + REQUIRED_API_VERSION_MINOR_MIN;
		
		if(versionInfo.getMajor() != REQUIRED_API_VERSION_MAJOR || 
				versionInfo.getMinor() < REQUIRED_API_VERSION_MINOR_MIN) {
			throw new ApiClientException("Cannot connect to the server, incompatible API version (served " 
					+ serverApiVersion + " / required " + requiredApiVersion + ").");
		}
	}
	
	private void logDebug(String message) {
		if(mDebugBuild) {
			Log.d(TAG, message);
		}
	}

	public boolean isMifareLoginSupported() {
		return true;
	}
	
	protected String buildArrayParamValue(Integer... values) {
		StringBuilder sb = new StringBuilder();
		
		if(values == null || values.length == 0) {
			return null;
		}
		
		sb.append(values[0]);
		if(values.length > 1) {
			for(int i = 1; i < values.length; i++) {
				sb.append(",").append(values[i]);
			}
		}
		
		return sb.toString();
	}
}
