package com.example.locationtrackertest.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.locationtrackertest.Database.LocationDatabase;
import com.example.locationtrackertest.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapLongClickListener {
    LocationManager locationManager;
    LocationListener locationListener;
    private final static int REQUEST_CHECK_SETTINGS_GPS = 0x1;
    private final static int REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;
    Button startTracking, stopTracking;
    private GoogleMap mMap;
    //To store longitude and latitude from map
    private double firstLong, firstLat;
    //From -> the first coordinate from where we need to calculate the distance
    private double currentLongitude;
    private double currentLatitude;
    //To -> the second coordinate to where we need to calculate the distance
    private double toLongitude;
    private double toLatitude;
    private Location mylocation;
    private GoogleApiClient googleApiClient;
    public static LocationDatabase locationDatabase;
    private com.example.locationtrackertest.Model.Location locationModel;
    private double firstLatitudeEntry;
    private double firstLongitudeEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpGClient();
        locationDatabase = Room.databaseBuilder(this, LocationDatabase.class, "User_locations").allowMainThreadQueries().build();
        locationModel = new com.example.locationtrackertest.Model.Location();
    }

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(currentLatitude, currentLongitude);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void initViews() {
        startTracking = findViewById(R.id.startTracking);
        stopTracking = findViewById(R.id.stopTracking);

    }

    private void initListeners() {
        startTracking.setOnClickListener(view -> {
            //start the process.
            startTracking.setEnabled(false);

            getMyLocation();
            stopTracking.setEnabled(true);

        });
        stopTracking.setOnClickListener(view -> {
            //perform stop functionality
            startTracking.setEnabled(true);
            stopTracking.setEnabled(false);
            locationManager.removeUpdates(locationListener);
            getLocationFromRoomDB();
            //zoom in
            moveMap();
            getDirection();
        });
    }

    private void getFirstLocationInDB() {
        int userid = locationModel.getId();
        List<com.example.locationtrackertest.Model.Location> locationList = locationDatabase.locationDAO().getUserLocationFromDBbyId(userid);


        for (int i = 0; i < locationList.size(); i++) {

            int firstDataId = locationList.get(0).getId();
            double firstDataLongitude = locationList.get(0).getLongitude();
            double firstDataLatitude = locationList.get(0).getLatitude();

            Toast.makeText(this, "user id: " + firstDataId + "\n Longitude: " + firstDataLongitude + "\n Latitude: " + firstDataLatitude, Toast.LENGTH_SHORT).show();
        }
    }

    private void getLastLocationInDb() {
        int userid = locationModel.getId();
        List<com.example.locationtrackertest.Model.Location> locationList = locationDatabase.locationDAO().getUserLocationFromDBbyId(userid);

        for (int i = 0; i < locationList.size(); i++) {
            com.example.locationtrackertest.Model.Location dd = locationList.get(locationList.size() - 1);
            int lastDataId = dd.getId();
            double lastDataLongitude = dd.getLongitude();
            double lastDataLatitude = dd.getLatitude();

            Toast.makeText(this, "user id: " + lastDataId + "\n Longitude: " + lastDataLongitude + "\n Latitude: " + lastDataLatitude, Toast.LENGTH_SHORT).show();
        }
    }


    private void getLocationFromRoomDB() {
        List<com.example.locationtrackertest.Model.Location> userLocations = locationDatabase.locationDAO().getUserLocationFromDB();
        for (com.example.locationtrackertest.Model.Location loc : userLocations) {
            List<com.example.locationtrackertest.Model.Location> locationList = locationDatabase.locationDAO().getUserLocationFromDBbyId(userLocations.get(0).getId());
            int id = locationList.get(0).getId();
            firstLongitudeEntry = locationList.get(0).getLongitude();
            firstLatitudeEntry = locationList.get(0).getLatitude();
            //Toast.makeText(this, "first entry user id: " + id + "\n Longitude: " + longitude + "\n Latitude: " + latitude, Toast.LENGTH_SHORT).show();


        }
//        for (int i = 0; i < userLocations.size(); i++) {
//
//            int firstDataId = userLocations.get(0).getId();
//            com.example.locationtrackertest.Model.Location dd = userLocations.get(userLocations.size() - (userLocations.size()-1));
//            double firstDataLongitude = dd.getLongitude();
//            double firstDataLatitude = dd.getLatitude();
//
//
//            Toast.makeText(this, "First user id: " + firstDataId + "\n Longitude: " + firstDataLongitude + "\n Latitude: " + firstDataLatitude, Toast.LENGTH_SHORT).show();
//        }
//
//        for (int i = 0; i < userLocations.size(); i++) {
//            com.example.locationtrackertest.Model.Location dd = userLocations.get(userLocations.size() - 1);
//            int lastDataId = dd.getId();
//            double lastDataLongitude = dd.getLongitude();
//            double lastDataLatitude = dd.getLatitude();
//
//            Toast.makeText(this, "Last user id: " + lastDataId + "\n Longitude: " + lastDataLongitude + "\n Latitude: " + lastDataLatitude, Toast.LENGTH_SHORT).show();
//        }
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

    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
            return;
        }

