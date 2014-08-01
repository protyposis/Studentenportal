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

public class Departure implements Comparable<Departure> {
	
	/**
	 * The number of the line this departure belongs to.
	 */
	private String lineNumber;
	
	/**
	 * The name of the public transportation network.
	 */
	private String network;
	
	/**
	 * The direction the bus is heading to.
	 */
	private String direction;

	/**
	 * The number of minutes relative to the time of the request ({@link Departures#getTime()})
	 * after which the bus will depart. 
	 */
	private int countdown;
	
	/**
	 * The absolute departure time.
	 */
	private Date time;
	
	/**
	 * The weekday of the departure. Can be compared with {@link Departures#getWeekday()} to
	 * detect if this departure is the same day for which the request was made, or one of the following.
	 * Sunday = 1, Monday = 2, ..., Saturday = 7
	 */
	private int weekday;

	public String getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(String lineNumber) {
		this.lineNumber = lineNumber;
	}

	public String getNetwork() {
		return network;
	}

	public void setNetwork(String network) {
		this.network = network;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public int getCountdown() {
		return countdown;
	}

	public void setCountdown(int countdown) {
		this.countdown = countdown;
	}

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

	@Override
	public int compareTo(Departure another) {
		// first: time
		if(!time.equals(another.getTime())) {
			return time.compareTo(another.getTime());
		} 
		else {
			// second: line
			if(!lineNumber.equals(another.getLineNumber())) {
				return lineNumber.compareTo(another.getLineNumber());
			}
			else {
				// third: direction
				return direction.compareTo(another.getDirection());
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Departure) {
			return compareTo((Departure)o) == 0;
		}
		return false;
	}
}
