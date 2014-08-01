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

import at.ac.uniklu.mobile.sportal.util.StringUtils;

public class Einstellungen implements Cloneable {

    private String mobilTelNr;
    private String studienFestnetzTelNr;

    private boolean shareMobilTelNr;
    private boolean shareStudienFestnetzTelNr;
    private boolean shareFoto;

    public String getMobilTelNr() {
        return mobilTelNr;
    }

    public void setMobilTelNr(String mobilTelNr) {
        this.mobilTelNr = mobilTelNr;
    }

    public String getStudienFestnetzTelNr() {
        return studienFestnetzTelNr;
    }

    public void setStudienFestnetzTelNr(String studienFestnetzTelNr) {
        this.studienFestnetzTelNr = studienFestnetzTelNr;
    }

    public boolean isShareMobilTelNr() {
        return shareMobilTelNr;
    }

    public void setShareMobilTelNr(boolean shareMobilTelNr) {
        this.shareMobilTelNr = shareMobilTelNr;
    }

    public boolean isShareStudienFestnetzTelNr() {
        return shareStudienFestnetzTelNr;
    }

    public void setShareStudienFestnetzTelNr(boolean shareStudienFestnetzTelNr) {
        this.shareStudienFestnetzTelNr = shareStudienFestnetzTelNr;
    }

    public boolean isShareFoto() {
        return shareFoto;
    }

    public void setShareFoto(boolean shareFoto) {
        this.shareFoto = shareFoto;
    }
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Einstellungen)) {
			return false;
		}
		Einstellungen e = (Einstellungen)o;
		return StringUtils.equals(getMobilTelNr(), e.getMobilTelNr()) 
				&& StringUtils.equals(getStudienFestnetzTelNr(), e.getStudienFestnetzTelNr()) 
				&& isShareFoto() == e.isShareFoto() 
				&& isShareMobilTelNr() == e.isShareMobilTelNr()
				&& isShareStudienFestnetzTelNr() == e.isShareStudienFestnetzTelNr();
	}
	
	public Einstellungen copy() {
		Einstellungen clone = null;
		try {
			clone = (Einstellungen) this.clone();
		} catch (CloneNotSupportedException e) {}
		return clone;
	}
}