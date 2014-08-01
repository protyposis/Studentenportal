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

package at.ac.uniklu.mobile.sportal.mensa;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import at.ac.uniklu.mobile.sportal.Studentportal;
import at.ac.uniklu.mobile.sportal.util.Analytics;
import at.ac.uniklu.mobile.sportal.util.StringUtils;

public class MensaMenus {
	
	private static final String TAG = "MensaMenus";
	private static final String NAME_PREFIX = "Uni Klagenfurt ";
	
	static class MensaSource {
		
		public String name;
		public String url;
		
		public MensaSource(String name, String url) {
			this.name = name;
			this.url = url;
		}
	}
	
	private static List<MensaSource> sSources;
	
	static {
		sSources = new ArrayList<MensaSource>();
		sSources.add(new MensaSource("Uni Klagenfurt Mensa", "http://menu.mensen.at/index/rss/locid/45"));
		sSources.add(new MensaSource("Uni Klagenfurt M-Cafe", "http://menu.mensen.at/index/rss/locid/46"));
		
		if(Studentportal.isDebugBuild()) {
			sSources.add(new MensaSource("Mensa ganze Woche", "http://android.protyposis.net/aausp/mensa/mensa-ganze-woche.rss"));
			sSources.add(new MensaSource("Mensa Feiertag", "http://android.protyposis.net/aausp/mensa/mensa-feiertag.rss"));
			sSources.add(new MensaSource("Mensa Sommerferien", "http://android.protyposis.net/aausp/mensa/mensa-sommerferien.rss"));
			sSources.add(new MensaSource("M-Cafe ganze Woche", "http://android.protyposis.net/aausp/mensa/mcafe-ganze-woche.rss"));
			sSources.add(new MensaSource("M-Cafe Feiertag", "http://android.protyposis.net/aausp/mensa/mcafe-feiertag.rss"));
			sSources.add(new MensaSource("M-Cafe Sommerferien", "http://android.protyposis.net/aausp/mensa/mcafe-sommerferien.rss"));
		}
	}
	
	public static List<Mensa> retrieve() {
		List<Mensa> list = new ArrayList<Mensa>();
		MensaParser mp = new MensaParser();
		
		for(MensaSource ms : sSources) {
			try {
				Mensa m = mp.parse(new URL(ms.url));
				
				if(m.name.startsWith(NAME_PREFIX)) {
					m.name = m.name.substring(NAME_PREFIX.length());
				}
				
				if(m.name.contains("Mensa")) {
					refineMensaToday(m);
				} else if(m.name.contains("M-Cafe")) {
					refineMCafeToday(m);
				}
				
				list.add(m);
			}
			catch (Exception e) {
				Analytics.onError(Analytics.ERROR_GENERIC + "/" + Analytics.ACTIVITY_MENSA, e);
				/* on error, return mensa object with NULL categories - if no data available but parsing 
	        	 * was successful, an empty categories list is returned by the parser */
	        	Mensa m = new Mensa();
	        	m.name = ms.name;
	        	m.categories = null;
	        	list.add(m);
			}
		}
		
		return list;
	}
	
	/**
	 * Processes the continuous string of today's offers and splits them into separate
	 * menu items for a nicer presentation.
	 * It does all the processing in a try/catch block and swallows all exceptions to avoid
	 * the menu not being displayed at all if the format of the processed text changes and
	 * the refinement fails.
	 * @param m
	 */
	private static void refineMensaToday(Mensa m) {
		try {
			if(m.categories == null || m.categories.isEmpty() || m.categories.get(0).menuItems.isEmpty()) {
				Log.d(TAG, "no data: " + m.name);
				return;
			}
			MenuCategory mc = m.categories.get(0);
			MenuItem sourceItem = mc.menuItems.get(0);
			List<MenuItem> targetItems = new ArrayList<MenuItem>();
			
			BufferedReader reader = new BufferedReader(new StringReader(sourceItem.description));
			
			String line = null;
			String description = "";
			MenuItem mi = null;
			while((line = reader.readLine()) != null) {
				if(line.matches(".*\\([0-9:\\s\\-]+\\).*")) {
					if(mi != null) {
						if(description.length() > 1) {
				        	description = description.substring(0, description.length() - 1);
				        }
						mi.description = description;
						targetItems.add(mi);
						description = "";
					}
					mi = new MenuItem();
					
					// remove trailing colon
					int last = line.lastIndexOf(":");
					int last2 = line.lastIndexOf(")");
					if(last > 0 && last > last2) {
						line = line.substring(0, last);
					}
					
					mi.title = line;
				} else if(!StringUtils.isEmpty(line)) {
					description += line + "\n";
				}
			}
			if(mi != null) {
				if(description.length() > 1) {
		        	description = description.substring(0, description.length() - 1);
		        }
				mi.description = description;
				targetItems.add(mi);
			}
			
			mc.menuItems = targetItems;
		} catch (Exception e) {
			Analytics.onError(Analytics.ERROR_GENERIC, e);
		}
	}
	
	private static void refineMCafeToday(Mensa m) {
		try {
			if(m.categories == null || m.categories.isEmpty() || m.categories.get(0).menuItems.isEmpty()) {
				Log.d(TAG, "no data: " + m.name);
				return;
			}
			MenuCategory mc = m.categories.get(0);
			MenuItem sourceItem = mc.menuItems.get(0);
			List<MenuItem> targetItems = new ArrayList<MenuItem>();
			
			for(String item : sourceItem.description.split("(?m)\\*+|^$")) {
				MenuItem mi = new MenuItem();
				mi.description = StringUtils.trim(item);
				
				// break text into title and description
				int breakIndex;
				if(mi.description.matches("\".+\"[A-Za-z].+")) {
					breakIndex = mi.description.indexOf("\"", 2) + 1;
					
				} else {
					breakIndex = mi.description.indexOf("\n");
					
				}
				mi.title = StringUtils.trim(mi.description.substring(0, breakIndex));
				mi.description = StringUtils.trim(mi.description.substring(breakIndex));
				
				// strip quotation marks
				if(mi.title.startsWith("\"")) {
					mi.title = mi.title.substring(1, mi.title.length() - 1);
				}
				
				targetItems.add(mi);
			}
			
			mc.menuItems = targetItems;
		} catch (Exception e) {
			Analytics.onError(Analytics.ERROR_GENERIC, e);
		}
	}

}
