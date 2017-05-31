package com.wadali.myapplication.network;

import com.wadali.myapplication.models.MapPathModel;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by jaswinderwadali on 30/05/17.
 */

public interface ApiServices {

    @GET("/maps/api/directions/json?sensor=false&units=metric&mode=driving&alternatives=true")
    Observable<MapPathModel> getGeoPoints(@Query("origin") String source, @Query("destination") String destination);

}