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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import at.ac.uniklu.mobile.sportal.Studentportal;
import at.ac.uniklu.mobile.sportal.api.Einstellungen;
import at.ac.uniklu.mobile.sportal.api.Kreuzelliste;
import at.ac.uniklu.mobile.sportal.api.Lehrveranstaltung;
import at.ac.uniklu.mobile.sportal.api.Semester;
import at.ac.uniklu.mobile.sportal.api.Student;
import at.ac.uniklu.mobile.sportal.api.Studium;
import at.ac.uniklu.mobile.sportal.api.Termin;
import at.ac.uniklu.mobile.sportal.util.Utils;

public class ModelService {
	
	private static final int CALENDAR_DATES_TO_LOAD = 50;
	
	public static CalendarModel getDashboardModel(Context context) throws Exception {
		CalendarModel model = new CalendarModel();
		
//		Calendar c = Calendar.getInstance();
//		c.set(Calendar.YEAR, 2012);
//		c.set(Calendar.MONTH, Calendar.OCTOBER);
//		c.set(Calendar.DAY_OF_MONTH, 1);

		model.setDates(Studentportal.getSportalClient()
				.getTermine(null, null, 10, getCourseBlacklist(context)));
		model.setMoreDatesToLoad(false);
		
		return model;
	}
	
	public static CourseModel getCourseModel(CourseModel cm, Semester semester) throws Exception {
		CourseModel model = new CourseModel();
		List<Semester> semesters;
		
		if(cm == null) {
			semesters = Studentportal.getSportalClient().getSemester();
			Collections.reverse(semesters);
			removeFutureSemesters(semesters);
		} else {
			semesters = cm.getSemester();
		}
		
		if(semester == null) {
			semester = semesters.get(0);
		}
		
		List<Lehrveranstaltung> courses = Studentportal.getSportalClient()
				.getLehrveranstaltungen(semester.getKey());
		
		model.setSemester(semesters);
		model.setCourses(courses);
		
		return model;
	}
	
	private static void removeFutureSemesters(List<Semester> semesters) {
		/* if the list contains at least 2 semesters, check if the most recent 
		 * semester lies in the future, and if it does, remove it if the semester
		 * before is the current one. If the semester before isn't the current one,
		 * it should be kept since the current time lies in holidays between 
		 * two semesters.
		 * TODO prevent delivery of future semesters on the server if in active semester
		 */
		if(semesters.size() > 1 && semesters.get(0).isInFuture() && semesters.get(1).isNow()) {
			semesters.remove(0);
		}
	}
	
	public static CourseModel getCourseModel() throws Exception {
		return getCourseModel(null, null);
	}
	
	public static KreuzellistenModel getKreuzellistenModel(int lvkey) throws Exception {
		KreuzellistenModel model = new KreuzellistenModel();
		List<Kreuzelliste> kreuzellisten = Studentportal.getSportalClient().getKreuzellisten(lvkey);
		model.setKreuzellisten(kreuzellisten);
		return model;
	}
	
	public static KreuzellisteModel getKreuzellisteModel(int lvkey, int klkey) throws Exception {
		KreuzellisteModel model = new KreuzellisteModel();
		model.setKreuzelliste(Studentportal.getSportalClient().getKreuzelliste(lvkey, klkey));
		return model;
	}
	
	public static TeilnehmerlisteModel getTeilnehmerlisteModel(int lvkey) throws Exception {
		TeilnehmerlisteModel model = new TeilnehmerlisteModel();
		model.setTeilnehmer(Studentportal.getSportalClient().getTeilnehmer(lvkey));
		model.setEinstellungen(Studentportal.getSportalClient().getEinstellungen());
		return model;
	}
	
