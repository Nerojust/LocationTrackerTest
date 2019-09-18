package com.example.locationtrackertest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    LocationManager locationManager;
    LocationListener locationListener;

    TextView longitude, latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        longitude = findViewById(R.id.longitude);
        latitude = findViewById(R.id.latitude);
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        boolean isLocationEnabled = false;
        if (locationManager != null) {
            isLocationEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }

        if (isLocationEnabled) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double currentLongitude = location.getLongitude();
                    double currentLatitude = location.getLatitude();
                    long currentTime = location.getTime();
                    float currentAccuracy = location.getAccuracy();
                    Toast.makeText(MainActivity.this, currentTime + "==" + currentAccuracy, Toast.LENGTH_SHORT).show();
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    try {
                        List<Address> addressList = geocoder.getFromLocation(currentLatitude, currentLongitude, 1);
                        longitude.setText(addressList.get(0).getAddressLine(0));
                        latitude.setText(addressList.get(0).getAdminArea());
                        Toast.makeText(MainActivity.this,
                                String.valueOf(addressList.get(0).getLatitude()) + " =="
                                        + String.valueOf(addressList.get(0).getLongitude()), Toast.LENGTH_SHORT).show();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            longitude.setText("getting location");
            latitude.setText("getting location");
            }
        }else {
            longitude.setText("access denied");
            latitude.setText("access denied");
        }

    }
}
