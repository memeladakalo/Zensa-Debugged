package com.zensa.app

import android.app.Application
import com.zensa.app.di.ServiceLocator
import com.zensa.app.sync.SyncScheduler

class ZensaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        SyncScheduler.schedulePeriodic(this)
    }
}
