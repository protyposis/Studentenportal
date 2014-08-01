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

package at.ac.uniklu.mobile.sportal.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;

import com.google.gson.reflect.TypeToken;

public class UnikluSportalApiClient extends UnikluApiClient {
	
	private static final String URL_API_AGENDA = "/api/sportal/agenda";
	private static final String URL_API_AGENDA_TERMINE = "/api/sportal/agenda/termine";
	private static final String URL_API_AGENDA_TERMINE_TERMIN = "/api/sportal/agenda/termine/";
	private static final String URL_API_STUDENT = "/api/sportal/student";
	private static final String URL_API_STUDENT_SEMESTERLIST = "/api/sportal/student/semester";
	private static final String URL_API_STUDENT_SEMESTER = "/api/sportal/student/semester/";
	private static final String URL_API_STUDENT_CURRENTSEMESTER = "/api/sportal/student/semester/aktuell";
	private static final String URL_API_STUDENT_STUDIEN = "/api/sportal/student/studien";
	private static final String URL_API_STUDENT_STUDIUM = "/api/sportal/student/studien/";
	private static final String URL_API_STUDENT_EINSTELLUNGEN = "/api/sportal/student/einstellungen";
	private static final String URL_API_LVS = "/api/sportal/lvs";
	private static final String URL_API_LV = "/api/sportal/lvs/";
	private static final String URL_API_LV_KREUZELLISTEN = "/api/sportal/lvs/%d/kreuzellisten";
	private static final String URL_API_LV_KREUZELLISTE = "/api/sportal/lvs/%d/kreuzellisten/%d";
	private static final String URL_API_LV_TEILNEHMER = "/api/sportal/lvs/%d/teilnehmer";
	private static final String URL_API_PRUEFUNGEN = "/api/sportal/pruefungen";
	private static final String URL_API_NOTEN = "/api/sportal/noten";
	private static final String URL_API_NOTIFICATIONS = "/api/sportal/notifications";
	private static final String URL_API_GCM_REGISTER = "/api/sportal/gcm";
	private static final String URL_API_GCM_UNREGISTER = "/api/sportal/gcm/%s";
	
	public UnikluSportalApiClient(String clientVersionInfo, boolean debug) {
		super(clientVersionInfo, debug);
	}
	
	public UnikluSportalApiClient(String clientVersionInfo, boolean debug, Context applicationContext, boolean useCache) {
		super(clientVersionInfo, debug, applicationContext, useCache);
	}
	
	public Agenda getAgenda() throws ApiClientException, ApiServerException {
		return get(URL_API_AGENDA, Agenda.class);
	}
	
	public List<Termin> getTermine(Date von, Date bis, Integer anzahl, Integer[] lvkeys) throws ApiClientException, ApiServerException {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		
		if(von != null) {
			params.add(new BasicNameValuePair("von", von.getTime()+""));
		}
		if(bis != null) {
			params.add(new BasicNameValuePair("bis", bis.getTime()+""));
		}
		if(anzahl != null) {
			params.add(new BasicNameValuePair("anzahl", anzahl.toString()));
		}
		if(lvkeys != null && lvkeys.length > 0) {
			params.add(new BasicNameValuePair("lvs", buildArrayParamValue(lvkeys)));
		}
		
		return get(URL_API_AGENDA_TERMINE, new TypeToken<List<Termin>>(){}.getType(), params.toArray(new NameValuePair[0]));
	}
	
	public List<Termin> getTermine(Integer[] lvkeys) throws ApiClientException, ApiServerException {
		return getTermine(null, null, null, lvkeys);
	}
	
	public List<Termin> getTermineHeute(Integer[] lvkeys) throws ApiClientException, ApiServerException {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date todayBegin = calendar.getTime();
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		Date todayEnd = calendar.getTime();
		return getTermine(todayBegin, todayEnd, null, lvkeys);
	}
	
	public void clearTermine() {
		clearCache(URL_API_AGENDA_TERMINE);
	}
	
	public Termin getTermin(int key) throws ApiClientException, ApiServerException {
		return get(URL_API_AGENDA_TERMINE_TERMIN + key, Termin.class);
	}
	
	public Student getStudent() throws ApiClientException, ApiServerException {
		return get(URL_API_STUDENT, Student.class);
	}
	
	public List<Semester> getSemester() throws ApiClientException, ApiServerException {
		return get(URL_API_STUDENT_SEMESTERLIST, new TypeToken<List<Semester>>(){}.getType());
	}
	
