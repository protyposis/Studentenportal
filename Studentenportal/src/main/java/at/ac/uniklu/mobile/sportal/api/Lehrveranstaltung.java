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

public class Lehrveranstaltung {

	private int key;
    private Studium studium;
    private String name;
    private String typ;
    private String nummer;
    private int stunden;
    private String status;
    private String moodleUrl;
    private String websiteUrl;
    private String detailsUrl;
    private boolean kreuzellisten;

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }


    public Studium getStudium() {
        return studium;
    }

    public void setStudium(Studium studium) {
        this.studium = studium;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public String getNummer() {
        return nummer;
    }

    public void setNummer(String nummer) {
        this.nummer = nummer;
    }

    public int getStunden() {
        return stunden;
    }

    public void setStunden(int stunden) {
        this.stunden = stunden;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMoodleUrl() {
        return moodleUrl;
    }

    public void setMoodleUrl(String moodleUrl) {
        this.moodleUrl = moodleUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getDetailsUrl() {
        return detailsUrl;
    }

    public void setDetailsUrl(String detailsUrl) {
        this.detailsUrl = detailsUrl;
    }

	public boolean isKreuzellisten() {
		return kreuzellisten;
	}

	public void setKreuzellisten(boolean kreuzellisten) {
		this.kreuzellisten = kreuzellisten;
	}
}