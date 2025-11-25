package com.ptit.iot

import okhttp3.Interceptor
import okhttp3.Response

class HeaderAuthorizationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
        request.addHeader("Authorization", "Bearer ${RemoteModule.token}")
        return chain.proceed(request.build())
    }
}