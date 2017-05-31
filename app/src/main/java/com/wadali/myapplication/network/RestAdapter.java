package com.wadali.myapplication.network;

import com.wadali.myapplication.BuildConfig;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by jaswinderwadali on 30/05/17.
 */
public class RestAdapter {
    private static RestAdapter apolloRestAdapter;

    private ApiServices apiService;

    private RestAdapter() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL_MAP)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiServices.class);
    }

    public static RestAdapter getInstance() {
        if (apolloRestAdapter == null)
            apolloRestAdapter = new RestAdapter();
        return apolloRestAdapter;
    }

    public ApiServices getApiService() {
        return apiService;
    }


}
