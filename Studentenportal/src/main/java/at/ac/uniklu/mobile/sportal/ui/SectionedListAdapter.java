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

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class SectionedListAdapter<T, H> extends BaseAdapter {
	
	public static final int VIEW_TYPE_HEADER = 0;
	public static final int VIEW_TYPE_ITEM = 1;
	
	public interface HeaderDataExtractor<T, H> {
		H extract(T itemData);
	}
	
	private static class TagWrapper {
		public int viewType;
		public Object wrappedTag;
	}
	
	private static class HeaderMapping {
		public boolean header;
		public int index;
	}
	
	private int mHeaderViewResourceId;
	private int mViewResourceId;
	private Context mContext;
	private List<T> mList;
	private HeaderDataExtractor<T, H> mHeaderDataExtractor;
	private SparseArray<HeaderMapping> mIndexMapping;
	
	public SectionedListAdapter(Context context, List<T> list, HeaderDataExtractor<T, H> headerDataExtractor, int headerViewResourceId, int viewResourceId) {
		mContext = context;
		mList = list;
		mHeaderDataExtractor = headerDataExtractor;
		mHeaderViewResourceId = headerViewResourceId;
		mViewResourceId = viewResourceId;
		init();
	}
	
	private void init() {
		H prev = null;
		H curr = null;
		int mappedIndex = 0;
		HeaderMapping mapping = null;
		mIndexMapping = new SparseArray<HeaderMapping>();
		for(int i = 0; i < mList.size(); i++) {
			curr = mHeaderDataExtractor.extract(mList.get(i));
			if(prev == null || !prev.equals(curr)) {
				// header!!!
				mapping = new HeaderMapping();
				mapping.header = true;
				mapping.index = i;
				mIndexMapping.put(mappedIndex++, mapping);
				prev = curr;
			}
			
			mapping = new HeaderMapping();
			mapping.header = false;
			mapping.index = i;
			mIndexMapping.put(mappedIndex++, mapping);
		}
	}
	
	@Override
	public int getViewTypeCount() {
		return 2; // HEADER & ITEM
	}
	
	@Override
	public int getItemViewType(int position) {
		return mIndexMapping.get(position).header ? 
				VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
	}

	@Override
	public int getCount() {
		return mIndexMapping.size();
	}

	@Override
	public Object getItem(int position) {
		return mIndexMapping.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}
	
	@Override
	public boolean isEnabled(int position) {
		return !mIndexMapping.get(position).header;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		boolean isHeader = isHeader(position);
		boolean reuse = false;
		View v = null;
		TagWrapper w = null;
		
		if(convertView != null) {
			v = convertView;
			w = (TagWrapper)v.getTag();
			v.setTag(w.wrappedTag);
			if((w.viewType == VIEW_TYPE_HEADER && isHeader) 
					|| (w.viewType == VIEW_TYPE_ITEM && !isHeader)) {
				reuse = true;
			}
		} else {
			w = new TagWrapper();
		}
		
		T item = mList.get(mIndexMapping.get(position).index);
		if(isHeader) {
			if(!reuse) {
				v = LayoutInflater.from(mContext).inflate(mHeaderViewResourceId, parent, false);
				w.viewType = VIEW_TYPE_HEADER;
			}
			updateHeaderView(v, mHeaderDataExtractor.extract(item));
		} else {
			if(!reuse) {
				v = LayoutInflater.from(mContext).inflate(mViewResourceId, parent, false);
				w.viewType = VIEW_TYPE_ITEM;
			}
			updateItemView(v, item);
		}
		
		w.wrappedTag = v.getTag();
		v.setTag(w);
		
		return v;
	}
	
	public abstract void updateHeaderView(View headerView, H headerData);
	
	public abstract void updateItemView(View view, T itemData);
	
	@Override
	public void notifyDataSetChanged() {
		init();
		super.notifyDataSetChanged();
	}
	
	private boolean isHeader(int position) {
		return mIndexMapping.get(position).header;
	}

}
