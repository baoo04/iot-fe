package com.ptit.iot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptit.iot.CustomException
import com.ptit.iot.LoginRegisterRequest
import com.ptit.iot.LoginRegisterResponse
import com.ptit.iot.RemoteModule
import com.ptit.iot.Resource
import com.ptit.iot.UnknownException
import com.ptit.iot.onSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val _registerState = Channel<Resource<LoginRegisterResponse>>()
    val registerState = _registerState.receiveAsFlow()

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _registerState.send(Resource.Loading())
            try {
                val response = RemoteModule.mainApi.register(
                    LoginRegisterRequest(
                        email = email,
                        password = password,
                        name = email.substringBefore('@')
                    )
                )
                    .onSuccess {
                        RemoteModule.token = it.data?.accessToken ?: ""
                    }
                _registerState.send(response)
            } catch (e: Exception) {
                _registerState.send(Resource.Error(e as? CustomException ?: UnknownException(null, e.message, "register")))
            }
        }
    }
}
