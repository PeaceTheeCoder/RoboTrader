package com.theecoder.robotrader.network.module

import java.io.Serializable

data class Signal(
    var action: String,
    var asset: String,
    var latestupdate: String,
    var price: String,
    var sl: String,
    var time: String,
    var tp: String,
): Serializable