	public Semester getSemester(int key) throws ApiClientException, ApiServerException {
		return get(URL_API_STUDENT_SEMESTER + key, Semester.class);
	}
	
	public Semester getSemesterAktuell() throws ApiClientException, ApiServerException {
		return get(URL_API_STUDENT_CURRENTSEMESTER, Semester.class);
	}
	
	public List<Studium> getStudien() throws ApiClientException, ApiServerException {
		return get(URL_API_STUDENT_STUDIEN, new TypeToken<List<Studium>>(){}.getType());
	}
	
	public Studium getStudium(int key) throws ApiClientException, ApiServerException {
		return get(URL_API_STUDENT_STUDIUM + key, Studium.class);
	}
	
	public Einstellungen getEinstellungen() throws ApiClientException, ApiServerException {
		return get(URL_API_STUDENT_EINSTELLUNGEN, false, Einstellungen.class);
	}
	
	public void postEinstellungen(Einstellungen einstellungen) throws ApiClientException, ApiServerException {
		post(URL_API_STUDENT_EINSTELLUNGEN, einstellungen, true);
	}
	
	public List<Lehrveranstaltung> getLehrveranstaltungen(String semester) throws ApiClientException, ApiServerException {
		NameValuePair[] params = null;
		if(semester != null) {
			params = new BasicNameValuePair[] {
					new BasicNameValuePair("semester", semester)
			};
		}
		return get(URL_API_LVS, new TypeToken<List<Lehrveranstaltung>>(){}.getType(), params);
	}
	
	public List<Lehrveranstaltung> getLehrveranstaltungen() throws ApiClientException, ApiServerException {
		return getLehrveranstaltungen(null);
	}
	
	public void clearLehrveranstaltungen() {
		clearCache(URL_API_LVS);
	}
	
	public Lehrveranstaltung getLehrveranstaltung(int key) throws ApiClientException, ApiServerException {
		return get(URL_API_LV + key, Lehrveranstaltung.class);
	}
	
	public List<Kreuzelliste> getKreuzellisten(int lvkey) throws ApiClientException, ApiServerException {
		return get(String.format(URL_API_LV_KREUZELLISTEN, lvkey), false, new TypeToken<List<Kreuzelliste>>(){}.getType());
	}
	
	public Kreuzelliste getKreuzelliste(int lvkey, int klkey) throws ApiClientException, ApiServerException {
		return get(String.format(URL_API_LV_KREUZELLISTE, lvkey, klkey), false, Kreuzelliste.class);
	}
	
	public void postKreuzelliste(int lvkey, Kreuzelliste kl) throws ApiClientException, ApiServerException {
		post(String.format(URL_API_LV_KREUZELLISTE, lvkey, kl.getKey()), kl, true);
	}
	
	public List<Student> getTeilnehmer(int lvkey) throws ApiClientException, ApiServerException {
		return get(String.format(URL_API_LV_TEILNEHMER, lvkey), new TypeToken<List<Student>>(){}.getType());
	}
	
	public List<Pruefung> getPruefungen(String semester) throws ApiClientException, ApiServerException {
		NameValuePair[] params = null;
		if(semester != null) {
			params = new BasicNameValuePair[] {
					new BasicNameValuePair("semester", semester)
			};
		}
		return get(URL_API_PRUEFUNGEN, new TypeToken<List<Pruefung>>(){}.getType(), params);
	}
	
	public List<Pruefung> getPruefungen() throws ApiClientException, ApiServerException {
		return getPruefungen(null);
	}
	
	public void clearPruefungen() {
		clearCache(URL_API_PRUEFUNGEN);
	}
	
	public List<Note> getNoten() throws ApiClientException, ApiServerException {
		return get(URL_API_NOTEN, new TypeToken<List<Note>>(){}.getType());
	}
	
	public void clearNoten() {
		clearCache(URL_API_NOTEN);
	}
	
	public List<Notification> getNotifications(Date von) throws ApiClientException, ApiServerException {
		return get(URL_API_NOTIFICATIONS, false,
				new TypeToken<List<Notification>>(){}.getType(), 
				new BasicNameValuePair("von", von.getTime()+""));
	}
	
	public void postGCMRegistrationId(String regId) throws ApiClientException, ApiServerException {
		post(URL_API_GCM_REGISTER, regId, true);
	}
	
	public void deleteGCMRegistrationId(String regId) throws ApiClientException, ApiServerException {
		delete(String.format(URL_API_GCM_UNREGISTER, regId), true);
	}
}
