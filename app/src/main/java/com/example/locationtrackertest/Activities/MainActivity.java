package com.example.locationtrackertest.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.maps.android.SphericalUtil;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Request for location as the application starts and cater for denial or whatever
 */
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
    private boolean allPermissionsGranted;
    private boolean anyPermissionPermanentlyDenied;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askUserForPermission();
    }

    private void askUserForPermission() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        allPermissionsGranted = report.areAllPermissionsGranted();
                        anyPermissionPermanentlyDenied = report.isAnyPermissionPermanentlyDenied();
                        if (!anyPermissionPermanentlyDenied) {
                            if (allPermissionsGranted) {
                                initMap();
                                initViews();
                                initListeners();
                                locationDatabase = Room.databaseBuilder(MainActivity.this, LocationDatabase.class, "User_locations").allowMainThreadQueries().build();
                                locationModel = new com.example.locationtrackertest.Model.Location();
                            } else {
                                askUserForPermission();
                            }
                        } else {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getResources().getString(R.string.need_permission));
        builder.setMessage(getResources().getString(R.string.settings_permission_request));
        builder.setPositiveButton(getResources().getString(R.string.goto_settings), (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton(getResources().getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();

    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
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
            startTracking.setEnabled(false);
            //get location coords
            if (!anyPermissionPermanentlyDenied) {
                if (allPermissionsGranted) {
                    mMap.clear();
                    getMyLocation();
                    stopTracking.setEnabled(true);
                } else {
                    askUserForPermission();
                }
            } else {
                askUserForPermission();
            }


        });
        stopTracking.setOnClickListener(view -> {
            //perform stop functionality
            startTracking.setEnabled(true);
            stopTracking.setEnabled(false);
            if (locationListener != null) {
                locationManager.removeUpdates(locationListener);
            }
            //save data to Room
            getLocationFromRoomDB();
            //zoom in
            moveMapEnd();
            //show tracker on map
            getDirection();
        });
    }

    private void getLocationFromRoomDB() {
        List<com.example.locationtrackertest.Model.Location> userLocations = locationDatabase.locationDAO().getUserLocationFromDB();
        if (userLocations.size() > 0) {
            List<com.example.locationtrackertest.Model.Location> locationList = locationDatabase.locationDAO().getUserLocationFromDBbyId(userLocations.get(0).getId());
            int id = locationList.get(0).getId();
            firstLongitudeEntry = locationList.get(0).getLongitude();
            firstLatitudeEntry = locationList.get(0).getLatitude();
            //Toast.makeText(this, "first entry user id: " + id + "\n Longitude: " + firstLongitudeEntry + "\n Latitude: " + firstLatitudeEntry, Toast.LENGTH_SHORT).show();
        } else {
            firstLongitudeEntry = currentLongitude;
            firstLatitudeEntry = currentLatitude;
        }
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
        askUserForPermission();
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
        if (googleApiClient != null) {
            googleApiClient.stopAutoManage(this);
            googleApiClient.disconnect();
        }
        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }


    private void getMyLocation() {
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {

                LocationRequest locationRequest = new LocationRequest();
                locationRequest.setInterval(10000);
                locationRequest.setFastestInterval(10000);
                locationRequest.setSmallestDisplacement(10);
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
                builder.setAlwaysShow(true);
                PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
                result.setResultCallback(result1 -> {
                    final Status status = result1.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
                            boolean isLocationEnabled = false;
                            if (locationManager != null) {
                                isLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                            }

                            if (isLocationEnabled) {
                                locationListener = new LocationListener() {

                                    @Override
                                    public void onLocationChanged(Location location) {
                                        currentLongitude = location.getLongitude();
                                        currentLatitude = location.getLatitude();
                                        //save the coords to db
                                        saveTheCoordsToDb();
                                        moveMapStart();
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

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    Activity#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for Activity#requestPermissions for more details.
                                    askUserForPermission();
                                    return;

                                }
                            }
                            if (locationManager != null) {
                                if (allPermissionsGranted && !anyPermissionPermanentlyDenied) {

                                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                                }
                            }


                            break;

                    }
                });

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

    //The parameter is the server response
    public void drawPath(String result) {
        //Getting both the coordinates
        LatLng from = new LatLng(firstLatitudeEntry, firstLongitudeEntry);
        LatLng to = new LatLng(currentLatitude, currentLongitude);

//        //Calculating the distance in meters
        double distance = SphericalUtil.computeDistanceBetween(from, to);

        //Displaying the distance
        Toast.makeText(this, "You moved " + distance + " Meters", Toast.LENGTH_SHORT).show();


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

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
    }
}
