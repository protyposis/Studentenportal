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

import java.lang.reflect.Field;

import android.annotation.TargetApi;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.util.Log;
import android.widget.TimePicker;

/**
 * Fixes issues introduced in Jelly Bean, see {@link FixedDatePickerDialog} for a
 * detailed description.
 */
public class FixedTimePickerDialog extends TimePickerDialog {
	
	private TimePicker mTimePicker;

	public FixedTimePickerDialog(Context context, int theme,
			OnTimeSetListener callBack, int hourOfDay, int minute,
			boolean is24HourView) {
		super(context, theme, getConstructorCallback(callBack), hourOfDay, minute, is24HourView);
		init(callBack);
	}

	public FixedTimePickerDialog(Context context, OnTimeSetListener callBack,
			int hourOfDay, int minute, boolean is24HourView) {
		super(context, getConstructorCallback(callBack), hourOfDay, minute, is24HourView);
		init(callBack);
	}
	
	private void init(final OnTimeSetListener callBack) {
		if (isJellyBeanAndAbove()) {
            setButton(DialogInterface.BUTTON_POSITIVE, 
            		getContext().getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
            	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
				@Override
                public void onClick(DialogInterface dialog, int which) {
            		TimePicker tp = getTimePicker();
                    callBack.onTimeSet(tp, tp.getCurrentHour(), tp.getCurrentMinute());
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
	
	private TimePicker getTimePicker() {
		if(mTimePicker == null) {
			Field f = null;
			
			/* Try to get object from mTimePicker field. If unsuccessful, check all
			 * fields for the TimePicker type. */
			try {
				f = TimePickerDialog.class.getDeclaredField("mTimePicker");
			} catch (NoSuchFieldException e) {
				for(Field f2 : TimePickerDialog.class.getDeclaredFields()) {
					if(f2.getType().isAssignableFrom(TimePicker.class)) {
						f = f2;
						break;
					}
				}
			}
			
			if(f != null) {
				f.setAccessible(true);
				try {
					mTimePicker = (TimePicker)f.get(this);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			} else {
				Log.e("FixedTimePickerDialog", "cannot find TimePicker object in superclass");
			}
		}
		
		return mTimePicker;
	}
	
	private static OnTimeSetListener getConstructorCallback(OnTimeSetListener callBack) {
		if(isJellyBeanAndAbove()) {
			return new OnTimeSetListener() {
				@Override
				public void onTimeSet(TimePicker view, int hourOfDay, int minute) {}
			};
		} else {
			return callBack;
		}
	}

	private static boolean isJellyBeanAndAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

}
