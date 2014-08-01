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

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StopDB {
	
	private static class Stops {
		public static final String TABLE_NAME = "stops";
		public static final String _ID = "_id";
		public static final String COLUMN_NAME = "name";
		public static final String COLUMN_SELECTED = "selected";
	}
	
	private static final String DATABASE_NAME = "pt-stw.sqlite";
	private static final int DATABASE_VERSION = 1;
	
    private static class DbOpenHelper extends SQLiteOpenHelper {
        
        private static final String STOPS_TABLE_CREATE =
                    "CREATE TABLE " + Stops.TABLE_NAME + " (" +
                    Stops._ID + " INTEGER PRIMARY KEY, " + // manual key, do not AUTOINCREMENT
                    Stops.COLUMN_NAME + " TEXT NOT NULL, " + 
                    Stops.COLUMN_SELECTED + " INTEGER DEFAULT 0" + 
                    ");"; 

        DbOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(STOPS_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
    
    private DbOpenHelper mDbOpenHelper;
    
    public StopDB(Context context) {
    	mDbOpenHelper = new DbOpenHelper(context);
    }
    
    public void close() {
    	mDbOpenHelper.close();
    }
    
    public boolean isEmpty() {
    	SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + Stops.TABLE_NAME, null);
    	cursor.moveToFirst();
    	boolean result = cursor.getInt(0) == 0;
    	cursor.close();
    	db.close();
    	return result;
    }
   
    /**
     * Deletes all stops from the table.
     */
    public void clearStops() {
    	SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
    	db.delete(Stops.TABLE_NAME, null, null);
    	db.close();
    }
    
    public Stop getSelectedStop(int code) {
    	Stop stop = null;
    	
    	SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery("SELECT * FROM " + Stops.TABLE_NAME 
    			+ " WHERE " + Stops._ID + " = " + code + " AND " + Stops.COLUMN_SELECTED + " = 1", null);
    	if(cursor.moveToFirst()) {
    		stop = new Stop(code, cursor.getString(1));
    	}
    	cursor.close();
    	db.close();
    	
    	return stop;
    }
    
    /**
     * Loads all stops.
     */
    public List<Stop> getStops() {
    	List<Stop> stops = new ArrayList<Stop>();
    	SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery("SELECT * FROM " + Stops.TABLE_NAME 
    			+ " ORDER BY " + Stops.COLUMN_NAME, null);
    	while(cursor.moveToNext()) {
    		stops.add(new Stop(cursor.getInt(0), cursor.getString(1)));
    	}
    	cursor.close();
    	db.close();
    	return stops;
    }
    
    /**
     * Loads all selected stops. A selected stop is a stop that a user wants to see in
     * the departure monitor stops list.
     */
    public List<Stop> getSelectedStops() {
    	List<Stop> stops = new ArrayList<Stop>();
    	SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
    	Cursor cursor = db.rawQuery("SELECT * FROM " + Stops.TABLE_NAME 
    			+ " WHERE " + Stops.COLUMN_SELECTED + " = 1 ORDER BY " + Stops.COLUMN_NAME, null);
    	while(cursor.moveToNext()) {
    		stops.add(new Stop(cursor.getInt(0), cursor.getString(1)));
    	}
    	cursor.close();
    	db.close();
    	return stops;
    }
    
    public void insertStops(List<Stop> stops) {
    	SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
    	db.beginTransaction();
    	
    	for(Stop stop : stops) {
	    	ContentValues contentValues = new ContentValues(4);
			contentValues.put(Stops._ID, stop.getCode());
			contentValues.put(Stops.COLUMN_NAME, stop.getName());
			db.insertOrThrow(Stops.TABLE_NAME, null, contentValues);
    	}
    	
    	db.setTransactionSuccessful();
    	db.endTransaction();
    	db.close();
    }
    
    public void selectStop(Stop stop) {
    	writeStopSelection(stop, true);
    }
    
    public void deselectStop(Stop stop) {
    	writeStopSelection(stop, false);
    }
    
    private void writeStopSelection(Stop stop, boolean selected) {
    	SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
    	ContentValues contentValues = new ContentValues(1);
    	contentValues.put(Stops.COLUMN_SELECTED, selected ? 1 : 0);
    	db.update(Stops.TABLE_NAME, contentValues, Stops._ID + "=" + stop.getCode(), null);
    	db.close();
    }
}
