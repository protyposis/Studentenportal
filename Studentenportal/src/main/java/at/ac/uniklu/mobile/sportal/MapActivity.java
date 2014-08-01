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

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import at.ac.uniklu.mobile.sportal.ui.AlertDialogOnClickDismissHandler;
import at.ac.uniklu.mobile.sportal.ui.ProgressNotificationToggle;
import at.ac.uniklu.mobile.sportal.ui.WebViewTimeoutClient;
import at.ac.uniklu.mobile.sportal.util.ActionBarHelper;
import at.ac.uniklu.mobile.sportal.util.Analytics;

public class MapActivity extends Activity implements ProgressNotificationToggle {
	
	private static final String TAG = "MapActivity";
	private static final String MAP_URL = "http://url.to/map/";
	
	private static final int DIALOG_SEARCHING = 1;
	
	private static final int SEARCHMODE_DEFAULT = 0;
	private static final int SEARCHMODE_ACCESSIBLE_WHEELCHAIR = 1;
	
	private ActionBarHelper mActionBar;
	private WebView mWebView;
	private Gson mGson;
	
	private LayerManager mCampusLayers;
	private LayerManager mPoiLayers;
	private AutoCompleteTextView mSearchText;
	private View[] mSearchPanels;
	private int mActiveSearchPanel = 0;
	private AutoCompleteTextView[] mSearchTexts;
	private int mSearchMode = SEARCHMODE_DEFAULT;
	
	private Handler mHandler = new Handler();
	
	private boolean mPendingSearch;
	private String mPendingSearchQuery;

	@TargetApi(11)
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		mActionBar = new ActionBarHelper(this).setupHeader();
        
        mGson = new GsonBuilder().create();
        
