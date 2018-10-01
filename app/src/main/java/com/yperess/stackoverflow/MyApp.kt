// Copyright (c) 2018 Magicleap. All right reserved.

package com.yperess.stackoverflow

import android.app.Application
import timber.log.Timber

/**
 *
 */
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}