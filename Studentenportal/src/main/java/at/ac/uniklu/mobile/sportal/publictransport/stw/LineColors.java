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

import java.util.HashMap;
import java.util.Map;

public class LineColors {

	private static Map<String, Integer> colorMap;
	
	static {
		colorMap = new HashMap<String, Integer>();
		colorMap.put("10", 0xff126182);
		colorMap.put("20", 0xff0f8cab);
		colorMap.put("30", 0xff611f80);
		colorMap.put("31", 0xff942969);
		colorMap.put("40", 0xff8ca11a);
		colorMap.put("41", 0xffa82e1a);
		colorMap.put("42", 0xff738414);
		colorMap.put("43", 0xffcc6329);
		colorMap.put("50", 0xff056e4d);
		colorMap.put("60", 0xffb87d24);
		colorMap.put("61", 0xffc79b63);
		colorMap.put("80", 0xff848487);
		colorMap.put("81", 0xffc4c4c7);
		colorMap.put("82", 0xffa6a6a8);
		colorMap.put("85", 0xff000000);
		colorMap.put("91", 0xff2093b0);
		colorMap.put("92", 0xffcc6329);
		colorMap.put("93", 0xff942969);
		colorMap.put("94", 0xffa82e1a);
		colorMap.put("95", 0xff056e4d);
		colorMap.put("96", 0xffb87d24);
		colorMap.put("98", 0xff848487);
	}
	
	public static int getColor(String line) {
		Integer color = colorMap.get(line);
		if(color == null) {
			color = 0x00000000; // default color for unmapped lines
		}
		return color;
	}
}
