package com.example.flatload

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*
import java.io.Serializable

//data class Loc (
//    @Expose
//    //var loc: Pair<Double,Double>
//    var locations: String
//) : Serializable

interface FlatAPI {
    @GET("/android/get")
    fun getJson(): Call<List<ResultGet>>

//    @GET("/android/get")
//    fun getJson(): Call<String>

//    @FormUrlEncoded
//    @POST("/android/post")
//    fun postJson(
//      @Field("locations") locations : String
//    ): Call<List<ResultGet>>

    @FormUrlEncoded
    @POST("/android/post")
    fun postJson(
        @Field("locations") locations : List<Pair<Double, Double>>
    ): Call<List<ResultGet>>


}