package com.wadali.myapplication.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.wadali.myapplication.R;

import java.util.List;
import java.util.Locale;

/**
 * Created by jaswinderwadali on 30/05/17.
 */
@SuppressWarnings({"MissingPermission"})
public abstract class BaseActivity extends AppCompatActivity {


    private static final long MIN_TIME_BW_UPDATES = 10000;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 5;
    private boolean isGPSEnabled;
    private boolean isNetworkEnabled;
    private boolean canGetLocation;
    private ProgressDialog progressDialog;
    private Marker marker;
    private int LOCATION_SERVICE_SETTINGS_CODE = 4321;

    protected void goToUserLocation(GoogleMap googleMap) {
        googleMap.setMyLocationEnabled(true);
        Location myLocation = getLocation();
        if (myLocation != null) {
            double latitude = myLocation.getLatitude();
            double longitude = myLocation.getLongitude();
            drawMark(googleMap, latitude, longitude);
        }
    }

    protected void drawMark(GoogleMap googleMap, double latitude, double longitude) {
        if (marker != null) marker.remove();
        LatLng latLng = new LatLng(latitude, longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        String locationName = getMyLocationName(latitude, longitude);
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(locationName);
        marker = googleMap.addMarker(markerOptions);

    }

    protected String getMyLocationName(double latitude, double longitude) {
        String myPosition = "My Current Location";
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = null;
            addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && addresses.size() > 0) {
                String address = addresses.get(0).getAddressLine(0);
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                String knownName = addresses.get(0).getFeatureName();
                myPosition = String.format("%s%s%s", address, city, country);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return myPosition;
        }

    }

    public Location getLocation() {
        Location location = null;
        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!isGPSEnabled && !isNetworkEnabled) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setMessage(getResources().getString(R.string.gps_off));
                dialog.setPositiveButton(getString(R.string.settings), (paramDialogInterface, paramInt) -> {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(myIntent, LOCATION_SERVICE_SETTINGS_CODE);
                });
                dialog.setNegativeButton(getString(R.string.cancel), (paramDialogInterface, paramInt) -> {
                    finish();
                });
                dialog.setCancelable(false);
                dialog.show();

            } else {
                this.canGetLocation = true;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                }
            }
            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                if (location == null) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_SERVICE_SETTINGS_CODE) {
            onLocationChangeCall(getLocation());
        }
    }

    protected abstract void onLocationChangeCall(Location location);

    protected LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            onLocationChangeCall(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
            getLocation();
        }
    };

    protected void showProgress(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    protected void hideProgress() {
        if (progressDialog != null) progressDialog.dismiss();
    }

}

