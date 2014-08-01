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

public class Student {

	private int key;
    private String mnr;
    private String vorname;
    private String nachname;
    private String username;
    private String nickname;
    private String email;
    private String titelVorgestellt;
    private String titelNachgestellt;
    private Date geburtsdatum;
    private boolean hasCard;
    private Date cardGueltigBis;
    private boolean profilePicture;
    private String telNr;
    private String mobileTelNr;
    
    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getMnr() {
        return mnr;
    }

    public void setMnr(String mnr) {
        this.mnr = mnr;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public void setNachname(String nachname) {
        this.nachname = nachname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTitelVorgestellt() {
        return titelVorgestellt;
    }

    public void setTitelVorgestellt(String titelVorgestellt) {
        this.titelVorgestellt = titelVorgestellt;
    }

    public String getTitelNachgestellt() {
        return titelNachgestellt;
    }

    public void setTitelNachgestellt(String titelNachgestellt) {
        this.titelNachgestellt = titelNachgestellt;
    }

	public Date getGeburtsdatum() {
		return geburtsdatum;
	}

	public void setGeburtsdatum(Date geburtsdatum) {
		this.geburtsdatum = geburtsdatum;
	}

	public boolean isHasCard() {
		return hasCard;
	}

	public void setHasCard(boolean hasCard) {
		this.hasCard = hasCard;
	}

	public Date getCardGueltigBis() {
		return cardGueltigBis;
	}

	public void setCardGueltigBis(Date cardGueltigBis) {
		this.cardGueltigBis = cardGueltigBis;
	}
	
    public boolean isProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(boolean profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getTelNr() {
        return telNr;
    }

    public void setTelNr(String telNr) {
        this.telNr = telNr;
    }

    public String getMobileTelNr() {
        return mobileTelNr;
    }

    public void setMobileTelNr(String mobileTelNr) {
        this.mobileTelNr = mobileTelNr;
    }
    
    public String getFullName() {
    	return vorname + " " + nachname;
    }
}
