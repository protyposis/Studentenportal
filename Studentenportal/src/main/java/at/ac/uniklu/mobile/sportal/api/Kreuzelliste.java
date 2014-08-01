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
import java.util.List;

public class Kreuzelliste {

    private int key;
    private String name;
    private Date ablaufdatum;
    private Date besprechungsdatum;
    private boolean offen;
    private Integer anzahl;
    private Integer kreuzel;
    private List<KreuzellisteAufgabe> aufgaben;

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getAblaufdatum() {
        return ablaufdatum;
    }

    public void setAblaufdatum(Date ablaufdatum) {
        this.ablaufdatum = ablaufdatum;
    }

    public Date getBesprechungsdatum() {
        return besprechungsdatum;
    }

    public void setBesprechungsdatum(Date besprechungsdatum) {
        this.besprechungsdatum = besprechungsdatum;
    }

    public boolean isOffen() {
        return offen;
    }

    public void setOffen(boolean offen) {
        this.offen = offen;
    }

    public Integer getAnzahl() {
		return anzahl;
	}

	public void setAnzahl(Integer anzahl) {
		this.anzahl = anzahl;
	}

	public Integer getKreuzel() {
		return kreuzel;
	}

	public void setKreuzel(Integer kreuzel) {
		this.kreuzel = kreuzel;
	}

	public List<KreuzellisteAufgabe> getAufgaben() {
        return aufgaben;
    }

    public void setAufgaben(List<KreuzellisteAufgabe> aufgaben) {
        this.aufgaben = aufgaben;
    }
}
