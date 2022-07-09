package com.theecoder.robotrader.network.module

import com.google.gson.annotations.SerializedName

data class Symbols(
    @SerializedName("data")
    val symbols: MutableList<Symbol>,
    val message: String
)