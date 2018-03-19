package com.artbating.solly;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by kim on 2017. 3. 26..
 */

public interface ApiService {
    static final String SOLLY_URL = "https://solly-b6b12.firebaseio.com/";

    @GET("audio/{audiokey}.json")
    Call<JsonObject> getAudio(@Path("audiokey") String audioKey);
}
