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

package at.ac.uniklu.mobile.sportal.model;

import at.ac.uniklu.mobile.sportal.api.Einstellungen;

public class EinstellungenModel {
	
	/**
	 * This are the original settings that come from the server.
	 */
	private Einstellungen serverEinstellungen;
	
	/**
	 * This settings come from the server but can be changed locally to be sent
	 * back to the server.
	 */
	private Einstellungen einstellungen;

	public Einstellungen getServerEinstellungen() {
		return serverEinstellungen;
	}

	public void setServerEinstellungen(Einstellungen serverEinstellungen) {
		this.serverEinstellungen = serverEinstellungen;
	}

	public Einstellungen getEinstellungen() {
		return einstellungen;
	}

	public void setEinstellungen(Einstellungen einstellungen) {
		this.einstellungen = einstellungen;
	}
	
	/**
	 * Checks if the settings have been changed. Only if they have been changed 
	 * they are required to be posted to the server.
	 * @return true if some settings have been changed, else false
	 */
	public boolean isEinstellungenChanged() {
		return !serverEinstellungen.equals(einstellungen);
	}

}