	public static CalendarModel getCalendarModel(Context context, CalendarModel model) throws Exception {
		boolean more = false;
		if(model == null) {
			model = new CalendarModel();
		} else {
			more = true;
		}
		
		// get dates from network
		List<Termin> datesList = null;
		Date queryDate = null;
		Termin lastDate = null;
		
		if(!more) {
			// query all dates of current day
			Calendar c = Calendar.getInstance();
//			c.set(Calendar.YEAR, 2012);
//			c.set(Calendar.MONTH, Calendar.OCTOBER);
//			c.set(Calendar.DAY_OF_MONTH, 1);
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			queryDate = c.getTime();
		} else {
			lastDate = model.getDates().get(model.getDates().size() - 1);
			queryDate = lastDate.getDatum();
		}
		
		datesList = Studentportal.getSportalClient().getTermine(
				queryDate, null, CALENDAR_DATES_TO_LOAD, getCourseBlacklist(context));

		/* Only try to load more dates if the server responded with the requested
		 * number of dates, because we can assume in this case that there are more
		 * dates existing. If the server responds with less, the end of the list
		 * has been reached.
		 */
		model.setMoreDatesToLoad(datesList.size() >= CALENDAR_DATES_TO_LOAD);
		
		if(!more) {
			model.setDates(datesList);
		} else if(!datesList.isEmpty()) {
			// detect overlap
			int cutoff = 0;
			for(cutoff = 0; cutoff < datesList.size(); cutoff++) {
				if(datesList.get(cutoff).getDatum().equals(queryDate) &&
						datesList.get(cutoff).getLvKey() == lastDate.getLvKey()) {
					break;
				}
			}
			/* there's always at least one overlap since the query date is
			 * the date of the last termin of the previous query (if the list isn't empty) */
			datesList = datesList.subList(cutoff + 1, datesList.size());
			model.getDates().addAll(datesList);
		}

		return model;
	}
	
	public static ExamModel getExamModel(ExamModel em, Semester semester) throws Exception {
		ExamModel model = new ExamModel();
		
		if(em != null) {
			model.setSemester(em.getSemester());
		} else {
			List<Semester> semesters = Studentportal.getSportalClient().getSemester();
			Collections.reverse(semesters);
			removeFutureSemesters(semesters);
			model.setSemester(semesters);
		}
		
		if(semester == null) {
			semester = model.getSemester().get(0);
		}
		
		model.setExams(Studentportal.getSportalClient().getPruefungen(semester.getKey()));
		
		return model;
	}
	
	public static ExamModel getExamModel() throws Exception {
		return getExamModel(null, null);
	}
	
	public static GradeModel getGradeModel(Context context) throws Exception {
		GradeModel model = new GradeModel();
		
		model.setGrades(Studentportal.getSportalClient().getNoten());
		
		return model;
	}
	
	public static ProfileModel getProfileModel(Context context) throws Exception {
		ProfileModel model = new ProfileModel();
		
		Student student = Studentportal.getSportalClient().getStudent();
		List<Studium> studies = Studentportal.getSportalClient().getStudien();
		Semester semester = Studentportal.getSportalClient().getSemesterAktuell();
		Bitmap studentPortrait = Utils.downloadBitmapWithCache(context, 
				Studentportal.getSportalClient().getBaseUrl() + 
				"/api/sportal/student/foto", 
				"portrait" + student.getMnr() + ".jpg");
		
		model.setStudent(student);
		model.setStudies(studies);
		model.setCurrentSemester(semester);
		model.setStudentPortrait(studentPortrait);
		
		return model;
	}
	
	public static EinstellungenModel getEinstellungenModel() throws Exception {
		EinstellungenModel model = new EinstellungenModel();
		Einstellungen e = Studentportal.getSportalClient().getEinstellungen();
		model.setEinstellungen(e);
		model.setServerEinstellungen(e.copy());
		return model;
	}
	
	public static void saveEinstellungenModel(EinstellungenModel model) throws Exception {
		Studentportal.getSportalClient().postEinstellungen(model.getEinstellungen());
	}
	
	private static Integer[] getCourseBlacklist(Context context) {
		List<Integer> ignoredCourses = Studentportal.getStudentPortalDB().courseBlacklist();
		
		Integer[] blacklist = new Integer[ignoredCourses.size()];
		for(int i = 0; i < blacklist.length; i++) {
			blacklist[i] = -ignoredCourses.get(i);
		}
		
		return blacklist;
	}
}
