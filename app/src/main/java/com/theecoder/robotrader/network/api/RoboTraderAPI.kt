package com.theecoder.robotrader.network.api

import com.theecoder.robotrader.network.module.*
import retrofit2.Response
import retrofit2.http.*

interface RoboTraderAPI {

    @POST("auth/")
    suspend fun authenticate(@Body authBody: AuthBody):Response<Account>


    @GET("signals/", )
    suspend fun getSignals(
        @Query("phone_secret") phone_secret: String?
    ):Response<Signals>

    @GET("auth/app/", )
    suspend fun getApp():Response<App>

    @GET("symbols/", )
    suspend fun getSymbols(
        @Query("phone_secret") phone_secret: String?
    ):Response<Symbols>

}