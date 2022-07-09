package com.theecoder.robotrader.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.theecoder.robotrader.repository.RTRepository



class ViewModelProviderFactory(
    private val rtRepository: RTRepository
): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(rtRepository) as T
    }
}
