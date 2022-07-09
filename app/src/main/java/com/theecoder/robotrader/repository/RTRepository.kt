package com.theecoder.robotrader.repository

import com.theecoder.robotrader.network.api.RetrofitInstance
import com.theecoder.robotrader.network.db.LicenceDB
import com.theecoder.robotrader.network.module.AuthBody
import com.theecoder.robotrader.network.module.Licence
import com.theecoder.robotrader.network.module.Sicence
import com.theecoder.robotrader.network.module.Symbol


class RTRepository(
    private val db : LicenceDB
) {
    suspend fun authenticate(authBody: AuthBody)=
        RetrofitInstance.api.authenticate(authBody = authBody)

    suspend fun getSignals(authBody: AuthBody)=
        RetrofitInstance.api.getSignals(authBody.phone_secret)

    suspend fun getSymbols(authBody: AuthBody)=
        RetrofitInstance.api.getSymbols(authBody.phone_secret)

    suspend fun getApp()= RetrofitInstance.api.getApp()

    //database functions
    suspend fun upsetLicence(licence: Licence) =
        db.getLicenceDao().upsert(licence)

     fun getLicences() =
        db.getLicenceDao().getDBLicences()

    suspend fun deleteLicence(licence: Licence) =
        db.getLicenceDao().deleteLicence(licence)

    //selected functions
    suspend fun upsetSel(licence: Sicence) =
        db.getLicenceDao().selectedupsert(licence)

    fun getSicences() =
        db.getLicenceDao().getSELicences()

    suspend fun deleteSicence(licence: Sicence) =
        db.getLicenceDao().deleteSicence(licence)

    //symbols functions db

    suspend fun upsetSymbol(symbol: Symbol) =
        db.getLicenceDao().upsetSymbol(symbol)

    fun getSavedSymbols(phone_secret:String) =
        db.getLicenceDao().getSavedSymbols(phone_secret)

    /*
    fun getDBSymbol(phone_secret:String, name: String) =
        db.getLicenceDao().getDBSymbol(phone_secret, name)*/

    suspend fun deleteSymbol(symbol: Symbol) =
        db.getLicenceDao().deleteSymbol(symbol)

    suspend fun deleteAllSymbol(phone_secret:String) =
        db.getLicenceDao().deleteAllSymbol(phone_secret)
}