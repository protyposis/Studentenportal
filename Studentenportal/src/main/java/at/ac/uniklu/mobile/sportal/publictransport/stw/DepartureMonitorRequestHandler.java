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
import java.util.Calendar;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class DepartureMonitorRequestHandler extends DefaultHandler {
	
	private static enum State {
		START,
		SERVING_LINES,
		SERVING_LINES_ROUTE_DESCRIPTION,
		DEPARTURE_LIST,
		END
	}
	
	private Departures departures;
	private State state;
	private Line line;
	private Departure departure;
	private Calendar calendar;
	private String stringBuffer;

	@Override
	public void startDocument() throws SAXException {
		departures = new Departures();
		departures.setLines(new ArrayList<Line>());
		departures.setDepartures(new ArrayList<Departure>());
		state = State.START;
		calendar = Calendar.getInstance();
		calendar.clear();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(localName.equals("itdRequest")) {
			//departures.setTime(new Date(attributes.getValue("now"))); // TODO parse date
			departures.setWeekday(Integer.parseInt(attributes.getValue("nowWD")));
		} else if(localName.equals("itdServingLines")) {
			state = State.SERVING_LINES;
		} else if(state == State.SERVING_LINES && localName.equals("itdServingLine")) {
			line = new Line();
			line.setNumber(attributes.getValue("number"));
			line.setDirection(attributes.getValue("direction"));
			line.setDestinationStopCode(attributes.getValue("destID"));
			line.setIndex(attributes.getValue("index"));
		} else if(localName.equals("itdRouteDescText")) {
			state = State.SERVING_LINES_ROUTE_DESCRIPTION;
		} else if(localName.equals("itdDepartureList")) {
			state = State.DEPARTURE_LIST;
		} else if(state == State.DEPARTURE_LIST && localName.equals("itdDeparture")) {
			departure = new Departure();
			departure.setCountdown(Integer.parseInt(attributes.getValue("countdown")));
		} else if(state == State.DEPARTURE_LIST && localName.equals("itdDate")) {
			calendar.set(Integer.parseInt(attributes.getValue("year")), 
					Integer.parseInt(attributes.getValue("month")) - 1, 
					Integer.parseInt(attributes.getValue("day")));
			departure.setWeekday(Integer.parseInt(attributes.getValue("weekday")));
		} else if(state == State.DEPARTURE_LIST && localName.equals("itdTime")) {
			calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(attributes.getValue("hour")));
			calendar.set(Calendar.MINUTE, Integer.parseInt(attributes.getValue("minute")));
		} else if(state == State.DEPARTURE_LIST && localName.equals("itdServingLine")) {
			departure.setLineNumber(attributes.getValue("number"));
			departure.setDirection(attributes.getValue("direction"));
			departure.setNetwork(attributes.getValue("stateless").startsWith("ktn:050") ? "stw" : "obb");
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if(state == State.SERVING_LINES_ROUTE_DESCRIPTION) {
			stringBuffer += new String(ch, start, length);
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(state == State.SERVING_LINES_ROUTE_DESCRIPTION) {
			line.setRouteDescription(stringBuffer);
			stringBuffer = "";
			state = State.SERVING_LINES;
		} else if(state == State.SERVING_LINES && localName.equals("itdServingLine")) {
			departures.getLines().add(line);
		} else if(state == State.DEPARTURE_LIST && localName.equals("itdDeparture")) {
			departure.setTime(calendar.getTime());
			departures.getDepartures().add(departure);
		} else if(state == State.SERVING_LINES && localName.equals("itdServingLines")) {
			state = State.END;
		}
	}
	
	@Override
	public void endDocument() throws SAXException {
	}
	
	public Departures getDepartures() {
		return departures;
	}
}
