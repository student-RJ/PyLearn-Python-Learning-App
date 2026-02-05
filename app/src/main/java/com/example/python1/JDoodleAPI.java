package com.example.python1;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface JDoodleAPI {
    @POST("v1/execute")
    Call<JDoodleResponse> executeCode(@Body JDoodleRequest request);
}
