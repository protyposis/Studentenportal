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

package at.ac.uniklu.mobile.sportal;

import android.app.Activity;
import android.content.Intent;

/**
 * Serves as an entry point for a separate departure monitor app launcher icon that can be deactivated programmatically
 * to switch the separate launcher icon on and off.
 */
public class BusDeparturesLaunchActivity extends Activity {
	
	@Override
	protected void onResume() {
		super.onResume();
		startActivity(new Intent(this, BusDeparturesActivity.class));
		finish();
	}
}
