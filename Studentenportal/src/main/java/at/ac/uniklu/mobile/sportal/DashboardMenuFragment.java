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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DashboardMenuFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.dashboard_menu, container, false);
		
		// create dashboard click handlers
        Button coursesButton = (Button)v.findViewById(R.id.courses);
        coursesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/* start the activity from the fragment container activity so that this 
				 * activity also receives the result */
				getActivity().startActivityForResult(new Intent(getActivity(), CourseListActivity.class), 0);
			}
        });
        Button examsButton = (Button)v.findViewById(R.id.exams);
        examsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ExamListActivity.class));
			}
        });
        Button gradesButton = (Button)v.findViewById(R.id.grades);
        gradesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), GradeListActivity.class));
			}
        });
        Button calendarButton = (Button)v.findViewById(R.id.calendar);
        calendarButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), CalendarActivity.class));
			}
        });
        Button profileButton = (Button)v.findViewById(R.id.profile);
        profileButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ProfileActivity.class));
			}
        });
        Button busButton = (Button)v.findViewById(R.id.bus);
        busButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), BusDeparturesActivity.class));
			}
        });
        
        return v;
	}

}
