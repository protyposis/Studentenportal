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

import android.content.Context;

public abstract class AsyncTaskLoader<T> extends android.support.v4.content.AsyncTaskLoader<AsyncResult<T>> {
	
	private AsyncResult<T> mResult;

	public AsyncTaskLoader(Context context) {
		super(context);
	}

	@Override
	protected void onStartLoading() {
		// http://stackoverflow.com/questions/8606048/asynctaskloader-doesnt-run
		if (mResult != null && mResult.isSuccess()) {
			deliverResult(mResult);
		} else {
			forceLoad();
		}
	}

	@Override
	public AsyncResult<T> loadInBackground() {
		T data = null;
		Exception ex = null;
		try {
			data = loadInBackgroundEx();
		} catch (Exception e) {
			ex = e;
		}
		mResult = new AsyncResult<T>(ex, data);
		return mResult;
	}

	/**
	 * Overwrite this to get automatic exception handling through the
	 * {@link AsyncResult}.
	 */
	public abstract T loadInBackgroundEx() throws Exception;

}
