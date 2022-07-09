package com.theecoder.robotrader.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theecoder.robotrader.ControlService
import com.theecoder.robotrader.network.module.App
import com.theecoder.robotrader.network.module.AuthBody
import com.theecoder.robotrader.network.module.Signals
import com.theecoder.robotrader.repository.RTRepository
import com.theecoder.robotrader.utils.Resource
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import retrofit2.Response

class HomeViewModel(private val rtRepository: RTRepository) : ViewModel() {

    val tradingInProgress : MutableLiveData<Boolean> = MutableLiveData()
    val app : MutableLiveData<Resource<App>> = MutableLiveData()

    fun getTradingStatus(mService : ControlService) = viewModelScope.launch {
        mService.getStatus().collect {
            tradingInProgress.postValue(it)
        }
    }

    fun getApp() = viewModelScope.launch {
        try {
            app.postValue(handleappresponse(rtRepository.getApp()))
        }catch (t: Throwable)
        {
            app.postValue(Resource.Error("something went wrong "+t.toString()))
        }
    }
    private fun handleappresponse(res : Response<App>): Resource<App>{
        if (res.isSuccessful){
            res.body()?.let {
                if (it.message == "accept"){
                    return Resource.Success(it)
                }

                return Resource.Error("Unknown Error Occurred!!")
            }
        }
        return Resource.Error("something went wrong ")
    }
}