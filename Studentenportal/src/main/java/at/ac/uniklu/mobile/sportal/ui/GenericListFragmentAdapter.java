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

import java.util.List;

import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Generic base adapter that takes care of recycling views. Extension classes only need to update the
 * views ({@link #updateView(int, View)}) and do not need to take care of the view recycling process.
 */
public abstract class GenericListFragmentAdapter<T> extends BaseAdapter {
	
	private ListFragment mContext;
	private int mViewResourceId;
	
	protected List<T> mList;
	
	public GenericListFragmentAdapter(ListFragment context, List<T> list, int viewResourceId) {
		this.mContext = context;
		this.mList = list;
		this.mViewResourceId = viewResourceId;
	}

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView;

        if (convertView == null) {
            itemView = (View)mContext.getActivity().getLayoutInflater().inflate(mViewResourceId, parent, false);
        } else {
        	// recycle existing view
            itemView = (View)convertView;
        }
        
        updateView(position, itemView);

        return itemView;
    }
    
    protected abstract void updateView(int position, View itemView);
}
