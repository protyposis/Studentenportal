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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class StopFinderRequestHandler extends DefaultHandler {
	
	private static enum State {
		START,
		STOP_FINDER_REQUEST,
		STOP_LIST, // state="notidentified|identified|list"
		STOP,
		END
	}
	
	private List<Stop> stops;
	private State state;
	private Stop stop;
	private String stringBuffer;

	@Override
	public void startDocument() throws SAXException {
		stops = new ArrayList<Stop>();
		state = State.START;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(localName.equals("itdStopFinderRequest")) {
			state = State.STOP_FINDER_REQUEST;
		} else if(state == State.STOP_FINDER_REQUEST && localName.equals("itdOdvName") && !attributes.getValue("state").equals("notidentified")) {
			state = State.STOP_LIST;
		} else if(state == State.STOP_LIST && localName.equals("odvNameElem")) {
			state = State.STOP;
			stop = new Stop();
			stop.setCode(Integer.parseInt(attributes.getValue("stopID")));
			stringBuffer = "";
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if(state == State.STOP) {
			// all calls of the characters method between a start and end tag need to be concatenated
			// http://sourceforge.net/tracker/?func=detail&aid=1538813&group_id=29449&atid=396219
			stringBuffer += new String(ch, start, length);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(state == State.STOP && localName.equals("odvNameElem")) {
			state = State.STOP_LIST;
			stop.setName(stringBuffer);
			stops.add(stop);
		}
	}
	
	@Override
	public void endDocument() throws SAXException {
	}
	
	public List<Stop> getStops() {
		return stops;
	}
}
