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

import android.annotation.SuppressLint;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Finds public transport stops in the Klagenfurt City public transport 
 * operator's (Stadtwerke / STW) web service.
 */
public class StopFinder {
	
	private XMLReader xmlReader;
	private StopFinderRequestHandler handler;
	
	public StopFinder() throws Exception {
		SAXParserFactory spf = SAXParserFactory.newInstance(); 
		SAXParser sp = spf.newSAXParser(); 

		xmlReader = sp.getXMLReader(); 

		handler = new StopFinderRequestHandler(); 
		xmlReader.setContentHandler(handler);
	}
	
	/**
	 * Query for all stops at a specified place satisfying the specified query.
	 */
	public List<Stop> query(Place place, String query) throws SAXException, IOException {
		InputSource is = new InputSource(new URL("http://fahrplan.verbundlinie.at/stw/XML_STOPFINDER_REQUEST?" + 
				"type_sf=undefined&typeInfo_sf=invalid&nameState_sf=empty&placeState_sf=empty&" + 
				"place_sf=placeID%3A" + place.getCodeWebsave() + "&name_sf=" + query).openStream());
		is.setEncoding("ISO-8859-1");
		xmlReader.parse(is);
		return handler.getStops();
	}
	
	/**
	 * Query for all stops that should be available for the user.
	 * @return
	 */
	@SuppressLint("UseSparseArrays")
	public List<Stop> queryAll() throws Exception {
		// get all stops without duplicates
		Map<Integer, Stop> stopMap = new HashMap<Integer, Stop>();
		for(char c : "abcdefghijklmnopqrstuvwxyz".toCharArray()) {
			for(Stop stop : query(Place.KLAGENFURT, c+"")) {
				/* Insert entry "Hauptbahnhof" in any case to overwrite "Hauptbahnhof Busbahnhof".
				 * Both have the same stop ID but "Hauptbahnhof" is the preferred name. */
				if(!stopMap.containsKey(stop.getCode()) || stop.getName().equals("Hauptbahnhof")) {
					stopMap.put(stop.getCode(), stop);
				}
			}
		}
		
		// sort stops
		List<Stop> stops = new ArrayList<Stop>();
		stops.addAll(stopMap.values());
		Collections.sort(stops, new Comparator<Stop>() {
			@Override
			public int compare(Stop s1, Stop s2) {
				return s1.getName().compareTo(s2.getName());
			}
		});
		
		return stops;
	}
}
