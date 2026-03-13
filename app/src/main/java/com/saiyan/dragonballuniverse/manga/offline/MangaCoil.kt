package com.saiyan.dragonballuniverse.manga.offline

import android.content.Context
import coil.ImageLoader
import com.saiyan.dragonballuniverse.BuildConfig
import com.saiyan.dragonballuniverse.network.UnsafeOkHttp

/**
 * Manga-only Coil configuration.
 *
 * We intentionally avoid overriding Coil's global singleton ImageLoader from MainActivity,
 * to prevent unintended side effects (including device/ROM-specific service churn).
 */
object MangaCoil {
    fun imageLoader(context: Context): ImageLoader {
        val appContext = context.applicationContext

        return if (BuildConfig.DEBUG) {
            ImageLoader.Builder(appContext)
                // DEBUG ONLY: trust-all TLS to bypass CertPathValidatorException for R2 images.
                .okHttpClient(UnsafeOkHttp.create())
                .build()
        } else {
            // Release: use default safe networking stack.
            ImageLoader.Builder(appContext).build()
        }
    }
}
