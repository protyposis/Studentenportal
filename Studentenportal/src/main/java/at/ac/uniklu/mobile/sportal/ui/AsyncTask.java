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

package at.ac.uniklu.mobile.sportal.ui;

import android.annotation.TargetApi;
import android.os.Build;

public abstract class AsyncTask {
	
	private boolean mIsExecuting;
	private Exception mBackgroundException;
	
	protected void onPreExecute() {}
	protected abstract void doInBackground() throws Exception;
	protected void onCancelled() {}
	protected void onPostExecute() {}
	protected void onSuccess() {}
	protected void onException(Exception e) {}
	
	public boolean isExecuting() {
		return mIsExecuting;
	}
	
	public boolean isExceptionThrown() {
		return mBackgroundException != null;
	}
	
	public Exception getException() {
		return mBackgroundException;
	}
	
	public void execute() {
		if(mIsExecuting) {
			throw new RuntimeException("task is already executing");
		}
		
		onExecuteInternal();
	}
	
	public void cancel(boolean mayInterruptIfRunning) {
		if(mIsExecuting) {
			mTask.cancel(mayInterruptIfRunning);
		}
	}
	
	public boolean isCancelled() {
		return mTask.isCancelled();
	}
	
	@TargetApi(11)
	void onExecuteInternal() {
		mIsExecuting = true;
		mBackgroundException = null;

		// always use the thread pool, avoid serial executor in Android >= 3.0
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// http://developer.android.com/reference/android/os/AsyncTask.html#executeOnExecutor(java.util.concurrent.Executor,
			// Params...)
			mTask.executeOnExecutor(android.os.AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			mTask.execute();
		}
	}
	
	/**
	 * Post-execute callbacks will only be called if this method returns true.
	 * @return true to execute post-execute actions, else false
	 */
	boolean shouldPostExecute() {
		// postExecute will only be called if this method returns true
		return true;
	}
	
	void onExecutionFinishedInternal() {
		mIsExecuting = false;
	}
	
	private android.os.AsyncTask<Void, Void, Void> mTask = new android.os.AsyncTask<Void, Void, Void>() {
		
		@Override
		protected void onPreExecute() {
			AsyncTask.this.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				AsyncTask.this.doInBackground();
			} catch (Exception e) {
				AsyncTask.this.mBackgroundException = e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(shouldPostExecute()) {
				AsyncTask.this.onPostExecute();
				if(AsyncTask.this.isExceptionThrown()) {
					AsyncTask.this.onException(AsyncTask.this.getException());
				} else {
					AsyncTask.this.onSuccess();
				}
			}
			AsyncTask.this.onExecutionFinishedInternal();
		}
		
		@Override
		protected void onCancelled() {
			AsyncTask.this.onCancelled();
			AsyncTask.this.onExecutionFinishedInternal();
		}
	};
}
