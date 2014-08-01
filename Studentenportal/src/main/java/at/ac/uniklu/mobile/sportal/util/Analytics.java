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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.Studentportal;
import at.ac.uniklu.mobile.sportal.api.ApiClientException;

import com.bugsense.trace.BugSenseHandler;
import com.flurry.android.FlurryAgent;

/**
 * Facility for usage and error logging services. Currently uses Flurry to report this data.
 */
public class Analytics {
	
	public static final String ACTIVITY_LOGIN = "a.login";
	public static final String ACTIVITY_DASHBOARD = "a.dashboard";
	public static final String ACTIVITY_COURSES = "a.courses";
	public static final String ACTIVITY_CALENDAR = "a.calendar";
	public static final String ACTIVITY_EXAMS = "a.exams";
	public static final String ACTIVITY_GRADES = "a.grades";
	public static final String ACTIVITY_PROFILE = "a.profile";
	public static final String ACTIVITY_BUS_DEPARTURES = "a.busdepartures";
	public static final String ACTIVITY_BUS_STOPS = "a.busstops";
	public static final String ACTIVITY_WEBVIEW = "a.webview";
	public static final String ACTIVITY_DEBUG = "a.debug";
	public static final String ACTIVITY_DEBUG_PREFERENCES = "a.debugpreferences";
	public static final String ACTIVITY_PREFERENCES = "a.preferences";
	public static final String ACTIVITY_MAP = "a.map";
	public static final String ACTIVITY_COURSE_CHECKLISTS = "a.course.checklists";
	public static final String ACTIVITY_COURSE_CHECKLIST = "a.course.checklist";
	public static final String ACTIVITY_COURSE_PARTICIPANTS = "a.course.participants";
	public static final String ACTIVITY_MENSA = "a.mensa";
	
	public static final String FRAGMENT_COURSES = "f.courses";
	public static final String FRAGMENT_EXAMS = "f.exams";
	
	public static final String EVENT_CLEAR_CACHE = "e.clearcache";
	public static final String EVENT_LOGIN = "e.login";
	public static final String EVENT_LOGOUT = "e.logout";
	public static final String EVENT_ABOUT = "e.about";
	public static final String EVENT_AUTOMUTE_TOGGLESWITCH = "e.automutetoggleswitch";
	public static final String EVENT_WEB_COURSEDETAILS = "e.web.coursedetails";
	public static final String EVENT_WEB_COURSEWEBSITE = "e.web.coursewebsite";
	public static final String EVENT_WEB_COURSEMOODLE = "e.web.coursemoodle";
	public static final String EVENT_WEB_COURSEMOODLE_DOWNLOAD = "e.web.coursemoodle.download";
	public static final String EVENT_WEB_GRADEDETAILS = "e.web.gradedetails";
	public static final String EVENT_WEB_EXAMDETAILS = "e.web.examdetails";
	public static final String EVENT_MENU_LOGIN = "e.menu.login";
	public static final String EVENT_MENU_DASHBOARD = "e.menu.dashboard";
	public static final String EVENT_MENU_DEPARTUREMONITOR = "e.menu.departuremonitor";
	public static final String EVENT_MENU_DEPARTUREMONITOR_STOPS = "e.menu.departuremonitor.stops";
	public static final String EVENT_CONTEXTMENU_COURSE = "e.contextmenu.course";
	public static final String EVENT_CONTEXTMENU_COURSE_PARTICIPANT = "e.contextmenu.courseparticipant";
	public static final String EVENT_IGNORE_COURSE = "e.ignorecourse";
	public static final String EVENT_LOAD_BUS_DEPARTURES = "e.loadbusdepartures";
	public static final String EVENT_DEPARTURES_STOPS_REFRESH = "e.departuremonitor.stops.refresh";
	public static final String EVENT_MAP_LAYERSWITCH_CAMPUS = "e.map.layerswitch.campus";
	public static final String EVENT_MAP_LAYERSWITCH_POI = "e.map.layerswitch.poi";
	public static final String EVENT_MAP_SEARCH = "e.map.search";
	public static final String EVENT_MAP_UNAVAILABLE = "e.map.unavailable";
	public static final String EVENT_COURSE_CHECKLISTS = "e.course.checklists";
	public static final String EVENT_COURSE_CHECKLIST = "e.course.checklist";
	public static final String EVENT_COURSE_CHECKLIST_SAVE = "e.course.checklist.save";
	public static final String EVENT_COURSE_PARTICIPANTS = "e.course.participants";
	public static final String EVENT_MENSA_SELECT = "e.mensa.select";
	public static final String EVENT_MENSA_REFRESH = "e.mensa.refresh";
	
