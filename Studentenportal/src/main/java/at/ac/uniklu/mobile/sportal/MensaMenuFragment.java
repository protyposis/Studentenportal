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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.mensa.MenuCategory;
import at.ac.uniklu.mobile.sportal.mensa.MenuItem;
import at.ac.uniklu.mobile.sportal.util.StringUtils;

public class MensaMenuFragment extends Fragment {
	
	public static final String ARGUMENT_INDEX = "index";
	
	private int mIndex;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIndex = getArguments().getInt(ARGUMENT_INDEX);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.mensa_menu_fragment, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		MensaActivity ma = (MensaActivity)getActivity();
		
		if(ma.isDataAvailable()) {
			final MenuCategory mc = ma.getMenuCategory(mIndex);
			ViewGroup container = (ViewGroup)getView().findViewById(R.id.menu_items_container);
			LayoutInflater inflater = getLayoutInflater(getArguments());
			
			container.findViewById(R.id.mensa_website).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mc.link)));
				}
			});
			
			for(MenuItem mi : mc.menuItems) {
				View menuItem = inflater.inflate(R.layout.mensa_menu_item, container, false);
				
				if(StringUtils.isEmpty(mi.title)) {
					menuItem.findViewById(R.id.text_title).setVisibility(View.GONE);
				} else {
					((TextView)menuItem.findViewById(R.id.text_title)).setText(mi.title);
				}
				
				((TextView)menuItem.findViewById(R.id.text_description)).setText(mi.description);
				
				if(!StringUtils.isEmpty(mi.description)) {
					container.addView(menuItem, container.getChildCount() - 1);
				}
			}
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}

}
