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

public class Termin {

    private String titel;
    private String typ;
    private Date datum;
    private int dauer; // Minuten
    private String raum;
    private int lvKey;
    private boolean storniert;

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public Date getDatum() {
        return datum;
    }

    public void setDatum(Date datum) {
        this.datum = datum;
    }

    public int getDauer() {
        return dauer;
    }

    public void setDauer(int dauer) {
        this.dauer = dauer;
    }

    public String getRaum() {
        return raum;
    }

    public void setRaum(String raum) {
        this.raum = raum;
    }

    public int getLvKey() {
        return lvKey;
    }

    public void setLvKey(int lvKey) {
        this.lvKey = lvKey;
    }

    public boolean isStorniert() {
        return storniert;
    }

    public void setStorniert(boolean storniert) {
        this.storniert = storniert;
    }
    
    public String getTitleWithType() {
    	return (typ != null ? typ + " " : "") + titel;
    }
    
    public Date getEndDate() {
    	return new Date(datum.getTime() + (60000 * dauer));
    }
    
    public boolean isNow() {
    	long now = System.currentTimeMillis();
    	return datum.getTime() <= now && getEndDate().getTime() >= now;
    }
    
    public boolean isPast() {
    	long now = System.currentTimeMillis();
    	return getEndDate().getTime() < now;
    }
    
    @Override
    public String toString() {
    	return datum.toString() + " " + getTitleWithType();
    }
}
