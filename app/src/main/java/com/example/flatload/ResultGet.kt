package com.example.flatload

import com.google.gson.annotations.SerializedName
import com.mapbox.geojson.Point

data class ResultGet(/* 서버에서 받는 json data class */
    @SerializedName("location")
    var location:String,
    @SerializedName("image")
    var image:String
)
