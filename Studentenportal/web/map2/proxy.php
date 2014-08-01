<?php
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

	$query=$_GET["query"];
	$res = "";
	
	if($query == "room") {
		$url = 'https://campus.aau.at/api/map/search/room?name='.urlencode($_GET["name"]);
		$res = file_get_contents($url);
	}
	else if($query == "route") {
		$url = 'https://campus.aau.at/api/map/search/route?roomFrom='.urlencode($_GET["roomFrom"])
				.'&roomTo='.urlencode($_GET["roomTo"])
				.'&type='.urlencode($_GET["type"]);
		$res = file_get_contents($url);
	}
	else if($query == "roomac") {
		$url = 'https://campus.aau.at/api/map/webservices/search.php?query='.urlencode($_GET["q"]);
		$res = file_get_contents($url);
	}
	echo $res;
?>