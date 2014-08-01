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

import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.widget.DatePicker;

/**
 * Fixes issues introduced in Jelly Bean where the callback always gets called on 
 * custom button actions or cancels by pressing the back button, and even twice
 * on a successful date selection.
 * 
 * http://stackoverflow.com/questions/11444238/jelly-bean-datepickerdialog-is-there-a-way-to-cancel
 * https://code.google.com/p/android/issues/detail?id=34833
 */
public class FixedDatePickerDialog extends DatePickerDialog {

	public FixedDatePickerDialog(Context context, int theme,
			OnDateSetListener callBack, int year, int monthOfYear,
			int dayOfMonth) {
		super(context, theme, getConstructorCallback(callBack), year, monthOfYear, dayOfMonth);
		init(callBack);
	}

	public FixedDatePickerDialog(Context context, OnDateSetListener callBack,
			int year, int monthOfYear, int dayOfMonth) {
		super(context, getConstructorCallback(callBack), year, monthOfYear, dayOfMonth);
		init(callBack);
	}
	
	private void init(final OnDateSetListener callBack) {
		if (isJellyBeanAndAbove()) {
			/* Adds buttons to the picker and emulates Android behavior
			 * below Jelly Bean on Jelly Bean devices.
			 * On pre Jelly Bean devices, this block doesn't get executed 
			 * and thus the default android implementation is used.
			 */
			
            setButton(DialogInterface.BUTTON_POSITIVE, 
            		getContext().getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
            	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
				@Override
                public void onClick(DialogInterface dialog, int which) {
                    DatePicker dp = getDatePicker();
                    callBack.onDateSet(dp, dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
                }
            });
            
			setButton(DialogInterface.BUTTON_NEGATIVE,
					getContext().getString(android.R.string.cancel),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {}
			});
        }
	}
	
	private static OnDateSetListener getConstructorCallback(OnDateSetListener callBack) {
		if(isJellyBeanAndAbove()) {
			/* On Jelly Bean and above, hand over a dummy listener. */
			return new OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear,
						int dayOfMonth) {
					/* Do nothing here. This listener just gets passed to the superclass
					 * to avoid a NullPointerException if the callback null-check gets 
					 * removed in a future Android version (> 4.2.2).
					 */
				}
			};
		} else {
			/* On pre Jelly Bean devices, hand over the listener passed to the constructor. */
			return callBack;
		}
	}

	private static boolean isJellyBeanAndAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }
}
