package com.ptit.iot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptit.iot.CustomException
import com.ptit.iot.ProfileResponse
import com.ptit.iot.ProfileDto
import com.ptit.iot.RemoteModule
import com.ptit.iot.Resource
import com.ptit.iot.UnknownException
import com.ptit.iot.UpdateProfileRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _profileState = MutableStateFlow<Resource<ProfileResponse>>(Resource.idle())
    val profileState: StateFlow<Resource<ProfileResponse>> = _profileState

    private val _updateProfileState = MutableStateFlow<Resource<Unit>>(Resource.idle())
    val updateProfileState: StateFlow<Resource<Unit>> = _updateProfileState

    fun getProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            _profileState.value = Resource.loading()
            try {
                val response = RemoteModule.mainApi.getProfile()
                _profileState.value = response
            } catch (e: Exception) {
                _profileState.value = Resource.error(
                    e as? CustomException ?: UnknownException(
                        null,
                        e.message,
                        "getProfile"
                    )
                )
            }
        }
    }

    fun updateProfile(profile: ProfileDto, name: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateProfileState.value = Resource.loading()
            try {
                val response = RemoteModule.mainApi.updateProfile(
                    UpdateProfileRequest(profile = profile, name = name)
                )
                _updateProfileState.value = response
                // Refresh profile after successful update
                if (response is Resource.Success) {
                    getProfile()
                }
            } catch (e: Exception) {
                _updateProfileState.value = Resource.error(
                    e as? CustomException ?: UnknownException(
                        null,
                        e.message,
                        "updateProfile"
                    )
                )
            }
        }
    }
    
    fun clearUpdateState() {
        _updateProfileState.value = Resource.idle()
    }
}