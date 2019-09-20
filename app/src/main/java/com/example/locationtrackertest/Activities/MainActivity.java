package com.example.locationtrackertest.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.locationtrackertest.Database.LocationDatabase;
import com.example.locationtrackertest.R;
import com.example.locationtrackertest.Utils.AppUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Request for location as the application starts and cater for denial or whatever
 */
public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final static int REQUEST_CHECK_SETTINGS_GPS = 0x1;
    private final static int REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;
    private android.location.Location mylocation;
    private GoogleApiClient googleApiClient;
    Button startTracking, stopTracking;
    private GoogleMap mMap;
    private double currentLongitude;
    private double currentLatitude;

    public static LocationDatabase locationDatabase;
    private com.example.locationtrackertest.Model.Location locationModel;
    private double firstLatitudeEntry;
    private double firstLongitudeEntry;
    private ProgressDialog loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpGClient();
        getMyLocation();
    }

    private synchronized void setUpGClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(android.location.Location location) {

        if (mylocation != null) {
            currentLatitude = location.getLatitude();
            currentLongitude = mylocation.getLongitude();
            saveTheCoordsToDb();
            //Toast.makeText(this, "gps  long:" + currentLongitude + "=== lat: " + currentLatitude, Toast.LENGTH_SHORT).show();
            moveMapStart();
            loading.dismiss();
        } else {
            getMyLocation();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        checkPermissions();
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Do whatever you need
        //You can display a message here
        Toast.makeText(this, "connection suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //You can display a message here
        Toast.makeText(this, "connection failed", Toast.LENGTH_SHORT).show();
    }

    private void getMyLocation() {
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {
                int permissionLocation = ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    mylocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setInterval(1000);
                    locationRequest.setFastestInterval(1000);
                    locationRequest.setSmallestDisplacement(10);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
                    builder.setAlwaysShow(true);
                    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
                    PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
                    result.setResultCallback(result1 -> {
                        final Status status = result1.getStatus();
                        switch (status.getStatusCode()) {
                            case LocationSettingsStatusCodes.SUCCESS:
                                // All location settings are satisfied.
                                // You can initialize location requests here.
                                int permissionLocation1 = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
                                if (permissionLocation1 == PackageManager.PERMISSION_GRANTED) {
                                    mylocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                                }
                                break;
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied.
                                // But could be fixed by showing the user a dialog.
                                try {
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    // Ask to turn on GPS automatically
                                    status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS_GPS);
                                } catch (IntentSender.SendIntentException e) {
                                    // Ignore the error.
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                // Location settings are not satisfied.
                                // However, we have no way
                                // to fix the
                                // settings so we won't show the dialog.
                                // finish();
                                break;
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS_GPS) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    getMyLocation();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(this, getString(R.string.location_needed), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void checkPermissions() {
        int permissionLocation = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
        } else {
            //getMyLocation();
            initMap();
            initViews();
            initListeners();
            locationDatabase = Room.databaseBuilder(this, LocationDatabase.class, "User_locations").allowMainThreadQueries().build();
            locationModel = new com.example.locationtrackertest.Model.Location();

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        int permissionLocation = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
            //getMyLocation();
            initMap();
            initViews();
            initListeners();
            locationDatabase = Room.databaseBuilder(this, LocationDatabase.class, "User_locations").allowMainThreadQueries().build();
            locationModel = new com.example.locationtrackertest.Model.Location();

        } else {
            checkPermissions();
        }
    }


    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-44, 3.47);
        mMap.addMarker(new MarkerOptions().position(sydney).title("User Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    /**
     * instantiate views
     */
    private void initViews() {
        startTracking = findViewById(R.id.startTracking);
        stopTracking = findViewById(R.id.stopTracking);

    }

    /**
     * instantiate click listeners
     */
    private void initListeners() {
        startTracking.setOnClickListener(view -> {
            //start the process.
            if (AppUtils.isLocationEnabled(this)) {
                startTracking.setEnabled(false);
                //Showing a dialog till we get the route
                loading = ProgressDialog.show(this, "Loading", "Please wait...", false, false);
                loading.show();
                //get location coords
                checkPermissions();
                getMyLocation();
                Toast.makeText(this, "This where you are :)", Toast.LENGTH_LONG).show();
                mMap.clear();
                stopTracking.setEnabled(true);
            } else {
                showDialogMessage("Put on your gps location", () -> {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    this.startActivity(myIntent);
                });
            }

        });
        stopTracking.setOnClickListener(view -> {
            //perform stop functionality
            startTracking.setEnabled(true);
            stopTracking.setEnabled(false);

            //save data to Room
            getLocationFromRoomDB();
            //zoom in
            moveMapEnd();
            //show tracker on map
            getDirection();
        });
    }

    private void showDialogMessage(String msg, MessageDialogInterface dialogInterface) {
        new MaterialDialog.Builder(this)
                .content(msg)
                .positiveText("Ok")
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .onPositive((dialog, which) -> dialogInterface.onClick())
                .show();
    }

    private void getLocationFromRoomDB() {
        List<com.example.locationtrackertest.Model.Location> userLocations = locationDatabase.locationDAO().getUserLocationFromDB();
        if (userLocations.size() > 0) {
            List<com.example.locationtrackertest.Model.Location> locationList = locationDatabase.locationDAO().getUserLocationFromDBbyId(userLocations.get(0).getId());
            int id = locationList.get(0).getId();
            firstLongitudeEntry = locationList.get(0).getLongitude();
            firstLatitudeEntry = locationList.get(0).getLatitude();
            //Toast.makeText(this, "first entry user id: " + id + "\n Longitude: " + firstLongitudeEntry + "\n Latitude: " + firstLatitudeEntry, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    private void saveTheCoordsToDb() {
        com.example.locationtrackertest.Model.Location location1 = new com.example.locationtrackertest.Model.Location();
        location1.setLatitude(currentLatitude);
        location1.setLongitude(currentLongitude);

        locationDatabase.locationDAO().addUserLocationToDB(location1);
        //Toast.makeText(this, "Data Saved to Db", Toast.LENGTH_SHORT).show();
    }

    //The parameter is the server response
    public void drawPath(String result) {
        //Getting both the coordinates
        LatLng from = new LatLng(firstLatitudeEntry, firstLongitudeEntry);
        LatLng to = new LatLng(currentLatitude, currentLongitude);

//        //Calculating the distance in meters
        double distance = SphericalUtil.computeDistanceBetween(from, to);
        DecimalFormat format = new DecimalFormat("##.00");
        String dist = format.format(distance);
        //Displaying the distance
        Toast.makeText(this, "You moved " + dist + " Meters", Toast.LENGTH_SHORT).show();


        try {
            //Parsing json
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);
            Polyline line = mMap.addPolyline(new PolylineOptions()
                    .addAll(list)
                    .width(20)
                    .color(Color.RED)
                    .geodesic(true)
            );


        } catch (JSONException e) {

        }
    }

    //Function to move the map
    private void moveMapEnd() {
        //Creating a LatLng Object to store Coordinates
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        //Adding marker to map
        mMap.addMarker(new MarkerOptions()
                .position(latLng) //setting position
                .draggable(true) //Making the marker draggable
                .title("Your End Location")); //Adding a title

        //Moving the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        //Animating the camera
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    //Function to move the map
    private void moveMapStart() {
        //Creating a LatLng Object to store Coordinates
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        //Adding marker to map
        mMap.addMarker(new MarkerOptions()
                .position(latLng) //setting position
                .draggable(true) //Making the marker draggable
                .title("Your Current Location")); //Adding a title

        //Moving the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        //Animating the camera
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
    }


    public String makeURL(double sourcelat, double sourcelog, double destlat, double destlog) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(sourcelat);
        urlString.append(",");
        urlString.append(sourcelog);
        urlString.append("&destination=");// to
        urlString.append(destlat);
        urlString.append(",");
        urlString.append(destlog);
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        urlString.append("&key=AIzaSyBj02AsPhErgBioUFU98Db5AENzi2sgcc4");
        return urlString.toString();
    }

    private void getDirection() {
        //Getting the URL
        String url = makeURL(firstLatitudeEntry, firstLongitudeEntry, currentLatitude, currentLatitude);

        //Showing a dialog till we get the route
        final ProgressDialog loading = ProgressDialog.show(this, "Getting Route", "Please wait...", false, false);

        //Creating a string request
        StringRequest stringRequest = new StringRequest(url,
                response -> {
                    loading.dismiss();
                    //Calling the method drawPath to draw the path
                    drawPath(response);
                },
                error -> loading.dismiss());

        //Adding the request to request queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    interface MessageDialogInterface {
        void onClick();
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}