//        locationManager.removeUpdates(locationListener);
    }

    private void getMyLocation() {
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {
                int permissionLocation = ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setInterval(1000);
                    locationRequest.setFastestInterval(1000);
                    locationRequest.setSmallestDisplacement(10);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
                    builder.setAlwaysShow(true);
                    PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
                    result.setResultCallback(result1 -> {
                        final Status status = result1.getStatus();
                        switch (status.getStatusCode()) {
                            case LocationSettingsStatusCodes.SUCCESS:
                                // All location settings are satisfied.
                                // You can initialize location requests here.
                                int permissionLocation1 = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
                                if (permissionLocation1 == PackageManager.PERMISSION_GRANTED) {
                                    locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
                                    boolean isLocationEnabled = false;
                                    if (locationManager != null) {
                                        isLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                                    }
                                    initMap();
                                    initViews();
                                    initListeners();
                                    if (isLocationEnabled) {
                                        locationListener = new LocationListener() {

                                            @Override
                                            public void onLocationChanged(Location location) {
                                                currentLongitude = location.getLongitude();
                                                currentLatitude = location.getLatitude();
                                                //save the coords to db

                                                saveTheCoordsToDb();


                                            }

                                            @Override
                                            public void onStatusChanged(String s, int i, Bundle bundle) {

                                            }

                                            @Override
                                            public void onProviderEnabled(String s) {

                                            }

                                            @Override
                                            public void onProviderDisabled(String s) {

                                            }
                                        };
                                    }
                                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                                            != PackageManager.PERMISSION_GRANTED
                                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                            != PackageManager.PERMISSION_GRANTED) {

                                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                                    } else {

                                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                                    }
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


    private void saveTheCoordsToDb() {
        com.example.locationtrackertest.Model.Location location1 = new com.example.locationtrackertest.Model.Location();
        location1.setLatitude(currentLatitude);
        location1.setLongitude(currentLongitude);

        locationDatabase.locationDAO().addUserLocationToDB(location1);
        //Toast.makeText(MainActivity.this, "Data Saved to Db", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Location is needed. Please put it on and try again", Toast.LENGTH_SHORT).show();
                    checkPermissions();
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
            getMyLocation();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        int permissionLocation = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
            getMyLocation();
            initViews();
            initListeners();
        } else checkPermissions();
    }

    //Function to move the map
    private void moveMap() {
        //Creating a LatLng Object to store Coordinates
        LatLng latLng = new LatLng(currentLatitude, currentLatitude);

        //Adding marker to map
        mMap.addMarker(new MarkerOptions()
                .position(latLng) //setting position
                .draggable(true) //Making the marker draggable
                .title("Current Location")); //Adding a title

        //Moving the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        //Animating the camera
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }

    public String makeURL(double sourcelat, double sourcelog, double destlat, double destlog) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString
                .append(Double.toString(sourcelog));
        urlString.append("&destination=");// to
        urlString
                .append(Double.toString(destlat));
        urlString.append(",");
        urlString.append(Double.toString(destlog));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        urlString.append("&key=AIzaSyBj02AsPhErgBioUFU98Db5AENzi2sgcc4");
        return urlString.toString();
    }

    private void getDirection() {
        //Getting the URL
        String url = makeURL(firstLatitudeEntry, firstLongitudeEntry, currentLatitude, currentLongitude);

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

    //The parameter is the server response
    public void drawPath(String result) {
        //Getting both the coordinates
        LatLng from = new LatLng(firstLatitudeEntry, firstLongitudeEntry);
        LatLng to = new LatLng(currentLatitude, currentLongitude);

//        //Calculating the distance in meters
//        Double distance = SphericalUtil.computeDistanceBetween(from, to);

        //Displaying the distance
        //Toast.makeText(this, String.valueOf(distance + " Meters"), Toast.LENGTH_SHORT).show();


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