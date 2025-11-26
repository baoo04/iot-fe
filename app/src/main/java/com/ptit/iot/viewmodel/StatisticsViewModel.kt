package com.ptit.iot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptit.iot.CustomException
import com.ptit.iot.HeartRateResponse
import com.ptit.iot.ProfileResponse
import com.ptit.iot.RemoteModule
import com.ptit.iot.Resource
import com.ptit.iot.UnknownException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class StatisticsViewModel : ViewModel() {

    private val _heartState = Channel<Resource<HeartRateResponse>>()
    val heartState = _heartState.receiveAsFlow()

    private val _profileState = Channel<Resource<ProfileResponse.Data>>()
    val profileState = _profileState.receiveAsFlow()

    fun loadHeartRate() {
        viewModelScope.launch {
            _heartState.send(Resource.Loading())
            try {
                val response = RemoteModule.mainApi.getRealtimeHeart()
                _heartState.send(response)
            } catch (e: Exception) {
                _heartState.send(Resource.Error(e as? CustomException ?: UnknownException(null, e.message, "getRealtimeHeart")))
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.send(Resource.Loading())
            try {
                when (val response = RemoteModule.mainApi.getProfile()) {
                    is Resource.Success -> {
                        val data = response.data?.data
                        if (data != null) _profileState.send(Resource.Success(data))
                    }
                    is Resource.Error -> {
                        _profileState.send(Resource.Error(response.error))
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _profileState.send(Resource.Error(e as? CustomException ?: UnknownException(null, e.message, "getProfile")))
            }
        }
    }


}
