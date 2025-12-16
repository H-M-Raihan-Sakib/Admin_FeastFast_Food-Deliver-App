package com.example.adminfeastfast

import android.app.Application
import com.cloudinary.android.MediaManager

// 1. Extend the Application class
class CloudinaryApplication : Application() {

    override fun onCreate() {        super.onCreate()

        val config = HashMap<String, String>()
        config["cloud_name"] = "dgusrefb3"
        config["api_key"] = "817458878365566"
        config["api_secret"] = "aKQ-ftk1nwWq2ecAU7VjxOFF4KM"

        // Initialize Cloudinary
        MediaManager.init(this, config)
    }
}
