package com.xingclass.app

import android.app.Application
import com.xingclass.app.di.AppContainer
import com.xingclass.app.di.DefaultAppContainer

class XingClassApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
