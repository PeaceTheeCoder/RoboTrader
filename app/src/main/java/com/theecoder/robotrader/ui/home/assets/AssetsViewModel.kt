package com.theecoder.robotrader.ui.home.assets

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theecoder.robotrader.network.module.*
import com.theecoder.robotrader.repository.RTRepository
import com.theecoder.robotrader.utils.Resource
import kotlinx.coroutines.launch
import retrofit2.Response

class AssetsViewModel( private val rtRepository: RTRepository
) : ViewModel() {

    val symbolsData: MutableLiveData<Resource<Symbols>> = MutableLiveData()


    fun getSymbols(authBody: AuthBody) = viewModelScope.launch {

        symbolsData.postValue(Resource.Loading())
        try {
            val response = rtRepository.getSymbols(authBody)
            symbolsData.postValue(handleSymbolsResponse(response))
        }catch (t: Throwable){
            symbolsData.postValue(Resource.Error("Oops! Something went wrong"))
        }

    }

    private fun handleSymbolsResponse(response: Response<Symbols>): Resource<Symbols>{
        if (response.isSuccessful){
            response.body()?.let {
                if (it.message == "accept"){
                    return Resource.Success(it)
                }

                return Resource.Error("Unknown Error Occurred!!")
            }
        }
        return Resource.Error(response.message())
    }

    fun saveSymbol(symbol: Symbol) = viewModelScope.launch {
        rtRepository.upsetSymbol(symbol)
    }

    fun getSavedSymbols(phone_secret_key:String) = rtRepository.getSavedSymbols(phone_secret_key)

    fun deleteSymbol(symbol: Symbol)  = viewModelScope.launch {
        rtRepository.deleteSymbol(symbol)
    }
    fun deleteAllSymbol(phone_secret:String) = viewModelScope.launch {
        rtRepository.deleteAllSymbol(phone_secret)
    }
}