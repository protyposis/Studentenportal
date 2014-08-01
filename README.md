AAU Studentenportal
===================

This is the complete source code of the Android app "AAU Studentenportal" for students of the 
Alpen-Adria-Universität Klagenfurt (AAU / Klagenfurt University), Austria. Screenshots of the app can be 
found on the app's [Facebook page](http://facebook.com/Studentenportal). This app was actively developed
for over 2 years as a hobby project besides my duties as student and employee of the university. It was
used by a large fraction of students, but it never got officially approved, and it has been locked out from 
the university servers in August 2014.

The published code is the code that the last publicly released version was built from (v1.8.1), 
with an additional security fix, and cleaned from private and copyrighted data (see changes below). It was 
written with space/data efficiency and data privacy in mind, and contains a lot of Android best practices 
and patterns. Development was stopped in August 2013 and it isn't updated to the latest UI paradigms 
(e.g. a content centered start screen or hamburger navigation).


Features
--------

I started developing this app during my studies and tried to implement all functionality that I 
considered vital or at least important for a mobile app for university students and that I have 
missed when there wasn't any app existing yet.

* List of registered courses
  * List of participants in registered courses incl. portrait picture and contact info
  * Checklists for practical courses
  * Possibility to blacklist unattended courses
  * Moodle support
  * Webview for detailed data
* List of exams incl. modality info and results
* List of grades
  * Support for normal grades and Fachprüfungen
  * Webview for detailed stats
* Calendar with upcoming and canceled classes and exams
* Virtual student ID card incl. additional profile data of student and registered study programs 
* Bus/train departures all over Klagenfurt from the official public transportation system
* Mensa food menus
* Campus map with POIs (e.g. bathrooms, printers, coffee machines)
  * Navigation routing
  * Room and POI search
  * Linking from calendar (to show location of classrooms) and AAU website
* Location-aware ringtone muting
  * Mutes only if student is at university and an attended class takes place
  * Detection through WiFi and GSM network
* Instant notifications through cloud messaging
  * Study enrollment, course registration status changes (e.g. accepted), finished study phases, exam announcements, new grades
* Home screen widget with upcoming classes and exams
* Support for current and past semesters
* Login with university account (username/password) or student ID card (NFC)

The app communicates with a JSON REST API that I have implemented in the server side code running on the
university servers. The API client side consists of a JSON <-> Java classes serialization/deserialization layer
with built in request caching to avoid unnecessary network requests and data transfers, and speed up the user experience. 


Changes from the Play Store release
-----------------------------------

* To avoid copyright problems, all official university logos have been garbled by pixellation.
* API keys of the used Flurry Analytics and BugSense services have been replaced with dummy data.
* A crypto seed has been replaced with dummy data.
* The URL to the privately hosted campus map interface has been replaced by a dummy.
* Private signing keys have been deleted.


Credits
-------

This app uses the following 3rd party resources and libraries:

* Some icons taken from [Iconic](https://github.com/somerandomdude/Iconic) by P.J. Onori, [CC BY-SA 3.0](http://creativecommons.org/licenses/by-sa/3.0/)
* “Tracking Location” symbol by Friedrich Santana, “Map Marker” symbol by Nathan Borror, from [The Noun Project](http://thenounproject.com/) collection
* Map data (c) [OpenStreetMap](http://www.openstreetmap.org/) contributors, [CC-BY-SA](http://creativecommons.org/licenses/by-sa/2.0/)
* Leaflet Copyright (c) 2010-2011, CloudMade, Vladimir Agafonkin. All rights reserved. [license](https://raw.github.com/CloudMade/Leaflet/master/LICENSE)
* [Prevel](https://github.com/protyposis/Prevel) by Alexey Chernikov, [MIT License](http://opensource.org/licenses/MIT)
* [Gson](https://code.google.com/p/google-gson/) licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
* [ViewPagerIndicator](https://github.com/JakeWharton/Android-ViewPagerIndicator) by Jake Wharton, licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


License
-------

Copyright (C) 2014 Mario Guggenberger <mario.guggenberger@aau.at>. This project is released under 
the terms of the GNU General Public License. See `LICENSE` for details.
