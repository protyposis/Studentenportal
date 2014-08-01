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

package at.ac.uniklu.mobile.sportal.util;

public class StringUtils {

	public static boolean equals(String s1, String s2) {
		// both are equal if both are null
		if(s1 == null && s2 == null) {
			return true;
		}
		// if one is null, it is assured here that both can't be null, so they're not equal
		if(s1 == null || s2 == null) {
			return false;
		}
		// both aren't null, compare their content
		return s1.equals(s2);
	}
	
	/**
	 * Null-tolerant string trimming that also removes non-breaking spaces.
	 * @param s
	 * @return
	 */
	public static String trim(String s) {
		if(s == null)
			return null;
		
		s = s.trim();
		
		// remove leading non-breaking spaces
		if(s.length() > 0) {
			while(s.indexOf(0xA0) == 0) {
				s = s.substring(1);
			}
		}
		
		if(s.length() > 0) {
			int index;
			while((index = s.lastIndexOf(0xA0)) > 0 && index == s.length() - 1) {
				s = s.substring(0, s.length() - 1);
			}
		}
		
		return s;
	}
	
	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0;
	}
}
