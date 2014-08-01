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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

class MensaParser {
	
	@SuppressWarnings("unused")
	private static final String TAG = MensaParser.class.getSimpleName();

	Mensa parse(URL url) throws IOException, XmlPullParserException {
		InputStream in = null;
		Mensa m = null;
		
		try {
			in = url.openStream();
			
			XmlPullParser parser = Xml.newPullParser();
	        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	        parser.setInput(in, null);
	        
	        int type;
	        while((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
	        	if(type == XmlPullParser.START_TAG && parser.getName().equals("channel")) {
	        		m = parseChannel(parser);
	        	}
	        }
		} finally {
			if(in != null) in.close();
		}
		
		return m;
	}
	
	private Mensa parseChannel(XmlPullParser parser) throws XmlPullParserException, IOException {
		Mensa m = new Mensa();
		m.categories = new ArrayList<MenuCategory>();
		
		int index = 0;
		int type;
        while(!((type = parser.next()) == XmlPullParser.END_TAG && parser.getName().equals("channel"))) {
        	if(type == XmlPullParser.START_TAG && parser.getName().equals("title")) {
        		parser.next();
        		m.name = parser.getText();
        	} else if(type == XmlPullParser.START_TAG && parser.getName().equals("item")) {
        		m.categories.add(parseItem(parser, index++));
        	}
        }
        
        return m;
	}
	
	private MenuCategory parseItem(XmlPullParser parser, int index) throws XmlPullParserException, IOException {
		MenuCategory m = new MenuCategory();
		
		int type;
        while(!((type = parser.next()) == XmlPullParser.END_TAG && parser.getName().equals("item"))) {
        	if(type == XmlPullParser.START_TAG && parser.getName().equals("title")) {
        		parser.next();
        		m.title = parser.getText();
        	} else if(type == XmlPullParser.START_TAG && parser.getName().equals("link")) {
        		parser.next();
        		m.link = parser.getText();
        	} else if(type == XmlPullParser.START_TAG && parser.getName().equals("description")) {
        		parser.next();
        		m.menuItems = new MenuParser().parse(StringEscapeUtils.unescapeHtml(parser.getText()), index == 0);
        	}
        }
        
        return m;
	}
}
