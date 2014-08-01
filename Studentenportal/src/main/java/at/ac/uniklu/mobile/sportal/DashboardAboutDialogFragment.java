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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import at.ac.uniklu.mobile.sportal.util.AppInfo;
import at.ac.uniklu.mobile.sportal.util.Utils;

public class DashboardAboutDialogFragment extends DialogFragment {
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AppInfo appInfo = Studentportal.getAppInfo();

		View aboutDialogView = getActivity().getLayoutInflater().inflate(R.layout.about_dialog, null);
		TextView aboutDialogVersionTextView = (TextView)aboutDialogView.findViewById(R.id.about_version_info);
		aboutDialogVersionTextView.setText(getString(R.string.app_version, appInfo.getVersionName()));
		TextView creditsTextView = (TextView)aboutDialogView.findViewById(R.id.credits);
		String creditsText = getString(R.string.credits_iconic) + " / " + 
				getString(R.string.credits_tnp) + " / " + 
				getString(R.string.credits_osm) + " / " + 
				getString(R.string.credits_leaflet) + " / " + 
				getString(R.string.credits_prevel) + " / " + 
				getString(R.string.credits_gson) + " / " + 
				getString(R.string.credits_vpi);
		creditsTextView.setText(Html.fromHtml(creditsText));
		creditsTextView.setMovementMethod(LinkMovementMethod.getInstance());
		
		aboutDialogView.findViewById(R.id.about_facebook).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(Utils.getOpenFacebookIntent(getActivity()));
			}
		});
		
		aboutDialogView.findViewById(R.id.bugsense).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.bugsense.com")));
			}
		});
		
		return new AlertDialog.Builder(getActivity())
				.setView(aboutDialogView)
				.setNeutralButton(getString(R.string.close), null)
				.create();
	}

}
