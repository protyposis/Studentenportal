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

import android.graphics.Bitmap;
import at.ac.uniklu.mobile.sportal.api.Semester;
import at.ac.uniklu.mobile.sportal.api.Student;
import at.ac.uniklu.mobile.sportal.api.Studium;

public class ProfileModel {

	private Student student;
	private List<Studium> studies;
	private Semester currentSemester;
	private Bitmap studentPortrait;
	
	public Student getStudent() {
		return student;
	}
	
	public void setStudent(Student student) {
		this.student = student;
	}
	
	public List<Studium> getStudies() {
		return studies;
	}

	public void setStudies(List<Studium> studies) {
		this.studies = studies;
	}

	public Semester getCurrentSemester() {
		return currentSemester;
	}

	public void setCurrentSemester(Semester currentSemester) {
		this.currentSemester = currentSemester;
	}

	public Bitmap getStudentPortrait() {
		return studentPortrait;
	}
	
	public void setStudentPortrait(Bitmap studentPortrait) {
		this.studentPortrait = studentPortrait;
	}
}
