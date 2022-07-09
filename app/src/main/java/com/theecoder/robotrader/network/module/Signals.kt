package com.theecoder.robotrader.network.module

import com.google.gson.annotations.SerializedName

data class Signals(
    @SerializedName("data")
    var signal: Signal,
    var message: String
)