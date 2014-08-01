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

public class Studium {

    private int key;
    private String name;
    private String skz;
    private Date beginn;
    private Date ende;
    private Link curriculum1;
    private Link curriculum2;

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

    public String getSkz() {
        return skz;
    }

    public void setSkz(String skz) {
        this.skz = skz;
    }

    public Date getBeginn() {
        return beginn;
    }

    public void setBeginn(Date beginn) {
        this.beginn = beginn;
    }

    public Date getEnde() {
        return ende;
    }

    public void setEnde(Date ende) {
        this.ende = ende;
    }
    
    public boolean isBeendet() {
    	return this.ende != null;
    }

    public Link getCurriculum1() {
        return curriculum1;
    }

    public void setCurriculum1(Link curriculum1) {
        this.curriculum1 = curriculum1;
    }

    public Link getCurriculum2() {
        return curriculum2;
    }

    public void setCurriculum2(Link curriculum2) {
        this.curriculum2 = curriculum2;
    }
}
