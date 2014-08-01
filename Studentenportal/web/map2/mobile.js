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
 
var baseUrl = 'https://campus.aau.at/api/map';
var mapBaseUrl = 'http://url.to/map/';
var ajaxProxyUrl = mapBaseUrl + 'proxy.php';

floorDefs = [
	[ 'Campus EG', 'e00' ],
	[ 'Campus E1', 'e01' ],
	[ 'Campus E2', 'e02' ],
	[ 'Campus E3', 'e03' ]
];

poiDefs = [ // name, id, style, visible, filterCampusLayer
	[ 'Access', 'aau:aau_access', 'aau_access', false, true ],
	[ 'Cafe', 'aau:poi', 'aau_cafe', false, true ],
	[ 'Computer', 'aau:poi', 'aau_computer', false, true ],
	[ 'Kopierer', 'aau:poi', 'aau_kopierer', false, true ],
	[ 'WC', 'aau:wc_pts', 'aau_wc', false, true ],
	[ 'Verkehr', 'aau:poi', 'aau_verkehr', false, false ],
	[ 'Weitere', 'aau:poi', 'aau_poi_anderes', false, true ]
];

var spi; // SP interface
var map;
var floorLayer, floorIndex;
var poiLayer;

function init() {
	spi = window.sp != null;
	var result = navigator.appVersion.match(/Android (\d+)/);
	var multitouch = (result != null && RegExp.$1 >= 4);

	map = L.map('map', {
		zoom: 14,
		center: [46.61627, 14.26488],
		maxZoom: 22,
		maxBounds: [
			[46.6286, 14.2464],
			[46.6066, 14.2842]
		],
		zoomControl: !multitouch,
		attributionControl: false,
		closePopupOnClick: false
	});

	L.tileLayer(baseUrl + '/tiles/osm/{z}/{x}/{y}.png', {
		maxZoom: 22
	}).addTo(map);

	showCampusLayer(0);
	zoomToUni();
}

function buildPoiLayer() {
	if (poiLayer != null) {
		map.removeLayer(poiLayer);
		poiLayer = null;
	}
	var layerIds = [], layerStyles = [], layerFilters = [];
	poiDefs.forEach(function(poiDef, index) {
		if(poiDef[3] == true) {
			layerIds.push(poiDef[1]);
			layerStyles.push(poiDef[2]);
			if(poiDef[4]) {
				layerFilters.push('(ebene=' + floorIndex + ')');
			} else {
				layerFilters.push('(ebene=0)');
			}
		}
	});
	if(layerIds.length == 0) return;
	poiLayer = L.tileLayer.wms(baseUrl + '/geoserver/wms', {
		layers: layerIds.join(','),
		styles: layerStyles.join(','),
		format: 'image/png',
		transparent: true,
		maxZoom: 22
	}).setParams({ 'CQL_FILTER': encodeURIComponent(layerFilters.join(';')) }).addTo(map);
}

/* transform calculations taken from OpenLayers: https://github.com/openlayers/openlayers/blob/master/lib/OpenLayers/Projection.js */
var Transform = {
	pole: 20037508.34,

    inverseMercator: function(latlng) {
        latlng.lng = 180 * latlng.lng / this.pole;
        latlng.lat = 180 / Math.PI * (2 * Math.atan(Math.exp((latlng.lat / this.pole) * Math.PI)) - Math.PI / 2);
        return latlng;
    },
	inverseMercator2: function(latlng) {
        latlng[0] = 180 * latlng[0] / this.pole;
        latlng[1] = 180 / Math.PI * (2 * Math.atan(Math.exp((latlng[1] / this.pole) * Math.PI)) - Math.PI / 2);
        return latlng;
    },

    forwardMercator: function(latlng) {
        latlng.lng = latlng.lng * this.pole / 180;
        latlng.lat = Math.log(Math.tan((90 + latlng.lat) * Math.PI / 360)) / Math.PI * this.pole;
        return latlng;
    },
	forwardMercator2: function(latlng) {
        latlng[0] = latlng[0] * this.pole / 180;
        latlng[1] = Math.log(Math.tan((90 + latlng[1]) * Math.PI / 360)) / Math.PI * this.pole;
        return latlng;
    }
}

var Room = {
	re: /HS[\.\s]?[A-Z0-9]{1,2}|[A-Z]{1,1}[0-9]?[\.\s][0-9]{1,1}[\.\s][0-9]{1,2}/g,
	hs: /^(HS)[\.\s]?([A-Z0-9]{1,2})$/,
	r: /^([A-Z]{1,1}[0-9]?)[\.\s]([0-9]{1,1})[\.\s]([0-9]{1,2})$/,
	
	detect: function(text) {
		return text.match(this.re);
	},
	
	normalize: function(room) {
		var result;
	
		if((result = room.match(this.hs)) != null) {
			return result[1] + ' ' + result[2];
		} else if((result = room.match(this.r)) != null) {
			return result[1] + '.' + result[2] + '.' + (result[3].length == 1 ? '0' : '') + result[3];
		}
		
		return null;
	}
}

