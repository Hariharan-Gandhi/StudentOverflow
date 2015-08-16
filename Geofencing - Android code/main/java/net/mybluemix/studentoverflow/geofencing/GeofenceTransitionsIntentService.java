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

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Listener for geofence transition changes.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class GeofenceTransitionsIntentService extends IntentService {


    public String geoType = "";
    public List<Geofence> triggeringGeofences;
    protected static final String TAG = "geofence-transitions";

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );



            Toast.makeText(this, "Geofencing Working Notification", Toast.LENGTH_LONG).show();
            // Send notification and log the transition details.
            sendNotification(geofenceTransitionDetails);
            /**********************************************
            * Custom Codes
             ***********************************************/
            postToCloudant(getTransitionString(geofenceTransition));

            Log.i(TAG, geofenceTransitionDetails);
        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param context               The app context.
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private void sendNotification(String notificationDetails) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }


    public void postToCloudant(String type){

        Log.e(TAG, "Inside post To Cloudant");
        geoType = type;
        new async().execute(type);

    }

    private class async extends AsyncTask<String, String, String> {
        protected String doInBackground(String... type) {
            Log.e(TAG, "Inside doInBackground");
            try {
                //String link = "http://studentoverflow.mybluemix.net/res/updateUserLocation/1724,1,49.762,8.62732";
                String zone = "";
                String link = "http://studentoverflow.mybluemix.net/res/updateUserLocation/";
                Geofence geofence = triggeringGeofences.get(0);
                String nameOfGeofence = geofence.getRequestId();
                LatLng temp = Constants.UNIVERSITY_LANDMARKS.get(nameOfGeofence);
                Log.e(TAG, "Name of Geofence entered =" + nameOfGeofence);
                Log.e(TAG, "Entry of Exit: " + geoType);

                switch (nameOfGeofence){
                    case "MENSA":
                        if(!(geoType== "")){
                        if(geoType.equals("Entered") ){
                            Log.e(TAG, "Entry" + geoType);
                            zone = "5";
                        }

                        else{
                            Log.e(TAG, "Exit" + geoType);
                            zone = "0";
                        }}
                        else{
                            Log.e(TAG, "geoType note set" + geoType);
                        }
                        break;
                    case "LIBRARY":
                        zone = "2"; break;
                    case "COMPCENTER":
                        zone = "3"; break;
                    case "LABORATORY":
                        zone = "4"; break;
                    case "LECTUREHALL":
                        zone = "1"; break;
                }

                link = link + "172417" + "," + zone + "," + temp.latitude + "," + temp.longitude;
                Log.e(TAG, "REST URL to be reached:" + link);

                URL url = new URL(link);
                Log.e(TAG, "Inside doInBackground2");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                Log.e(TAG, "Inside doInBackground3");
                conn.setDoOutput(true);
                Log.e(TAG, "Inside doInBackground4");
                conn.setRequestMethod("POST");
                Log.e(TAG, "Inside doInBackground5");

                if (conn.getResponseCode() != 200) {
           /*         throw new RuntimeException("Failed : HTTP error code : "
                            + conn.getResponseCode());*/
                }

                conn.disconnect();
                Log.e(TAG, "Inside doInBackground6");
            }
            catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Exception Caught!!");
            }
            return "success";
        }

        protected void onPostExecute(Long result) {
            System.out.print("Server Reached");
        }

    }

}
