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

package at.ac.uniklu.mobile.sportal.util;

import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;

/**
 * Helper class to support login with the uni@klu.CARD Mifare Classic card. It obtains the card's uid
 * which is used for authorization on the server.
 * 
 * NOTE: NFC with Mifare Classic support was added in API level 10, so do not instantiate this class on platforms with a lower level.
 */
@TargetApi(10)
public class NfcLogin {
	
	private Activity mActivity;
	
	private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private IntentFilter[] mNfcFilters;
    private String[][] mNfcTechLists;
    
    public NfcLogin(Activity activity) {
    	mActivity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        // setup NFC
        // http://stackoverflow.com/questions/5685946/nfc-broadcastreceiver-problem
        // http://stackoverflow.com/questions/5685770/nfc-intent-get-info-from-the-tag
    	// NOTE on devices without NFC, this method call returns NULL
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mActivity);
        
        mNfcPendingIntent = PendingIntent.getActivity(mActivity, 0, new Intent(mActivity, mActivity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        try {
            tech.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mNfcFilters = new IntentFilter[] { tech };
        
        // Mifare Classic are also NfcA, but in contrary to NfcA, MifareClassic support is optional
        // http://developer.android.com/reference/android/nfc/tech/MifareClassic.html
        mNfcTechLists = new String[][] { new String[] { NfcA.class.getName() } };
    }

	public void onResume() {
		if(mNfcAdapter != null) {
			mNfcAdapter.enableForegroundDispatch(mActivity, mNfcPendingIntent, mNfcFilters, mNfcTechLists);
		}
	}
	
	public void onPause() {
		if(mNfcAdapter != null) {
			mNfcAdapter.disableForegroundDispatch(mActivity);
		}
	}
	
	public String onNewIntent(Intent intent) {
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) { 
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG); 
            byte[] idData = tag.getId();
            
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < idData.length; i++) {
            	String byteString = Integer.toHexString(0xFF & idData[i]);
                hexString.append((byteString.length() == 1 ? "0" : "")).append(byteString);
            }
            return hexString.toString().toUpperCase(Locale.GERMAN);
        }
		return null;
	}
	
	public boolean isAvailable() {
		return mNfcAdapter != null;
	}
	
	public boolean isEnabled() {
		return mNfcAdapter != null && mNfcAdapter.isEnabled();
	}
}
