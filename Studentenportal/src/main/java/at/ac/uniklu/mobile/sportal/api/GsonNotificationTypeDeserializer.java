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

import at.ac.uniklu.mobile.sportal.api.Notification.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Custom deserializer for the {@link Notification.Type} enumeration. This is needed since
 * new types of notification can be implemented and returned by the server without being 
 * supported on the client (e.g. because it's an old client version). Normally this would 
 * provoke an exception and the deserialization fails, in this special case the type gets
 * set to UNKNOWN so it can be ignored by the client and normal processing continues.
 */
public class GsonNotificationTypeDeserializer implements JsonDeserializer<Type> {

	@Override
	public Type deserialize(JsonElement json, java.lang.reflect.Type classOfT, 
			JsonDeserializationContext context) throws JsonParseException {
		try {
			return Type.valueOf(json.getAsString());
		} catch (IllegalArgumentException e) {
			return Type.UNKNOWN;
		}
	}

}
