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

package at.ac.uniklu.mobile.sportal.publictransport.stw;

import java.util.Date;
import java.util.List;

public class Departures {

	/**
	 * The servertime at the time of the request.
	 */
	private Date time;
	
	/**
	 * The weekday at the time of the request.
	 * Sunday = 1, Monday = 2, ..., Saturday = 7
	 */
	private int weekday;
	
	/**
	 * The name of the stop this departure monitor has been requested for.
	 */
	private String stopName;
	
	/**
	 * A map of serving line with the line number as key.
	 */
	private List<Line> lines;
	
	/**
	 * A list of departures on this line.
	 */
	private List<Departure> departures;

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public int getWeekday() {
		return weekday;
	}

	public void setWeekday(int weekday) {
		this.weekday = weekday;
	}

	public String getStopName() {
		return stopName;
	}

	public void setStopName(String stopName) {
		this.stopName = stopName;
	}

	public List<Line> getLines() {
		return lines;
	}

	public void setLines(List<Line> lines) {
		this.lines = lines;
	}

	public List<Departure> getDepartures() {
		return departures;
	}

	public void setDepartures(List<Departure> departures) {
		this.departures = departures;
	}
	
	public Date getFirstDate() {
		if(departures == null || departures.isEmpty()) {
			return null;
		}
		return departures.get(0).getTime();
	}
	
	public Date getLastDate() {
		if(departures == null || departures.isEmpty()) {
			return null;
		}
		return departures.get(departures.size() - 1).getTime();
	}
}
