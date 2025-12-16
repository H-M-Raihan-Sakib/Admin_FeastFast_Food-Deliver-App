package com.example.adminfeastfast

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Wait for 3 seconds (3000ms), then decide where to go
        Handler(Looper.getMainLooper()).postDelayed({

            // CHECK: Is the user already logged in?
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                // Case 1: User IS logged in -> Go directly to Main Dashboard
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                // Case 2: User is NOT logged in -> Go to Login Screen
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }

            // Close the Splash Screen so the user can't go back to it
            finish()

        }, 3000)
    }
}
