package com.theecoder.robotrader.network.module

import com.google.gson.annotations.SerializedName

data class Account(
    @SerializedName("data")
    var Licence: Licence,
    var message: String
)