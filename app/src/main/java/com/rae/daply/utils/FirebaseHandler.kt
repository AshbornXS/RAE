package com.rae.daply.utils

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class FirebaseHandler : Application() {

        override fun onCreate() {
            super.onCreate()

            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        }
}