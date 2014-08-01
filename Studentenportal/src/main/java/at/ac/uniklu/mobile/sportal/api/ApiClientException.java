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

public class ApiClientException extends Exception {
	
	public static enum Code {
		NONE,
		UNKNOWNHOST,
		TIMEOUT,
		READING_RESPONSE,
		LOGIN_FAILED,
		LOGIN_FAILED_STAFF,
		SSL
	}
	
	private static final long serialVersionUID = -736419520370405444L;
	
	private Code code;

	public ApiClientException() {
		super();
	}

	public ApiClientException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public ApiClientException(String detailMessage) {
		super(detailMessage);
	}
	
	public ApiClientException(Code code, Throwable throwable) {
		super(throwable);
		this.code = code;
	}

	public ApiClientException(Code code) {
		super();
		this.code = code;
	}

	public ApiClientException(Throwable throwable) {
		super(throwable);
	}
	
	public boolean hasCode() {
		return code != null && code != Code.NONE;
	}
	
	public Code getCode() {
		return code;
	}
	
	@Override
	public String toString() {
		return super.toString() + 
			(hasCode() ? " [code " + code.ordinal() + "/" + code.name() + "]" : "");
	}
}