var RoomSearch = {
	roomLayer: null,
	routingLayer: null,
	autocompleteRequest: null,
	
	RoutingMode: {
		DEFAULT: 0,
		ACCESSIBLE_WHEELCHAIR: 1
	},
	
	autocomplete: function(query, callback) {
		this.autocompleteRequest = pl.ajax({
			url:  ajaxProxyUrl,
			type: 'GET',
			data: { 
				'query': 'roomac',
				'q': query 
			},
			success: function(data) {
				var roomNames = [];
				try {
					data = JSON.parse(data);
					if(data.success) {
						roomNames = [];
						data.raume.forEach(function(room, index) { roomNames.push(room.raumname); });
					}
				} catch(e) {
					roomNames = null;
				}
				callback(roomNames);
			}
		});
	},
	cancelAutocomplete: function() {
		if(this.autocompleteRequest != null) {
			this.autocompleteRequest.abort();
			this.autocompleteRequest = null;
		}
	},

	clear: function() {
		if (this.roomLayer != null) {
			map.removeLayer(this.roomLayer);
		}
		if (this.routingLayer != null) {
			map.removeLayer(this.routingLayer);
		}
	},
	
	loadRoom: function(roomName) {
		this.clear();
		var _this = this;
		pl.ajax({
			url:  ajaxProxyUrl,
			type: 'GET',
			data: { 
				'query': 'room',
				'name': roomName 
			},
			success: function(data) {
				try {
					data = JSON.parse(data);
				} catch(e) {
					roomNotFound(roomName);
					return;
				}
				var markers = [];
				_this.roomLayer = L.geoJson(data, {
					pointToLayer: function (feature, latlng) {
						var marker = L.marker(Transform.inverseMercator(latlng), {
							clickable: false
						}).bindPopup(roomName);
						markers.push(marker);
						return marker;
					}
				}).addTo(map);
				if(markers.length > 0) {
					showCampusLayer(markers[0].feature.properties.ebene);
					map.panTo(markers[0].getLatLng());
					map.setZoom(19);
					markers[0].openPopup();
					getCampusLayers(); // update SP layer menu
				}
				workFinished();
			},
			error: function(errorNum, errorText) {
				roomNotFound(roomName);
			}
		});
	},

	loadRoute: function(from, to, routingMode) {
		routingMode = routingMode || 0;
		var routingModeInternal = routingMode;
		var _this = this;
		var campusLayer;
		
		if(routingMode == RoomSearch.RoutingMode.ACCESSIBLE_WHEELCHAIR) routingModeInternal = 2;
		this.clear();
		
		pl.ajax({
			url:  ajaxProxyUrl,
			type: 'GET',
			data: { 
				'query': 'route',
				'roomFrom': from,
				'roomTo': to,
				'type': routingModeInternal
			},
			success: function(data) {
				try {
					data = JSON.parse(data);
				} catch(e) {
					roomNotFound(from+'/'+to);
					return;
				}
				if(data.error) {
					roomNotFound(Room.detect(data.error)[0]);
					return;
				}
				for(var i = 0; i < data.features.length; i++) {
					var feature = data.features[i];
					var coords = feature.geometry.coordinates;
					for(var j = 0; j < coords.length; j++) {
						coords[j] = Transform.inverseMercator2(coords[j]);
					}
				}
				_this.routingLayer = L.geoJson(data, {
					style: function (feature) {
						campusLayer = feature.properties.layer;
						return { 
							color: feature.properties.myColor,
							weight: 5,
							opacity: 1
						};
					}
				}).addTo(map);
				showCampusLayer(campusLayer);
				getCampusLayers(); // update SP layer menu
				RoomSearch.loadRouteMarkers(from, to);
				map.fitBounds(_this.routingLayer.getBounds());
				workFinished();
			},
			error: function(errorNum, errorText) {
				//called if no route available
				roomNotFound('');
			}
		});
	},

	generateRouteMarker: function(point, image, caption) {
		var marker = L.marker([point[1], point[0]], { clickable: false });
		if(image != null) {
			marker.setIcon(new L.icon({
				iconUrl: baseUrl + '/resources/icons/map/' + image,
				iconSize: [20, 20]
			}));
		}
		if(caption != null) {
			marker.bindPopup(caption);
		}
		return marker;
	},
	
	loadRouteMarkers: function(from, to) {
		var _this = this;
		var markers = [];
		var currentFeature, previousFeature;
		_this.routingLayer.eachLayer(function (layer) {
			previousFeature = currentFeature;
			currentFeature = layer.feature;
			if(previousFeature != null) {
				var startPoint = layer.feature.geometry.coordinates[0];
				var floorDown = previousFeature.properties.layer > currentFeature.properties.layer;
				var image = null;
				switch(currentFeature.properties.type) {
					case '1': image = floorDown ? 'stairs_down.png' : 'stairs_up.png'; break;
					case '2': image = 'zebra.png'; break;
					case '3': image = 'stairs.png'; break;
					case '6': image = floorDown ? 'elevator_down_30px.png' : 'elevator_up_30px.png'; break;
					case '9': image = 'elevator.png'; break;
				}
				if(image != null) {
					markers.push(_this.generateRouteMarker(startPoint, image));
				}
			}
		});
		for(var i = 0; i < markers.length; i++) {
			_this.routingLayer.addLayer(markers[i]);
		}
		var toMarker = _this.generateRouteMarker(currentFeature.geometry.coordinates[currentFeature.geometry.coordinates.length - 1], null, to);
		_this.routingLayer.addLayer(toMarker);
		toMarker.openPopup();
	}
}

