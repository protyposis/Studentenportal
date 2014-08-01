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

package at.ac.uniklu.mobile.sportal.model;

import java.util.List;

import at.ac.uniklu.mobile.sportal.api.Lehrveranstaltung;
import at.ac.uniklu.mobile.sportal.api.Semester;

public class CourseModel {
	
	private List<Semester> semester;
	private List<Lehrveranstaltung> courses;

	public List<Semester> getSemester() {
		return semester;
	}

	public void setSemester(List<Semester> semester) {
		this.semester = semester;
	}

	public List<Lehrveranstaltung> getCourses() {
		return courses;
	}

	public void setCourses(List<Lehrveranstaltung> courses) {
		this.courses = courses;
	}
	
	public Lehrveranstaltung getCourse(int index) {
		return courses.get(index);
	}
}
