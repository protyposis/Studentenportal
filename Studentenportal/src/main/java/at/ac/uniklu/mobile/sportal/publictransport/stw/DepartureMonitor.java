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

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.util.Log;
import at.ac.uniklu.mobile.sportal.util.Utils;

/**
 * Reads departure information for a given stop from Klagenfurt City public transport 
 * operator's (Stadtwerke / STW) web service.
 * The web service isn't public but I could find it out by studying publicly available 
 * documents on the software producer's website and other public transport operators that 
 * use the same software. It is part of the EFA software platform (Elektronische Fahrplanauskunft)
 * written by Mentz Datenverarbeitung GmbH (http://www.mentzdv.de/).
 */
public class DepartureMonitor {
	
	private XMLReader xmlReader;
	private DepartureMonitorRequestHandler handler;
	
	public DepartureMonitor() throws Exception {
		SAXParserFactory spf = SAXParserFactory.newInstance(); 
		SAXParser sp = spf.newSAXParser(); 

		xmlReader = sp.getXMLReader(); 

		handler = new DepartureMonitorRequestHandler(); 
		xmlReader.setContentHandler(handler);
	}
	
	public Departures queryMonitorForStop(Stop stop, Date timestamp, Line line)
			throws DepartureMonitorException {
		try {
			List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>(10);
			params.add(new BasicNameValuePair("type_dm", "stop"));
			params.add(new BasicNameValuePair("placeID", Place.KLAGENFURT.getCodeWebsave()));
			params.add(new BasicNameValuePair("mode", "direct"));
			params.add(new BasicNameValuePair("nameInfo_dm", stop.getCode()+""));
			
			if(timestamp != null) {
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
				SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
				params.add(new BasicNameValuePair("itdDateDayMonthYear", dateFormat.format(timestamp)));
				params.add(new BasicNameValuePair("itdTime", timeFormat.format(timestamp)));
			}
			
			if(line != null) {
				params.add(new BasicNameValuePair("dmLineSelectionAll", "0"));
				params.add(new BasicNameValuePair("dmLineSelection", line.getIndex()));
			} else {
				params.add(new BasicNameValuePair("dmLineSelectionAll", "1"));
			}
			
			InputSource is = new InputSource(new URL(
					"http://fahrplan.verbundlinie.at/stw/XML_DM_REQUEST" + 
					Utils.buildQuery(params.toArray(new BasicNameValuePair[0]))
					).openStream());
			is.setEncoding("ISO-8859-1");
			xmlReader.parse(is); 
		} catch(SAXException se) {
			Log.e("SAX XML", "sax error", se);
			throw new DepartureMonitorException("SAX XML / sax error", se);
		} catch(IOException ioe) { 
			Log.e("SAX XML", "sax parse io error", ioe); 
			throw new DepartureMonitorException("SAX XML / sax parse io error", ioe);
		} 

		return handler.getDepartures();
	}
}
