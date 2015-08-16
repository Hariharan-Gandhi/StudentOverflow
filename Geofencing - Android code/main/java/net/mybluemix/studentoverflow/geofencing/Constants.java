/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.location.sample.geofencing;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * Constants used in this sample.
 */
public final class Constants {

    private Constants() {
    }

    public static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";

    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES_NAME";

    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    /**
     * Used to set an expiration time for a geofence. After this amount of time Location Services
     * stops tracking the geofence.
     */
    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 72;

    /**
     * For this sample, geofences expire after twelve hours.
     */
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    public static final float GEOFENCE_RADIUS_IN_METERS = 100;

    /**
     * Map for storing information about airports in the University
     */
    public static final HashMap<String, LatLng> UNIVERSITY_LANDMARKS = new HashMap<String, LatLng>();
    static {

        UNIVERSITY_LANDMARKS.put("MENSA", new LatLng(49.994724, 8.245611));
        UNIVERSITY_LANDMARKS.put("LIBRARY", new LatLng(49.98, 8.245611));
        UNIVERSITY_LANDMARKS.put("COMPCENTER", new LatLng(49.97, 8.245611));
        UNIVERSITY_LANDMARKS.put("LABORATORY", new LatLng(49.96, 8.245611));
        UNIVERSITY_LANDMARKS.put("LECTUREHALL", new LatLng(49.95, 8.245611));

    }

    public interface ACTION {
        public static String MAIN_ACTION = "com.google.android.gms.location.sample.geofencing.GeofencingService.action.main";
        public static String PASSIVATE_ACTION = "com.google.android.gms.location.sample.geofencing.GeofencingService.action.prev";
        public static String ACTIVATE_ACTION = "com.google.android.gms.location.sample.geofencing.GeofencingService.action.play";
        public static String KILL_ACTION = "com.google.android.gms.location.sample.geofencing.GeofencingService.action.next";
        public static String STARTFOREGROUND_ACTION = "com.google.android.gms.location.sample.geofencing.GeofencingService.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.google.android.gms.location.sample.geofencing.GeofencingService.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
}
