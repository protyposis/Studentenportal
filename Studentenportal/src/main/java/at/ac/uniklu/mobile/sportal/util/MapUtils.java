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

import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.util.Linkify;
import android.widget.TextView;

public class MapUtils {
	
	public static final String ROOM_URL = "at.ac.uniklu.mobile.sportal://map/search/room?";

	private static Pattern sRoomMatcher = Pattern.compile("\\b(HS [A-Z0-9]{1,2})|([A-Z][0-9]?\\.[0-9]\\.[A-Za-z0-9]{1,3})");
	
	public static void linkifyRooms(TextView v) {
		Linkify.addLinks(v, sRoomMatcher, ROOM_URL);
	}
	
	public static void openMapAndShowRoom(Activity a, String room) {
		String uri = ROOM_URL + room;
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		a.startActivity(i);
	}
}