	public static final String EVENT_HEADER_HOME = "e.header.home";
	
	public static final String SERVICE_MUTING = "s.muting";
	
	public static final String EVENT_MUTINGSERVICE_ON = "e.ms.on";
	public static final String EVENT_MUTINGSERVICE_OFF = "e.ms.off";
	public static final String EVENT_MUTINGSERVICE_OVERRIDE = "e.ms.override";
	public static final String EVENT_MUTINGSERVICE_MUTE_SCHEDULE = "e.ms.schedulemute";
	public static final String EVENT_MUTINGSERVICE_MUTE = "e.ms.mute";
	public static final String EVENT_MUTINGSERVICE_UNMUTE_SCHEDULE = "e.ms.scheduleunmute";
	public static final String EVENT_MUTINGSERVICE_UNMUTE = "e.ms.unmute";
	
	public static final String EVENT_WIDGET_SIMPLECALENDAR_ADD = "e.w.simplecalendar.add";
	public static final String EVENT_WIDGET_SIMPLECALENDAR_REMOVE = "e.w.simplecalendar.remove";
	
	public static final String EVENT_GCM_REGISTERED = "e.gcm.registered";
	public static final String EVENT_GCM_UNREGISTERED = "e.gcm.unregistered";
	public static final String EVENT_GCM_MSG_NOTIFICATIONTICKLE = "e.gcm.msg.notificationtickle";
	public static final String EVENT_GCM_MSG_BROADCAST = "e.gcm.msg.broadcast";
	public static final String EVENT_GCM_MSG_UNKNOWN = "e.gcm.msg.unknown";
	
	public static final String ERROR_GENERIC = "ex.generic";
	public static final String ERROR_LOGIN_FAILED = "ex.loginfailed";
	public static final String ERROR_LOGOUT_FAILED = "ex.logoutfailed";
	public static final String ERROR_UPCOMING_DATES = "ex.upcomingdates";
	public static final String ERROR_VERSIONINFO = "ex.versioninfo";
	public static final String ERROR_MUTINGSERVICE_ONLINEUPDATE = "ex.ms.onlineupdate";
	public static final String ERROR_WIDGET_SIMPLECALENDAR_UPDATE = "ex.w.simplecalendar.update";
	public static final String ERROR_NOTIFICATIONS_REQUEST = "ex.notifications";
	public static final String ERROR_GCM = "ex.gcm";
	public static final String ERROR_GCM_CLEAR_REGID = "ex.gcm.clearregid";
	
	@SuppressWarnings("unused")
	private static final String TAG = "Analytics";

	private static final String FLURRY_API_KEY_DEBUG = "XXXXXXXXXXXXXXXXXXXX";
	private static final String FLURRY_API_KEY_RELEASE = "XXXXXXXXXXXXXXXXXXXX";
	
	private static final String BUGSENSE_API_KEY_DEBUG = "XXXXXXXX";
	private static final String BUGSENSE_API_KEY_RELEASE = "XXXXXXXX";
	
	private static String mFlurryApiKey;
	private static String mBugsenseApiKey;
	private static String mUserId;
	
	private static Queue<String> mActionHistory;

	public static void init(Context context) {
		mActionHistory = new BoundedLinkedList<String>(8);
		if(Studentportal.isDebugBuild()) {
			mFlurryApiKey = FLURRY_API_KEY_DEBUG;
			mBugsenseApiKey = BUGSENSE_API_KEY_DEBUG;
		} else {
			mFlurryApiKey = FLURRY_API_KEY_RELEASE;
			mBugsenseApiKey = BUGSENSE_API_KEY_RELEASE;
		}
		BugSenseHandler.initAndStartSession(context, mBugsenseApiKey);
	}
	
