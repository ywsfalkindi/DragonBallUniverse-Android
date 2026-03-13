package com.saiyan.dragonballuniverse.network

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * DEBUG-ONLY: Trust-all OkHttpClient to bypass invalid/self-signed TLS certs.
 *
 * DO NOT use this for release builds.
 */
object UnsafeOkHttp {
    fun create(): OkHttpClient {
        val trustAllCerts =
            arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                    ) = Unit

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?,
                    ) = Unit

                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                },
            )

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        val sslSocketFactory = sslContext.socketFactory
        val trustManager = trustAllCerts[0] as X509TrustManager

        val hostnameVerifier = HostnameVerifier { _, _ -> true }

        return OkHttpClient
            .Builder()
            .sslSocketFactory(sslSocketFactory, trustManager)
            .hostnameVerifier(hostnameVerifier)
            .build()
    }
}