// SP -> JS

function getCampusLayers() {
	var layers = [];
	floorDefs.forEach(function(floorDef, i) { layers.push({ 
			index: i, 
			name: floorDef[0],
			visible: i == floorIndex
	});});
	if(spi) window.sp.setCampusLayers(JSON.stringify(layers));
	return layers;
}

function showCampusLayer(index) {
	if(index == floorIndex) return;
	if(floorLayer == null) {
		floorLayer = L.tileLayer(baseUrl + '/tiles/1.0.0/{level}/{z}/{x}/{y}.png', {
			level: floorDefs[index][1],
			tms: true,
			maxZoom: 22
		}).addTo(map);
	} else {
		floorLayer.options.level = floorDefs[index][1];
		floorLayer.redraw();
	}
	floorIndex = index;
	if(poiLayer != null) { 
		buildPoiLayer();
	}
}

function getPoiLayers() {
	var layers = [];
	poiDefs.forEach(function(poiDef, index) { layers.push({ 
			index: index, 
			name: poiDef[0],
			visible: poiDef[3]
	});});
	if(spi) window.sp.setPoiLayers(JSON.stringify(layers));
	return layers;
}

function showPoiLayer(index) {
	poiDefs[index][3] = true;
	buildPoiLayer();
}

function hidePoiLayer(index) {
	poiDefs[index][3] = false;
	buildPoiLayer();
}

function panTo(lon, lat) {
	map.panTo([lon, lat]);
}

function zoomToUni() {
	map.fitBounds([
		[46.61850, 14.26184],
		[46.61420, 14.26990]
	]);
}

function searchRoom(roomName) {
	roomName = unescape(roomName);
	RoomSearch.autocomplete(roomName, function(roomNames) {
		if(roomNames == null || roomNames.length == 0) {
			roomNotFound(roomName);
		} else {
			RoomSearch.loadRoom(roomNames[0]);
		}
	});
}

function searchRoute(from, to, mode) {
	from = unescape(from);
	to = unescape(to);
	RoomSearch.autocomplete(from, function(fromNames) {
		if(fromNames == null || fromNames.length == 0) {
			roomNotFound(from);
		} else {
			RoomSearch.autocomplete(to, function(toNames) {
				if(toNames == null || toNames.length == 0) {
					roomNotFound(to);
				} else {
					RoomSearch.loadRoute(fromNames[0], toNames[0], mode);
				}
			});
		}
	});
}

function autocompleteRoomName(query) {
	RoomSearch.autocomplete(query, function(roomNames) {
		if(roomNames != null) reportRoomAutocompletion(roomNames);
	});
}

function autocompleteRoomNameCancel() {
	RoomSearch.cancelAutocomplete();
}

// JS -> SP

function roomNotFound(roomName) { // AI
	workFinished();
	if(spi) {
		window.sp.roomNotFound(roomName);
	} else {
		alert('Room not found: ' + roomName);
	}
}

function workFinished() {
	if(spi) window.sp.workFinished();
}

function reportCurrentBounds() {
	var bounds = map.getBounds();
	if(spi) window.sp.reportCurrentBounds(bounds.getNorthWest().lon, bounds.getNorthWest().lat, bounds.getSouthEast().lon, bounds.getSouthEast().lat);
}

function reportRoomAutocompletion(roomNames) {
	if(spi) window.sp.reportRoomAutocompletion(JSON.stringify(roomNames)); else console.log(roomNames);
}



function roomTests() {
	console.log("------ ROOM NORMALIZATION ------");
	
	var roomvariants = ["HS1", "HSA", "HS 1", "HS A", "HS.1", "HS.A", "HS10", "HS 10", 
			"E.2.42", "E242", "E 2 42", "E 2 05", "E 2 5", "E.2.5", "L4.2.10", "L4 2 10", "L4 2 5"];
			
	roomvariants.forEach(function(rv) { 
		console.log(rv + " -> " + Room.normalize(rv));
	});
	
	
	console.log("------ ROOM DETECTION ------");
	
	var routevariants = ["E 2 5"/*wrong*/, "E.2.42 HS A", "E.2.42 HSA", "E 2 42 HS A"];
	routevariants.forEach(function(rv) {
		console.log(rv + " -> " + Room.detect(rv));
	});
}

function log(message) {
	if(spi) window.sp.log(message); else console.log(message);
}