	public static synchronized void onActivityStart(Context context, String activityId) {
		FlurryAgent.setUseHttps(true);
		FlurryAgent.onStartSession(context, mFlurryApiKey);
		FlurryAgent.setUserId(getUserId(context));
		FlurryAgent.onEvent(activityId);
		FlurryAgent.onPageView();
		
		BugSenseHandler.addCrashExtraData("userId", getUserId(context));
		
		addBreadcrumb(activityId);
	}
	
	public static synchronized void onActivityStop(Context context) {
		FlurryAgent.onEndSession(context);
	}
	
	public static synchronized void onServiceStart(Context context) {
		FlurryAgent.setUseHttps(true);
		FlurryAgent.onStartSession(context, mFlurryApiKey);
		FlurryAgent.setUserId(getUserId(context));
		
		BugSenseHandler.addCrashExtraData("userId", getUserId(context));
	}
	
	public static synchronized void onServiceStart(Context context, String serviceId) {
		onServiceStart(context);
		FlurryAgent.onEvent(serviceId);
		addBreadcrumb(serviceId);
	}
	
	public static synchronized void onServiceStop(Context context) {
		FlurryAgent.onEndSession(context);
	}
	
	public static synchronized void onEvent(String eventId) {
		FlurryAgent.onEvent(eventId);
		addBreadcrumb(eventId);
	}
	
	public static synchronized void onEvent(String eventId, String... parameters) {
		Map<String, String> parameterMap = new HashMap<String, String>(parameters.length / 2);
		for(int x = 0; x < parameters.length; x += 2) {
			parameterMap.put(parameters[x], parameters[x + 1]);
		}
		FlurryAgent.onEvent(eventId, parameterMap);
		addBreadcrumb(eventId);
	}
	
	public static synchronized void onError(String errorId, Exception exception) {
		FlurryAgent.onError(errorId, exception.toString(), exception.getClass().getName());
		
		/* Only log exceptions which are NOT ApiClientExceptions with an ErrorCode (because they're
		 * all about connection errors which I have no influence on) */
		if(!(exception instanceof ApiClientException && ((ApiClientException)exception).hasCode())) {
			BugSenseHandler.sendException(exception);
		}
		
		if(Studentportal.isDebugBuild()) {
			Log.e("Analytics", "exception logged", exception);
		}
	}
	
	private static String getUserId(Context context) {
		if(mUserId == null) {
			if(Studentportal.getSportalClient().getLoginStatus().isLoggedIn()) {
				mUserId = Studentportal.getSportalClient()
						.getLoginStatus().getUserhash();
			} else {
				SharedPreferences preferences = PreferenceManager
						.getDefaultSharedPreferences(context);
				mUserId = Preferences.getUserId(preferences);
			}
		}
		return mUserId;
	}
	
	private static void addBreadcrumb(String breadcrumb) {
		mActionHistory.add(breadcrumb);
		/* Add this to the bugsense handler since native breadcrumbs are not available 
		 * in the free and open source plans (this emulates the BugSense breadcrumbs feature). */
		BugSenseHandler.addCrashExtraData("history", Utils.getCSV(mActionHistory, " -> "));
	}
	
	private static class BoundedLinkedList<T> extends LinkedList<T> {

		private static final long serialVersionUID = -5203158757928308632L;
		private int size;

		public BoundedLinkedList(int size) {
			super();
			this.size = size;
		}

		@Override
		public boolean add(T object) {
			ensureBound();
			return super.add(object);
		}

		@Override
		public boolean offer(T o) {
			ensureBound();
			return super.offer(o);
		}

		private void ensureBound() {
			if(this.size() == size) {
				/* we already reached the bound so the oldest element needs
				 * to be thrown away in order to insert a new element.
				 */
				remove();
			}
		}
	}
}
