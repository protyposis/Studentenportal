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

import java.util.Date;

public class Note {
	
	public enum Typ {
		EP,
		FP
	}

    private int key;
    private Typ typ;
    private int pruefungKey;
    private int epKey;
    private Date datum;
    private Lehrveranstaltung lv;
    private String note;
    private float ects;
    private int stunden;
    private boolean uebernommen;
    private String detailsUrl;

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Typ getTyp() {
		return typ;
	}

	public void setTyp(Typ typ) {
		this.typ = typ;
	}

	public int getPruefungKey() {
        return pruefungKey;
    }

    public void setPruefungKey(int pruefungKey) {
        this.pruefungKey = pruefungKey;
    }

    public int getEpKey() {
        return epKey;
    }

    public void setEpKey(int epKey) {
        this.epKey = epKey;
    }

    public Date getDatum() {
        return datum;
    }

    public void setDatum(Date datum) {
        this.datum = datum;
    }

    public Lehrveranstaltung getLv() {
        return lv;
    }

    public void setLv(Lehrveranstaltung lv) {
        this.lv = lv;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public float getEcts() {
        return ects;
    }

    public void setEcts(float ects) {
        this.ects = ects;
    }

    public int getStunden() {
        return stunden;
    }

    public void setStunden(int stunden) {
        this.stunden = stunden;
    }
    
	public boolean isUebernommen() {
		return uebernommen;
	}

	public void setUebernommen(boolean uebernommen) {
		this.uebernommen = uebernommen;
	}

	public String getDetailsUrl() {
		return detailsUrl;
	}

	public void setDetailsUrl(String detailsUrl) {
		this.detailsUrl = detailsUrl;
	}
}