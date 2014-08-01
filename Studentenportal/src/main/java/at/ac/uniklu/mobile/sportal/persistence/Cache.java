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

package at.ac.uniklu.mobile.sportal.persistence;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Cache {
	
	private static final String TAG = "Cache";
	
    private static class CacheDbOpenHelper extends SQLiteOpenHelper {

        private static final String CACHE_DATABASE_FILE = "sportalcache.sqlite";

        private static final int DATABASE_VERSION = 1;
        private static final String JSON_TABLE_NAME = "jsonstring";
        
        private static final String JSON_TABLE_CREATE =
                    "CREATE TABLE " + JSON_TABLE_NAME + " (" +
                    "apimethod TEXT PRIMARY KEY, " + // api method
                    "mnr TEXT NOT NULL, " + // matriculation number
                    "timestamp INTEGER NOT NULL, " + // time of storage in milliseconds (date.getTime())
                    "json TEXT NOT NULL);"; // json string returned by the api method

        CacheDbOpenHelper(Context context) {
            super(context, CACHE_DATABASE_FILE, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(JSON_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Ignore.
        }
    }
    
    private static final long CACHE_VALID_TIME = 1000 * 60 * 60; // cache entries are valid for an hour
    
    private static CacheDbOpenHelper sCacheDbOpenHelper;
    
    public Cache(Context applicationContext) {
    	if(sCacheDbOpenHelper == null) {
    		sCacheDbOpenHelper = new CacheDbOpenHelper(applicationContext);
    		Log.d(TAG, "cache created");
    	} else {
    		// this should never happen, but just to make sure, print an error message in case
    		Log.e(TAG, "cache helper already existing, no need to recreate it");
    	}
    }
    
    public boolean checkForExistingRecord(String method, String mnr) {
    	SQLiteDatabase database = sCacheDbOpenHelper.getReadableDatabase();
    	Cursor cursor = database.rawQuery("SELECT * FROM jsonstring WHERE apimethod like ? and mnr like ?", 
    			new String[] { method, mnr });
    	boolean recordExists = cursor.moveToFirst();
    	cursor.close();
    	return recordExists;
    }
    
    public boolean checkForExistingValidRecord(String method, String mnr) {
    	SQLiteDatabase database = sCacheDbOpenHelper.getReadableDatabase();
    	Cursor cursor = database.rawQuery("SELECT * FROM jsonstring WHERE apimethod like ? and mnr like ? and timestamp > ?", 
    			new String[] { method, mnr, (new Date().getTime() - CACHE_VALID_TIME)+"" });
    	boolean recordExists = cursor.moveToFirst();
    	cursor.close();
    	return recordExists;
    }

    public void put(String method, String mnr, String jsonResult) {
    	// here the MNR is not used as an WHERE argument to override a potentially
    	// existing record, no matter what user was logged in previously
    	// otherwise the database would store data from students forever, if they were
    	// just logged in once on a phone
    	Object[] values = new Object[] {
    			method,
    			mnr,
    			new Date().getTime(),
    			jsonResult
    	};
    	SQLiteDatabase database = sCacheDbOpenHelper.getWritableDatabase();
    	database.execSQL("INSERT OR REPLACE INTO jsonstring (apimethod, mnr, timestamp, json) VALUES (?, ?, ?, ?)", values);
    }
    
    public String get(String method, String mnr) {
    	// the MNR is used to fetch data from the actual logged in user
    	// in case that multiple students would login on the same phone, the cache would
    	// otherwise return cached data from the previously logged in user
    	SQLiteDatabase database = sCacheDbOpenHelper.getReadableDatabase();
    	Cursor cursor = database.rawQuery("SELECT json FROM jsonstring WHERE apimethod like ? and mnr like ?  and timestamp > ?", 
    			new String[] { method, mnr, (new Date().getTime() - CACHE_VALID_TIME)+"" });
    	String result = null;
    	if(cursor.moveToFirst()) {
    		result = cursor.getString(0);
    	}
    	cursor.close();
    	return result;
    }
    
    public void clear(String method, String mnr) {
    	SQLiteDatabase database = sCacheDbOpenHelper.getWritableDatabase();
    	database.delete(CacheDbOpenHelper.JSON_TABLE_NAME, 
    			"apimethod like ? and mnr like ?", new String[] { method, mnr });
    }
    
    public void clear() {
    	SQLiteDatabase database = sCacheDbOpenHelper.getWritableDatabase();
    	database.delete(CacheDbOpenHelper.JSON_TABLE_NAME, null, null);
    }
}
