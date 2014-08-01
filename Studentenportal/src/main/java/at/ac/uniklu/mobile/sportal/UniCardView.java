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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * A view that keeps its width relative to it's height according to the 
 * standardized ID card size aspect ratio.
 */
public class UniCardView extends RelativeLayout {
	
	// standardized credit card aspect ratio: http://de.wikipedia.org/wiki/ISO/IEC_7810
	private static final float RATIO = 1.586f;

	public UniCardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public UniCardView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public UniCardView(Context context) {
		super(context);
	}

	@Override
	public void addView(View child) {
		checkViewCount();
		super.addView(child);
	}

	@Override
	public void addView(View child, int index) {
		checkViewCount();
		super.addView(child, index);
	}

	@Override
	public void addView(View child, int width, int height) {
		checkViewCount();
		super.addView(child, width, height);
	}

	@Override
	public void addView(View child, ViewGroup.LayoutParams params) {
		checkViewCount();
		super.addView(child, params);
	}

	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		checkViewCount();
		super.addView(child, index, params);
	}
	
	private void checkViewCount() {
		if (getChildCount() > 0) {
            throw new IllegalStateException("UniCardView can host only one direct child");
        }
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		// determine this view's size
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);   
	    
		// get the aspect ratio of the view
	    float ratio = getMeasuredWidth() / (float)getMeasuredHeight();
	    
	    /* If the ratio is too small, leave it like it is - this is needed for devices 
	     * with small screens; otherwise, limit the width of the view to the credit
	     * card aspect ratio.
	     */
	    if(ratio > RATIO) {
	    	widthMeasureSpec = MeasureSpec.makeMeasureSpec((int)(getMeasuredHeight() * RATIO), MeasureSpec.AT_MOST);
	    	super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	    }
    }

}
