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

public class Pruefung {

    public enum Modus {
        SCHRIFTLICH,
        MUENDLICH,
        MUENDLICH_SCHRIFTLICH
    }

    public enum Unterlagen {
        OHNE,
        MIT,
        TEILWEISE
    }

    public enum Status {
        NICHT_ANGEMELDET,
        ANGEMELDET,
        ANGETRETEN,
        BENOTET,
        FREIGEGEBEN,
        NICHT_ANGETRETEN,
        ABGEWIESEN
    }

    private int key;
    private Lehrveranstaltung lv;
    private Date datum;
    private String von;
    private String bis;
    private String raum;
    private Modus modus;
    private Unterlagen unterlagen;
    private Status status;
    private String bemerkung;
    private Double punkte;
    private String detailsUrl;

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Lehrveranstaltung getLv() {
        return lv;
    }

    public void setLv(Lehrveranstaltung lv) {
        this.lv = lv;
    }

    public Date getDatum() {
        return datum;
    }

    public void setDatum(Date datum) {
        this.datum = datum;
    }

    public String getVon() {
        return von;
    }

    public void setVon(String von) {
        this.von = von;
    }

    public String getBis() {
        return bis;
    }

    public void setBis(String bis) {
        this.bis = bis;
    }

    public String getRaum() {
        return raum;
    }

    public void setRaum(String raum) {
        this.raum = raum;
    }

    public Modus getModus() {
        return modus;
    }

    public void setModus(Modus modus) {
        this.modus = modus;
    }

    public Unterlagen getUnterlagen() {
        return unterlagen;
    }

    public void setUnterlagen(Unterlagen unterlagen) {
        this.unterlagen = unterlagen;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getBemerkung() {
        return bemerkung;
    }

    public void setBemerkung(String bemerkung) {
        this.bemerkung = bemerkung;
    }

    public Double getPunkte() {
        return punkte;
    }

    public void setPunkte(Double punkte) {
        this.punkte = punkte;
    }

	public String getDetailsUrl() {
		return detailsUrl;
	}

	public void setDetailsUrl(String detailsUrl) {
		this.detailsUrl = detailsUrl;
	}
}
