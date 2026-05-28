package com.example

import android.app.Application

class AutoReplyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RuleRepository.init(this)
        LogRepository.init(this)
    }
}
