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

import java.util.List;

import at.ac.uniklu.mobile.sportal.api.Termin;

public class CalendarModel {
	
	private List<Termin> dates;
	private boolean moreDatesToLoad;

	public List<Termin> getDates() {
		return dates;
	}

	public void setDates(List<Termin> dates) {
		this.dates = dates;
	}

	public boolean isMoreDatesToLoad() {
		return moreDatesToLoad;
	}

	public void setMoreDatesToLoad(boolean moreDatesToLoad) {
		this.moreDatesToLoad = moreDatesToLoad;
	}
}
