package com.fitnessrpg.app

import android.app.Application

/**
 * Application entry point. Dependency wiring (Supabase client, repositories,
 * the manual DI container) is initialised here as the rewrite progresses.
 */
class FitnessRpgApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // ServiceLocator.init(this)  // wired in when the data layer lands.
    }
}
