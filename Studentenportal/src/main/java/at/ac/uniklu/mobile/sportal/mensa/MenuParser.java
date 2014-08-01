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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

class MenuParser {
	
	@SuppressWarnings("unused")
	private static final String TAG = MenuParser.class.getSimpleName();
	
	List<MenuItem> parse(String menu, boolean processAsString) throws XmlPullParserException, IOException {
        List<MenuItem> list = new ArrayList<MenuItem>();
        MenuItem mi = null;
        
        /* the first menu in the RSS is the detailed overview of the current day and doesn't conform to
         * XML (tags and text in a single parent tag). It just contains one item and no title so we just 
         * strip out the tags and use the string.
         */
        if(processAsString) {
			mi = new MenuItem();
			mi.description = menu.replaceAll("<[a-z/]+>", "").trim();
			list.add(mi);
		}
        else {
        	// add root element; without it, it isn't XML conform and Android <= 2.3 cannot read it and throws a ParseException
        	menu = "<menus>" + menu + "</menus>";
        	
	        XmlPullParser parser = Xml.newPullParser();
	        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	        parser.setInput(new StringReader(menu));
	        
	        int type;
	        while((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
	        	if(type == XmlPullParser.START_TAG && parser.getName().equals("menu")) {
	        		mi = new MenuItem();
	        	} else if(type == XmlPullParser.START_TAG && parser.getName().equals("day")) {
	        		parser.next();
	        		mi.title = parser.getText();
	        	} else if(type == XmlPullParser.START_TAG && parser.getName().equals("description")) {
	        		mi.description = parseDescription(parser);
	        	} else if(type == XmlPullParser.END_TAG && parser.getName().equals("menu")) {
	        		list.add(mi);
	        	}
	        }
        }
        
        return list;
	}
	
	private String parseDescription(XmlPullParser parser) throws XmlPullParserException, IOException {
        String description = "";
		int type;
        while(!((type = parser.next()) == XmlPullParser.END_TAG && parser.getName().equals("description"))) {
        	if(type == XmlPullParser.START_TAG && parser.getName().equals("p")) {
        		parser.next();
        		description += parser.getText() + "\n";
        	}
        }
        
        // strip last newline
        if(description.length() > 1) {
        	description = description.substring(0, description.length() - 1);
        }
        
        return description;
	}

}
