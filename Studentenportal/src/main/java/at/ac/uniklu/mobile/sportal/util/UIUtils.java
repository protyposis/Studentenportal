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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import at.ac.uniklu.mobile.sportal.LoginActivity;
import at.ac.uniklu.mobile.sportal.R;
import at.ac.uniklu.mobile.sportal.api.ApiClientException;
import at.ac.uniklu.mobile.sportal.api.ApiServerException;
import at.ac.uniklu.mobile.sportal.ui.AlertDialogDefinition;
import at.ac.uniklu.mobile.sportal.ui.Refreshable;

public class UIUtils {

	/**
	 * Shows a simple alert dialog with a text message and an OK button.
	 */
	public static AlertDialog showSimpleAlertDialog(Context context, Integer iconId, CharSequence mTitle, CharSequence mMessage) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context)
				.setMessage(mMessage)
				.setNeutralButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
		if(iconId != null) {
			builder.setIcon(iconId);
		}
		if(mTitle != null) {
			builder.setTitle(mTitle);
		}
		AlertDialog d = builder.create();
		
		// avoid android.view.WindowManager$BadTokenException by checking isFinishing()
		if(context instanceof Activity && !((Activity)context).isFinishing()) {
			d.show();
		}
		
		return d;
	}
	
	public static AlertDialog showSimpleAlertDialog(Context context, CharSequence message) {
		return showSimpleAlertDialog(context, null, null, message);
	}
	
	public static AlertDialog showSimpleAlertDialog(Context context, Integer iconId, Integer titleId, Integer messageId) {
		return showSimpleAlertDialog(context, iconId,
				titleId != null ? context.getString(titleId) : null, 
				context.getString(messageId));
	}
	
	public static AlertDialog showSimpleAlertDialog(Context context, Integer messageId) {
		return showSimpleAlertDialog(context, null, null, messageId);
	}
	
	/**
	 * Shows a simple error dialog with a text message and an OK button and
	 * closes the activity on click.
	 */
	public static AlertDialog showSimpleErrorDialogAndClose(final Activity activity, CharSequence message) {
		AlertDialog d = new AlertDialog.Builder(activity)
				.setMessage(message)
				.setNeutralButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								activity.finish();
							}
						}).create();
		d.show();
		return d;
	}
	
	public static void reloadActivity(Activity activity) {
		activity.startActivity(new Intent(activity, activity.getClass()));
		activity.finish();
	}
	
	public static AlertDialogDefinition buildExceptionDialog(Context context, Exception e) {
		Integer errorTitle = null;
		Integer errorText = null;
		if(e instanceof ApiServerException) {
			at.ac.uniklu.mobile.sportal.api.Error error = ((ApiServerException)e).getError();
			if(error.getCode() == 404) {
				errorText = R.string.error_not_found;
			} else if(error.getCode() == 401) {
				errorText = R.string.error_not_authorized;
			} else if(error.getCode() == 500) {
				errorText = R.string.error_server_internal;
			}
		}
		else if(e instanceof ApiClientException) {
			ApiClientException cE = (ApiClientException)e;
			if(cE.hasCode()) {
				if(cE.getCode() == ApiClientException.Code.UNKNOWNHOST) {
					errorText = R.string.error_unknown_host;
				} else if(cE.getCode() == ApiClientException.Code.TIMEOUT) {
					errorText = R.string.error_timeout;
				} else if(cE.getCode() == ApiClientException.Code.READING_RESPONSE) {
					errorText = R.string.error_reading_response;
				} else if(cE.getCode() == ApiClientException.Code.LOGIN_FAILED) {
					errorTitle = R.string.error_login_failed;
					errorText = R.string.error_login_failed_msg;
				} else if(cE.getCode() == ApiClientException.Code.LOGIN_FAILED_STAFF) {
					errorTitle = R.string.error_login_failed;
					errorText = R.string.error_login_failed_staff_msg;
				} else if(cE.getCode() == ApiClientException.Code.SSL) {
					errorText = R.string.error_ssl;
				}
			}
		}
		
		if(errorText == null) {
			return new AlertDialogDefinition("Error", e.getMessage());
		}
		return new AlertDialogDefinition(
				errorTitle != null ? context.getString(errorTitle) : "Error", 
				context.getString(errorText));
	}
	
	public static boolean processExceptionForLogin(Exception e, Activity a, int requestCode) {
		if(Utils.isMissingAuthenticationException(e)) {
			a.startActivityForResult(new Intent(a, LoginActivity.class)
					.putExtra(LoginActivity.MODE, LoginActivity.MODE_RELOGIN), requestCode);
			return true;
		}
		return false;
	}
	
	public static void processActivityRefreshException(Exception e, Activity a) {
		if(!UIUtils.processExceptionForLogin(e, a, Refreshable.REQUEST_REFRESH)) {
			e.printStackTrace();
			UIUtils.buildExceptionDialog(a, e).show(a);
			Analytics.onError(Analytics.ERROR_GENERIC, e);
		}
	}
	
	public static boolean executeActivityRefresh(int requestCode, Refreshable refreshable) {
		if(requestCode == Refreshable.REQUEST_REFRESH) {
			refreshable.refresh();
			return true;
		}
		return false;
	}
	
	public static String getTermName(String termCode, Context context) {
		int year = Integer.parseInt(termCode.substring(0, 2));
		if(year > 50) {
			year += 1900;
		} else {
			year += 2000;
		}
		if(termCode.charAt(2) == 'W') {
			return context.getString(R.string.winterterm, year, year + 1);
		} else {
			return context.getString(R.string.summerterm, year);
		}
	}
	
	/**
	 * source: http://code.google.com/p/iosched/source/browse/android/src/com/google/android/apps/iosched/util/UIUtils.java
	 */
	public static boolean isTablet(Context context) {
	    return (context.getResources().getConfiguration().screenLayout
	            & Configuration.SCREENLAYOUT_SIZE_MASK)
	            >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}
}
