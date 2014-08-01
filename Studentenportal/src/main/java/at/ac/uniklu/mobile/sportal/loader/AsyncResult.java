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

package at.ac.uniklu.mobile.sportal.loader;

public class AsyncResult<T> {

	private Exception mException;
	private T mData;
	
	public AsyncResult(Exception mException, T mData) {
		this.mException = mException;
		this.mData = mData;
	}
	
	public AsyncResult(T data) {
		this(null, data);
	}

	public AsyncResult(Exception exception) {
		this(exception, null);
	}

	public void setException(Exception exception) {
		this.mException = exception;
	}

	public Exception getException() {
		return mException;
	}

	public void setData(T data) {
		this.mData = data;
	}

	public T getData() {
		return mData;
	}

	private boolean hasException() {
		return mException != null;
	}
	
	private boolean hasData() {
		return mData != null;
	}
	
	public boolean isSuccess() {
		return hasData() && !hasException();
	}
	
	public boolean isFailure() {
		return hasException();
	}
}