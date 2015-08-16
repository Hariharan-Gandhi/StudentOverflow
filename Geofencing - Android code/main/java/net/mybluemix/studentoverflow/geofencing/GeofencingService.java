package com.google.android.gms.location.sample.geofencing;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Map;

public class GeofencingService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private static final String TAG = "Geofencing Service";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * The list of geofences used in this sample.
     */
    protected ArrayList<Geofence> mGeofenceList;

    /**
     * Used to keep track of whether geofences were added.
     */
    private boolean mGeofencesAdded;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;

    /**
     * Used to persist application state about whether geofences were added.
     */
    private SharedPreferences mSharedPreferences;
    protected boolean mServiceStatus;

    public GeofencingService() {
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mGeofenceList = new ArrayList<Geofence>();

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

        // Retrieve an instance of the SharedPreferences object.
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                MODE_PRIVATE);

        // Get the value of mGeofencesAdded from SharedPreferences. Set to false as a default.
        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
        // Get the geofences used. Geofence data is hard coded in this sample.
        populateGeofenceList();

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Start Foreground Intent ");

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Intent passivateIntent = new Intent(this, GeofencingService.class);
            passivateIntent.setAction(Constants.ACTION.PASSIVATE_ACTION);
            PendingIntent ppassivateIntent = PendingIntent.getService(this, 0,
                    passivateIntent, 0);

            Intent activateIntent = new Intent(this, GeofencingService.class);
            activateIntent.setAction(Constants.ACTION.ACTIVATE_ACTION);
            PendingIntent pactivateIntent = PendingIntent.getService(this, 0,
                    activateIntent, 0);


            Intent killIntent = new Intent(this, GeofencingService.class);
            killIntent.setAction(Constants.ACTION.KILL_ACTION);
            PendingIntent pkillIntent = PendingIntent.getService(this, 0,
                    killIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_notification);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("BibCast")
                    .setTicker("Geofencing(Service Started Running)")
                    .setContentText("Geofencing service!!")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(
                            Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)

                    .setOngoing(true)
                    .addAction(android.R.drawable.btn_star_big_off,
                            "Passivate", ppassivateIntent)
                    .addAction(android.R.drawable.btn_star_big_off, "Activate",
                            pactivateIntent)
                    .addAction(android.R.drawable.ic_menu_help, "Stop",
                            pkillIntent).build();
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    notification);

            activate();

        } else if (intent.getAction().equals(Constants.ACTION.PASSIVATE_ACTION)) {
            Log.i(TAG, "Deactivate Service");
            passivate();
        } else if (intent.getAction().equals(Constants.ACTION.ACTIVATE_ACTION)) {
            Log.i(TAG, "Re-activate Service");
            activate();
        }
        else if (intent.getAction().equals(Constants.ACTION.KILL_ACTION)) {
            Log.i(TAG, "Kill Service");
            Toast.makeText(this, "Geofencing Service stopped", Toast.LENGTH_SHORT ).show();

            Log.i(TAG, "Received Stop Foreground Intent");
            Intent killIntent = new Intent(this, GeofencingService.class);
            killIntent
                    .setAction(Constants.ACTION.KILL_ACTION);
            stopService(killIntent);

        }else if (intent.getAction().equals(
                Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Stop Foreground Intent");
                stopForeground(true);
                stopSelf();

        }

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroy Method");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to GoogleApiClient");
        addGeofencingService();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     *
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    @Override
    public void onResult(Status status) {

            if (status.isSuccess()) {
                // Update state and save in shared preferences.
                mGeofencesAdded = !mGeofencesAdded;
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
                editor.commit();

                Toast.makeText(
                        this,
                        getString(mGeofencesAdded ? R.string.geofences_added :
                                R.string.geofences_removed),
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                // Get the status code for the error and log it using a user-friendly message.
                String errorMessage = GeofenceErrorMessages.getErrorString(this,
                        status.getStatusCode());
                Log.e(TAG, errorMessage);
            }
        }

    /**
     * This sample hard codes geofence data. A real app might dynamically create geofences based on
     * the user's location.
     */
    public void populateGeofenceList() {
        for (Map.Entry<String, LatLng> entry : Constants.UNIVERSITY_LANDMARKS.entrySet()) {

            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.getKey())

                            // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )

                            // Set the expiration duration of the geofence. This geofence gets automatically
                            // removed after this period of time.
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                            // Set the transition types of interest. Alerts are only generated for these
                            // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                            // Create the geofence.
                    .build());
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Method to register all listeners
     */
    public void activate() {
        if (!mServiceStatus) {
            mGoogleApiClient.connect();
            mServiceStatus = true;
            Toast.makeText(this, "Geofencing Service enabled", Toast.LENGTH_SHORT ).show();

        } else {
            Toast.makeText(this, "Geofencing Service already enabled", Toast.LENGTH_SHORT ).show();
            Log.i(TAG, "Service is Already Active");
        }
    }

    /**
     * Method to unregister all listeners.
     * Also called to temporarily stop Service from Notification
     */
    public void passivate() {

        if (mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting()) {

            Log.i(TAG, "Stopping LocalSiriServices Listeners");
            removeGeofencingService();

            Log.i(TAG, "Disconnecting Google API Client");
            mGoogleApiClient.disconnect();

            mServiceStatus = false;

            Toast.makeText(this, "Geofencing Service paused temporarily", Toast.LENGTH_SHORT ).show();
        } else {
            Toast.makeText(this, "Geofencing Servics already disabled", Toast.LENGTH_SHORT ).show();

            Log.i(TAG, "Service is Already Passive");
        }
    }


    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    public void addGeofencingService() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    public void removeGeofencingService() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

}
