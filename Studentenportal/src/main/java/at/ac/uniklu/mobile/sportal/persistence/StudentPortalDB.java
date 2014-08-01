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

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import at.ac.uniklu.mobile.sportal.api.Termin;

public class StudentPortalDB {
	
	private static final String TAG = "StudentPortalDB";
	
	private static class MutingPeriods {
		public static final String TABLE_NAME = "mutingperiods";
		public static final String _ID = "_id";
		public static final String COLUMN_BEGIN = "begin";
		public static final String COLUMN_END = "end";
		public static final String COLUMN_NAME = "name";
		public static final String COLUMN_COURSEKEY = "coursekey";
	}
	
	private static class CourseBlacklist {
		public static final String TABLE_NAME = "courseblacklist";
		public static final String COLUMN_COURSEKEY = "coursekey";
	}
	
	private static final String DATABASE_NAME = "sportal.sqlite";
	private static final int DATABASE_VERSION = 1;
	
    private static class DbOpenHelper extends SQLiteOpenHelper {
        
        private static final String MUTINGPERIODS_TABLE_CREATE =
                    "CREATE TABLE " + MutingPeriods.TABLE_NAME + " (" +
                    MutingPeriods._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                    MutingPeriods.COLUMN_BEGIN + " INTEGER NOT NULL, " + 
                    MutingPeriods.COLUMN_END + " INTEGER NOT NULL, " + 
                    MutingPeriods.COLUMN_NAME + " TEXT NOT NULL, " + 
                    MutingPeriods.COLUMN_COURSEKEY + " INTEGER" + 
                    ");"; 
        
        private static final String COURSEBLACKLIST_TABLE_CREATE =
		            "CREATE TABLE " + CourseBlacklist.TABLE_NAME + " (" +
		            CourseBlacklist.COLUMN_COURSEKEY + " INTEGER NOT NULL" + 
		            ");"; 

        DbOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(MUTINGPERIODS_TABLE_CREATE);
            db.execSQL(COURSEBLACKLIST_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
    
    private static DbOpenHelper sDbOpenHelper;
    
    public StudentPortalDB(Context context) {
    	if(sDbOpenHelper == null) {
    		sDbOpenHelper = new DbOpenHelper(context);
    		Log.d(TAG, "cache created");
    	} else {
    		// this should never happen, but just to make sure, print an error message in case
    		Log.e(TAG, "db helper already existing, no need to recreate it");
    	}
    }
    
    /**
     * Inserts new dates into the muting table that aren't already in there.
     * @param dates
     */
    public void mutingPeriods_insertNew(List<Termin> dates) {
    	SQLiteDatabase db = sDbOpenHelper.getWritableDatabase();
    	for(Termin date : dates) {
    		if(date.isStorniert()) {
    			// don't schedule canceled dates
    			continue;
    		}
    		
    		// query for the date to be inserted
    		Cursor cursor = db.rawQuery(
    				"SELECT * FROM " + MutingPeriods.TABLE_NAME + " " + 
    				"WHERE " + MutingPeriods.COLUMN_BEGIN + " = ? " + 
    				"AND " + MutingPeriods.COLUMN_END + " = ? " + 
    				"AND " + MutingPeriods.COLUMN_NAME + " = ? " +
    				"AND " + MutingPeriods.COLUMN_COURSEKEY + " = ?", 
    				new String[] { 
    						date.getDatum().getTime()+"", 
    						date.getEndDate().getTime()+"", 
    						date.getTitleWithType(), 
    						date.getLvKey()+"" 
    						});
    		
    		// if the date doesn't exist in the table, insert it
    		if(!cursor.moveToFirst()) {
	    		ContentValues contentValues = new ContentValues(4);
	    		contentValues.put(MutingPeriods.COLUMN_BEGIN, date.getDatum().getTime());
	    		contentValues.put(MutingPeriods.COLUMN_END, date.getEndDate().getTime());
	    		contentValues.put(MutingPeriods.COLUMN_NAME, date.getTitleWithType());
	    		contentValues.put(MutingPeriods.COLUMN_COURSEKEY, date.getLvKey());
	    		db.insertOrThrow(MutingPeriods.TABLE_NAME, null, contentValues);
    		}
    		cursor.close();
    	}
    }
    
    /**
     * Deletes all past periods from the table.
     */
    public void mutingPeriods_cleanup() {
    	SQLiteDatabase db = sDbOpenHelper.getWritableDatabase();
    	db.delete(MutingPeriods.TABLE_NAME, MutingPeriods.COLUMN_END + " < ?", 
    			new String[] { System.currentTimeMillis()+"" });
    }
    
    /**
     * Deletes all periods from the table.
     */
    public void mutingPeriods_clear() {
    	SQLiteDatabase db = sDbOpenHelper.getWritableDatabase();
    	db.delete(MutingPeriods.TABLE_NAME, null, null);
    }
    
    public MutingPeriod mutingPeriods_getPeriod(int id) {
    	SQLiteDatabase db = sDbOpenHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery(
    			"SELECT * FROM " + MutingPeriods.TABLE_NAME + " " +
    			"WHERE " + MutingPeriods._ID + " = ?", new String[] { id+"" });
    	MutingPeriod mutingPeriod = null;
    	if(cursor.moveToFirst()) {
    		mutingPeriod = mutingPeriods_readPeriodFromCursor(cursor);
    	}
    	cursor.close();
    	return mutingPeriod;
    }
    
    public MutingPeriod mutingPeriods_getCurrentPeriod() {
    	SQLiteDatabase db = sDbOpenHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery(
    			"SELECT * FROM " + MutingPeriods.TABLE_NAME + " " +
    			"WHERE " + MutingPeriods.COLUMN_BEGIN + " < ? " + 
    			"AND " + MutingPeriods.COLUMN_END + " > ? " +
    			"AND " + MutingPeriods.COLUMN_COURSEKEY + 
    				" NOT IN (SELECT " + CourseBlacklist.COLUMN_COURSEKEY + " FROM " + CourseBlacklist.TABLE_NAME + ") " +
    			"ORDER BY " + MutingPeriods.COLUMN_BEGIN + " DESC", 
    			new String[] { System.currentTimeMillis()+"", System.currentTimeMillis()+"" });
    	MutingPeriod mutingPeriod = null;
    	if(cursor.moveToFirst()) {
    		mutingPeriod = mutingPeriods_readPeriodFromCursor(cursor);
    	}
    	cursor.close();
    	return mutingPeriod;
    }
    
    public MutingPeriod mutingPeriods_getNextPeriod() {
    	SQLiteDatabase db = sDbOpenHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery(
    			"SELECT * FROM " + MutingPeriods.TABLE_NAME + " " +
    			"WHERE " + MutingPeriods.COLUMN_BEGIN + " > ? " + 
    			"AND " + MutingPeriods.COLUMN_COURSEKEY + 
					" NOT IN (SELECT " + CourseBlacklist.COLUMN_COURSEKEY + " FROM " + CourseBlacklist.TABLE_NAME + ") " +
    			"ORDER BY " + MutingPeriods.COLUMN_BEGIN + " ASC", 
    			new String[] { System.currentTimeMillis()+"" });
    	MutingPeriod mutingPeriod = null;
    	if(cursor.moveToFirst()) {
    		mutingPeriod = mutingPeriods_readPeriodFromCursor(cursor);
    	}
    	cursor.close();
    	return mutingPeriod;
    }
    
    private MutingPeriod mutingPeriods_readPeriodFromCursor(Cursor cursor) {
    	MutingPeriod mutingPeriod = new MutingPeriod();
		mutingPeriod.setId(cursor.getInt(cursor.getColumnIndex(MutingPeriods._ID)));
		mutingPeriod.setBegin(cursor.getLong(cursor.getColumnIndex(MutingPeriods.COLUMN_BEGIN)));
		mutingPeriod.setEnd(cursor.getLong(cursor.getColumnIndex(MutingPeriods.COLUMN_END)));
		mutingPeriod.setName(cursor.getString(cursor.getColumnIndex(MutingPeriods.COLUMN_NAME)));
		mutingPeriod.setCourseKey(cursor.getInt(cursor.getColumnIndex(MutingPeriods.COLUMN_COURSEKEY)));
		return mutingPeriod;
    }
    
    public void courseBlacklist_insert(int courseKey) {
    	SQLiteDatabase db = sDbOpenHelper.getWritableDatabase();
    	ContentValues contentValues = new ContentValues(1);
    	contentValues.put(CourseBlacklist.COLUMN_COURSEKEY, courseKey);
    	db.insertOrThrow(CourseBlacklist.TABLE_NAME, null, contentValues);
    }
    
    public boolean courseBlacklist_isListed(int courseKey) {
    	SQLiteDatabase db = sDbOpenHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery(
    			"SELECT * FROM " + CourseBlacklist.TABLE_NAME + " " + 
    			"WHERE " + CourseBlacklist.COLUMN_COURSEKEY + " = ?", 
    			new String[] { courseKey+"" });
    	boolean isListed = cursor.moveToFirst();
    	cursor.close();
    	return isListed;
    }
    
    public void courseBlacklist_delete(int courseKey) {
    	SQLiteDatabase db = sDbOpenHelper.getWritableDatabase();
    	db.delete(CourseBlacklist.TABLE_NAME, CourseBlacklist.COLUMN_COURSEKEY + " = ?", new String[] { courseKey+"" });
    }
    
    public List<Integer> courseBlacklist() {
    	List<Integer> courseKeys = new ArrayList<Integer>();
    	SQLiteDatabase db = sDbOpenHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery("SELECT * FROM " + CourseBlacklist.TABLE_NAME, null);
    	while(cursor.moveToNext()) {
    		courseKeys.add(cursor.getInt(0));
    	}
    	cursor.close();
    	return courseKeys;
    }
}
