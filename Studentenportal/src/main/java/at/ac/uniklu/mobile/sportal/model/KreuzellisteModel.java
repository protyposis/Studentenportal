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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.SparseBooleanArray;
import at.ac.uniklu.mobile.sportal.R;
import at.ac.uniklu.mobile.sportal.api.Kreuzelliste;
import at.ac.uniklu.mobile.sportal.api.KreuzellisteAufgabe;

public class KreuzellisteModel {
	
	private Kreuzelliste kreuzelliste;

	public Kreuzelliste getKreuzelliste() {
		return kreuzelliste;
	}

	public void setKreuzelliste(Kreuzelliste kreuzelliste) {
		this.kreuzelliste = kreuzelliste;
	}
	
	public String[] getAufgabenTitel(Context context) {
		String prefix = context.getString(R.string.course_checklist_exercise) + " ";
		String[] titel = new String[kreuzelliste.getAufgaben().size()];
		for(int i = 0; i < titel.length; i++) {
			titel[i] = prefix + kreuzelliste.getAufgaben().get(i).getNummer();
		}
		return titel;
	}
	
	public boolean[] getAufgabenKreuzel() {
		boolean[] kreuzel = new boolean[kreuzelliste.getAufgaben().size()];
		for(int i = 0; i < kreuzel.length; i++) {
			kreuzel[i] = kreuzelliste.getAufgaben().get(i).isGekreuzt();
		}
		return kreuzel;
	}
	
	/**
	 * Creates a copy of the Kreuzelliste with all data that is needed for posting.
	 * @param checks
	 * @return
	 */
	public Kreuzelliste prepareForSubmit(SparseBooleanArray checks) {
		Kreuzelliste kl = new Kreuzelliste();
		kl.setKey(kreuzelliste.getKey());
		
		List<KreuzellisteAufgabe> aufgaben = new ArrayList<KreuzellisteAufgabe>();
		for(int i = 0; i < kreuzelliste.getAufgaben().size(); i++) {
			boolean value = checks.valueAt(i);
			KreuzellisteAufgabe aufgabe = new KreuzellisteAufgabe();
			aufgabe.setKey(kreuzelliste.getAufgaben().get(i).getKey());
			aufgabe.setNummer(kreuzelliste.getAufgaben().get(i).getNummer());
			aufgabe.setGekreuzt(value);
			aufgaben.add(aufgabe);
		}
		
		kl.setAufgaben(aufgaben);
		return kl;
	}
}
