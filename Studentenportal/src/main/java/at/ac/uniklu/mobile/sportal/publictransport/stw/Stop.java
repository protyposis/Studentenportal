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

/**
 * Represents a supported bus/train stop in Klagenfurt. The name of the stop
 * is a proper name and doesn't need to be localized.
 */
public class Stop {
	
	/**
	 * An enumeration of supported default bus/train stops for students in Klagenfurt. 
	 * The names of the stops can reside in here since they're proper names and need not be localized.
	 */
	public static enum DefaultStop {
		UNIVERSITAET			(60906515, "Universität"),
		UNIVERSITAETSSTRASSE	(60906989, "Universitätsstraße"),
		MINIMUNDUS_UNIVERSITAET	(60904006, "Minimundus - Universität"),
		NECKHEIMGASSE			(60906476, "Neckheimgasse"),
		STERNECKSTRASSE			(60906491, "Sterneckstraße"),
		HEILIGENGEISTPLATZ		(60909108, "Heiligengeistplatz"),
		HAUPTBAHNHOF			(60909499, "Hauptbahnhof"),
		STEINERNE_BRUECKE		(60904005, "Steinerne Brücke"),
		LEND_BAHNHOF			(60903645, "Lend Bahnhof")
		;

		private int code;
		private String name;
		
		private DefaultStop(int code, String name) {
			this.code = code;
			this.name = name;
		}
		
		public int getCode() {
			return this.code;
		}
		
		public String getName() {
			return this.name;
		}
		
		public Stop getStop() {
			return new Stop(code, name);
		}
		
		@Override
		public String toString() {
			return this.name;
		}
	}
	
	private int code;
	private String name;
	
	Stop() {
	}
	
	Stop(int code, String name) {
		this.code = code;
		this.name = name;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		return code == ((Stop)o).getCode();
	}

	@Override
	public int hashCode() {
		return code;
	}
}