        mWebView = (WebView) findViewById(R.id.web_view);
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY); // hide right scroll border on android < 3
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(false);
        mWebView.getSettings().setUseWideViewPort(false);
        mWebView.addJavascriptInterface(new SpJavaScriptInterface(), "sp");
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
        	/* disable hardware acceleration:
        	 * - results in better rendering performance on Android 3.x & 4.0
        	 * - avoids webkit bugs in Android 4.x with OpenLayers and Leaflet
        	 */
        	mWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        }
        mWebView.setWebViewClient(new WebViewTimeoutClient(30000) {
        	private boolean timeout;
        	@Override
        	public void onPageStarted(WebView view, String url, Bitmap favicon) {
        		super.onPageStarted(view, url, favicon);
        		progressNotificationOn();
        		timeout = false;
        	}
        	@Override
        	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        		progressNotificationOff();
        		if(failingUrl != null && failingUrl.equals(MAP_URL)) {
        			showTimeoutMessage();
        		}
        	}
        	@Override
        	public void onPageFinished(WebView view, String url) {
        		super.onPageFinished(view, url);
        		
        		if(MapActivity.this.isFinishing()) {
        			/* do nothing if activity has already been closed
        			 * (avoids android.view.WindowManager$BadTokenException because of
        			 * a following showDialog() call)
        			 */
        			return;
        		}
        		
        		// this method is only called once since the remaining data is loaded asynchronously
        		progressNotificationOff();
        		if(!timeout) {
	        		getCampusLayers();
	        		getPoiLayers();
	        		if(mPendingSearch) {
	        			mPendingSearch = false;
	        			searchRoom(mPendingSearchQuery);
	        		}
        		}
        	}
        	@Override
        	public void onTimeout() {
        		Log.d(TAG, "timeout");
        		timeout = true;
        		mWebView.stopLoading();
        		showTimeoutMessage();
        		Analytics.onEvent(Analytics.EVENT_MAP_UNAVAILABLE);
        	}
        });
        
        View uniPositionButton = findViewById(R.id.uni_position);
        uniPositionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				zoomToUni();
			}
		});
        
		View searchButton = findViewById(R.id.search);
		searchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mSearchPanels[mActiveSearchPanel].getVisibility() != View.GONE) {
					onSearchRequested(); // hide current form
				}
				mActiveSearchPanel = 0;
				onSearchRequested();
			}
		});
		
		View routeButton = findViewById(R.id.route);
		routeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mSearchPanels[mActiveSearchPanel].getVisibility() != View.GONE) {
					onSearchRequested(); // hide current form
				}
				mActiveSearchPanel = 1;
				onSearchRequested();
			}
		});
        
        View currentPositionButton = findViewById(R.id.current_position);
        currentPositionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
				Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				if(location == null) {
					Toast.makeText(MapActivity.this, R.string.map_error_nolocation, Toast.LENGTH_LONG).show();
				} else {
					Log.d(TAG, location.toString());
					panTo(location.getLongitude(), location.getLatitude());
				}
			}
		});
        
        View campusLayersButton = findViewById(R.id.campuslayers);
        campusLayersButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mCampusLayers == null || mCampusLayers.isEmpty()) return;
				
				AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
		        builder.setTitle(R.string.map_floors);
		        builder.setSingleChoiceItems(mCampusLayers.getListItems(), 
		        		mCampusLayers.getListSingleSelectionIndex(), 
		        		new DialogInterface.OnClickListener(){
		            public void onClick(DialogInterface dialog, int item) {
		            	dialog.dismiss();
		            	showCampusLayer(item);
		            	mCampusLayers.deselectAll();
		            	mCampusLayers.select(item);
		            	Analytics.onEvent(Analytics.EVENT_MAP_LAYERSWITCH_CAMPUS, 
		            			"layer", mCampusLayers.get(item).name);
		            }
		        });
		        builder.setNeutralButton(R.string.close, new AlertDialogOnClickDismissHandler());
		        builder.create().show();
			}
		});
        
        View poiLayersButton = findViewById(R.id.poilayers);
        poiLayersButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mPoiLayers == null || mPoiLayers.isEmpty()) return;
				
				AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
		        builder.setTitle(R.string.map_pois);
		        builder.setMultiChoiceItems(mPoiLayers.getListItems(), 
		        		mPoiLayers.getListMultiSelection(), 
		        		new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						dialog.dismiss();
						if(isChecked) {
							showPoiLayer(which);
							Analytics.onEvent(Analytics.EVENT_MAP_LAYERSWITCH_POI, 
			            			"layer", mPoiLayers.get(which).name);
						} else {
							hidePoiLayer(which);
						}
						mPoiLayers.toggle(which);
						
					}
				});
		        builder.setNeutralButton(R.string.close, new AlertDialogOnClickDismissHandler());
		        builder.create().show();
			}
		});
        
        AutoCompleteTextView searchInput1 = (AutoCompleteTextView)findViewById(R.id.searchpanel_input);
        AutoCompleteTextView searchInput2 = (AutoCompleteTextView)findViewById(R.id.searchpanel_input_from);
        AutoCompleteTextView searchInput3 = (AutoCompleteTextView)findViewById(R.id.searchpanel_input_to);
        mSearchTexts = new AutoCompleteTextView[] { searchInput1, searchInput2, searchInput3 };
        View.OnFocusChangeListener searchInputFocusListener = new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus) mSearchText = (AutoCompleteTextView)v;
			}
		};
		TextView.OnEditorActionListener searchInputActionListener = new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
				
				if(mActiveSearchPanel == 0)
					searchRoom(v.getText().toString());
				else
					searchRoute(mSearchTexts[1].getText().toString(), 
							mSearchTexts[2].getText().toString(), mSearchMode);
				
				mSearchPanels[mActiveSearchPanel].setVisibility(View.GONE);
				
				// hide keyboard
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
	            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	            
				return true;
			}
		};
		AdapterView.OnItemClickListener searchDropdownSelectionListener = new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mSearchText.onEditorAction(mSearchText.getImeOptions());
			}
		};
		TextWatcher searchInputWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				autocompleteRoomNameCancel();
				autocompleteRoomName(s.toString());
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		};
		for(AutoCompleteTextView si : mSearchTexts) {
			si.setOnFocusChangeListener(searchInputFocusListener);
			si.setOnEditorActionListener(searchInputActionListener);
			si.addTextChangedListener(searchInputWatcher);
			si.setOnItemClickListener(searchDropdownSelectionListener);
		}
        
        View searchPanel1 = findViewById(R.id.searchpanel);
        View searchPanel2 = findViewById(R.id.searchpanel2);
        mSearchPanels = new View[] { searchPanel1, searchPanel2 };
        
        findViewById(R.id.searchpanel_toggle_disability).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ImageButton ib = (ImageButton)v;
				mSearchMode = ib.getDrawable().getLevel() == SEARCHMODE_DEFAULT ? 
						SEARCHMODE_ACCESSIBLE_WHEELCHAIR : SEARCHMODE_DEFAULT;
				ib.setImageLevel(mSearchMode);
			}
		});
        
        // process search requests
        Uri data = getIntent().getData();
        if(data != null) {
        	String url = data.toString();
        	String room = url.substring(url.indexOf("?") + 1);
        	mPendingSearch = true;
        	mPendingSearchQuery = room;
        }
        
        loadMap();
	}
	
	private void loadMap() {
		progressNotificationOn();
		mWebView.loadUrl(MAP_URL);
	}
	
	private void showTimeoutMessage() {
		findViewById(R.id.timeout).setVisibility(View.VISIBLE);
	}

    @Override
    protected void onStart() {
    	super.onStart();
    	Analytics.onActivityStart(this, Analytics.ACTIVITY_MAP);
    }

    @Override
    protected void onStop() {
    	super.onStop();
    	Analytics.onActivityStop(this);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	if(id == DIALOG_SEARCHING) {
    		ProgressDialog d = new ProgressDialog(this);
			d.setIndeterminate(true);
			d.setCancelable(true);
			d.setMessage(getString(R.string.map_searching));
			return d;
    	}
    	return super.onCreateDialog(id);
    }
    
    @Override
    public void onBackPressed() {
    	if(mSearchPanels[mActiveSearchPanel].getVisibility() == View.VISIBLE) {
    		onSearchRequested();
    	} else {
    		super.onBackPressed();
    	}
    }
    
    /**
     * Toggles the visibility of the active search panel
     */
    @Override
    public boolean onSearchRequested() {
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	View searchInput = null;
    	
    	if(mActiveSearchPanel == 0) {
    		searchInput = mSearchTexts[0];
    	} else { 
    		searchInput = mSearchTexts[1];
    	}
    	
    	Animation slideInAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
		Animation slideOutAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_top);
		
		final View panelToHideAfterAnimation = mSearchPanels[mActiveSearchPanel];
		slideOutAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation arg0) {}
			@Override
			public void onAnimationRepeat(Animation arg0) {}
			@Override
			public void onAnimationEnd(Animation arg0) {
				panelToHideAfterAnimation.setVisibility(View.GONE);
			}
		});
    	
    	if(mSearchPanels[mActiveSearchPanel].getVisibility() == View.GONE) {
    		mSearchPanels[mActiveSearchPanel].setVisibility(View.VISIBLE);
    		mSearchPanels[mActiveSearchPanel].startAnimation(slideInAnimation);
			searchInput.requestFocus();
			imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
		} else {
			mSearchPanels[mActiveSearchPanel].startAnimation(slideOutAnimation);
			//mSearchPanels[mActiveSearchPanel].setVisibility(View.GONE);
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
		}
    	
    	return true;
    }

	@Override
	public void progressNotificationOn() {
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
	}

	@Override
	public void progressNotificationOff() {
		findViewById(R.id.progress).setVisibility(View.GONE);
	}
	
	private void getCampusLayers() {
		mWebView.loadUrl("javascript:getCampusLayers()");
	}
	
	private void showCampusLayer(int index) {
		mWebView.loadUrl("javascript:showCampusLayer(" + index + ")");
	}
	
	private void getPoiLayers() {
		mWebView.loadUrl("javascript:getPoiLayers()");
	}
	
	private void showPoiLayer(int index) {
		mWebView.loadUrl("javascript:showPoiLayer(" + index + ")");
	}
	
	private void hidePoiLayer(int index) {
		mWebView.loadUrl("javascript:hidePoiLayer(" + index + ")");
	}
	
	private void panTo(double lon, double lat) {
		mWebView.loadUrl("javascript:panTo(" + lon + "," + lat + ")");
	}
	
	private void zoomToUni() {
		mWebView.loadUrl("javascript:zoomToUni()");
	}
	
	private void searchRoom(String roomName) {
		if(isFinishing() || roomName == null || roomName.trim().length() == 0) return;
		roomName = roomName.trim();
		
		showDialog(DIALOG_SEARCHING);
		mWebView.loadUrl("javascript:searchRoom('" + roomName + "')");
		
		Log.d(TAG, "search room: " + roomName);
		Analytics.onEvent(Analytics.EVENT_MAP_SEARCH, "query", roomName);
	}
	
	private void searchRoute(String from, String to, int mode) {
		if(from == null || from.trim().length() == 0) return;
		from = from.trim();
		
		if(to == null || to.trim().length() == 0) return;
		to = to.trim();
		
		showDialog(DIALOG_SEARCHING);
		mWebView.loadUrl("javascript:searchRoute('" + from + "', '" + to + "', " + mode +")");
		
		Log.d(TAG, "search route: " + from + " -> " + to + " (" + mode + ")");
		Analytics.onEvent(Analytics.EVENT_MAP_SEARCH, 
				"queryFrom", from, "queryTo", to, "queryMode", mode+"");
	}
	
	private void autocompleteRoomName(String query) {
		mWebView.loadUrl("javascript:autocompleteRoomName('" + query + "')");
	}
	
	private void autocompleteRoomNameCancel() {
		mWebView.loadUrl("javascript:autocompleteRoomNameCancel()");
	}
	
	@SuppressWarnings("unused")
	private class SpJavaScriptInterface {

		SpJavaScriptInterface() {}
		
		@JavascriptInterface
		public void setCampusLayers(String jsonLayers) {
			List<Layer> layers = mGson.fromJson(jsonLayers, new TypeToken<List<Layer>>(){}.getType());
			for(Layer layer : layers) {
				Log.d(TAG, "Campus: " + layer);
			}
			mCampusLayers = new LayerManager(layers);
		}
		
		@JavascriptInterface
		public void setPoiLayers(String jsonLayers) {
			List<Layer> layers = mGson.fromJson(jsonLayers, new TypeToken<List<Layer>>(){}.getType());
			for(Layer layer : layers) {
				Log.d(TAG, "POI: " + layer);
			}
			mPoiLayers = new LayerManager(layers);
		}
		
		@JavascriptInterface
		public void log(String message) {
			Log.d(TAG, "SPI log: " + message);
		}

		@JavascriptInterface
        public void workFinished() {
        	mHandler.post(new Runnable() {
                public void run() {
                	if(!isFinishing()) {
                		removeDialog(DIALOG_SEARCHING);
                	}
                }
            });
        }
        
		@JavascriptInterface
        public void reportCurrentBounds(double left, double top, double right, double bottom) {
        	Log.d(TAG, "reportBounds: " + left + " " + top + " " + right + " " + bottom);
        }
        
		@JavascriptInterface
        public void roomNotFound(String room) {
        	Toast.makeText(MapActivity.this, 
        			getString(R.string.map_error_roomnotfound, room), 
        			Toast.LENGTH_SHORT).show();
        }
        
		@JavascriptInterface
        public void reportRoomAutocompletion(String roomNames) {
        	final List<String> roomNameList = mGson.fromJson(roomNames, new TypeToken<List<String>>(){}.getType());
        	mHandler.post(new Runnable() { // update on GUI thread
                public void run() {
                	ArrayAdapter<String> a = new ArrayAdapter<String>(MapActivity.this, 
                			android.R.layout.simple_dropdown_item_1line, 
                			roomNameList.toArray(new String[0]));
                    mSearchText.setAdapter(a);
                	a.notifyDataSetChanged(); // needed for the ac dropdown to be updated every time
                }
            });
        }
    }

	private static class Layer {
		
		public int index;
		public String name;
		
		public boolean visible;
		
		@Override
		public String toString() {
			return "Layer [index=" + index + ", name=" + name + "]";
		}
	}
	
	private static class LayerManager {
		
		private List<Layer> mLayers;
		
		public LayerManager(List<Layer> layers) {
			mLayers = layers;
		}
		
		public CharSequence[] getListItems() {
			CharSequence[] items = new CharSequence[mLayers.size()];
			for(Layer l : mLayers) {
				items[l.index] = l.name;
			}
			return items;
		}
		
		public int getListSingleSelectionIndex() {
			for(Layer l : mLayers) {
				if(l.visible) return l.index;
			}
			return -1;
		}
		
		public boolean[] getListMultiSelection() {
			boolean selectedItems[] = new boolean[mLayers.size()];
			for(Layer l : mLayers) {
				if(l.visible) selectedItems[l.index] = true;
			}
			return selectedItems;
		}
		
		public boolean isEmpty() {
			return mLayers.isEmpty();
		}
		
		public void select(int index) {
			mLayers.get(index).visible = true;
		}
		
		public void deselectAll() {
			for(Layer l : mLayers) {
				l.visible = false;
			}
		}
		
		public void toggle(int index) {
			mLayers.get(index).visible = !mLayers.get(index).visible;
		}
		
		public Layer get(int index) {
			return mLayers.get(index);
		}
	}
}
