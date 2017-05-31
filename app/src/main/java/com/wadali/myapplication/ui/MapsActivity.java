package com.wadali.myapplication.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.wadali.myapplication.R;
import com.wadali.myapplication.models.MapPathModel;
import com.wadali.myapplication.network.RestAdapter;
import com.wadali.myapplication.utils.Utils;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.wadali.myapplication.R.id.map;

/**
 * Created by jaswinderwadali on 30/05/17.
 */

public class MapsActivity extends BaseActivity {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 2212;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_PICKUP_CODE = 3312;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_DROP_CODE = 3313;
    private final String TAG = getClass().getSimpleName();
    private GoogleMap mMap = null;
    private TextView pickUpTv, dropTv;
    private LinearLayout routeContainerLayout;
    private Polyline line;
    private TextView routeLable;
    private LatLng dropLatLng, pickUpLatLng;
    private Marker picUpMark, dropMark;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        loadIds();
        if (!Utils.hasLocationPermission(this)) loadMap();
        else askPermissions();

    }

    private void loadIds() {
        routeContainerLayout = (LinearLayout) findViewById(R.id.container_place);
        pickUpTv = (TextView) findViewById(R.id.pickup_tv);
        routeLable = (TextView) findViewById(R.id.route_lable);
        dropTv = (TextView) findViewById(R.id.drop_tv);
        pickUpTv.setOnClickListener(v -> {
            findPlace(PLACE_AUTOCOMPLETE_REQUEST_PICKUP_CODE);
        });
        dropTv.setOnClickListener(v -> findPlace(PLACE_AUTOCOMPLETE_REQUEST_DROP_CODE));
    }

    private void loadMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            if (!Utils.hasLocationPermission(this)) goToUserLocation(mMap);
            else askPermissions();
        });

    }


    private void askPermissions() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission)
                .setMessage(R.string.ask_permissions)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION)).setNegativeButton(R.string.cancel, (dialogInterface, i) -> finish()).setCancelable(false)
                .create()
                .show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadMap();
                } else {
                    finish();
                    Toast.makeText(this, "Can't load map without permissions!", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PLACE_AUTOCOMPLETE_REQUEST_PICKUP_CODE:
                Place pickUpPlace = getLatLonResult(resultCode, data);
                if (pickUpPlace != null) {
                    pickUpLatLng = pickUpPlace.getLatLng();
                    pickUpTv.setText(pickUpPlace.getName());
                    if (dropLatLng != null) getPathRequest();
                }
                break;
            case PLACE_AUTOCOMPLETE_REQUEST_DROP_CODE:
                Place dropUpPlace = getLatLonResult(resultCode, data);
                if (dropUpPlace != null) {
                    dropLatLng = dropUpPlace.getLatLng();
                    dropTv.setText(dropUpPlace.getName());
                    if (pickUpLatLng != null) getPathRequest();

                }
                break;
        }
    }


    private void getPathRequest() {
        showProgress(getString(R.string.please_wait));
        String lat1 = String.valueOf(pickUpLatLng.latitude);
        String lat2 = String.valueOf(dropLatLng.latitude);
        String longi1 = String.valueOf(pickUpLatLng.longitude);
        String longi2 = String.valueOf(dropLatLng.longitude);
        String sourceLatLong = String.format("%s,%s", lat1, longi1);
        String destinationLatLong = String.format("%s,%s", lat2, longi2);

        Observable<MapPathModel> call = RestAdapter.getInstance().getApiService().getGeoPoints(sourceLatLong, destinationLatLong);
        call.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    drawPath(response);
                    hideProgress();
                });
        ;
    }

    private Place getLatLonResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Place place = PlaceAutocomplete.getPlace(this, data);
            return place;
        } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
            Status status = PlaceAutocomplete.getStatus(this, data);
            Log.i(TAG, status.getStatusMessage());
        } else if (resultCode == RESULT_CANCELED) {
            // The user canceled the operation.
        }
        return null;
    }


    private void drawPath(MapPathModel mapPathModel) {
        if (mMap != null) {
            mMap.clear();
            picUpMark.remove();
            dropMark.remove();
            routeContainerLayout.removeAllViews();
        }

        picUpMark = mMap.addMarker(new MarkerOptions().position(pickUpLatLng).icon(
                BitmapDescriptorFactory.fromResource(R.drawable.dot)));
        dropMark = mMap.addMarker(new MarkerOptions().position(dropLatLng).icon(
                BitmapDescriptorFactory.fromResource(R.drawable.dot)));
        if (mapPathModel.getRoutes().size() > 0) {
            drawPathLine(mapPathModel.getRoutes().get(0));

            routeLable.setVisibility(View.VISIBLE);
            for (MapPathModel.Route route : mapPathModel.getRoutes()) {
                String s = route.getSummary();
                TextView textView = (TextView) getLayoutInflater().inflate(R.layout.text_view, routeContainerLayout, false);
                textView.setText(s);
                routeContainerLayout.addView(textView);
                textView.setTag(route);
                textView.setOnClickListener(v -> drawPathLine((MapPathModel.Route) v.getTag()));
            }
        } else
            Toast.makeText(this, R.string.no_availble, Toast.LENGTH_SHORT).show();

    }

    private void drawPathLine(MapPathModel.Route route) {
        if (line != null) {
            line.remove();
        }
        MapPathModel.OverviewPolyline overviewPolylines = route.getOverviewPolyline();
        String encodedString = overviewPolylines.getPoints();
        List<LatLng> list = Utils.getPointsDecode(encodedString);
        PolylineOptions options = new PolylineOptions().width(10).color(Color.BLUE).geodesic(true);
        for (LatLng latLng : list)
            options.add(latLng);
        line = mMap.addPolyline(options);
        ridePath();
    }


    private void ridePath() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickUpLatLng);
        builder.include(dropLatLng);
        int padding = 100;
        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.setOnMapLoadedCallback(() -> mMap.animateCamera(cu));
    }


    private void findPlace(int requestCode) {
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                    .build(this);
            startActivityForResult(intent, requestCode);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }
    }

    @Override
    protected void onLocationChangeCall(Location location) {
        if (mMap != null) {
            super.drawMark(mMap, location.getLatitude(), location.getLongitude());
        }
    }


}
