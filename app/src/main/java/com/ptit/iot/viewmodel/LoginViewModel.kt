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

class LoginViewModel : ViewModel() {

    private val _loginState = Channel<Resource<LoginRegisterResponse>>()
    val loginState = _loginState.receiveAsFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.send(Resource.Loading())
            try {
                val response = RemoteModule.mainApi.login(
                    LoginRegisterRequest(
                        email = email,
                        password = password,
                        name = null
                    )
                )
                    .onSuccess {
                        RemoteModule.token = it.data?.accessToken ?: ""
                    }
                _loginState.send(response)
            } catch (e: Exception) {
                _loginState.send(Resource.Error(e as? CustomException ?: UnknownException(null, e.message, "login")))
            }
        }
    }
}
