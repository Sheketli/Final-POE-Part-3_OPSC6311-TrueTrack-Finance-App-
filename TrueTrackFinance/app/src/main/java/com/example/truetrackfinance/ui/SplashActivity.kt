package com.example.truetrackfinance.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.truetrackfinance.R
import com.example.truetrackfinance.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "SplashActivity"

/**
 * SplashActivity displays the brand logo and slogan with a loading indicator.
 * It gates the entry point to either the Auth screen or the main Dashboard.
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Reverting to custom XML splash to support the requested loading bar
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Log.d(TAG, "onCreate: Launching brand splash screen")

        // Wait for 2 seconds to show branding, then check session
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndNavigate()
        }, 2000)
    }

    private fun checkSessionAndNavigate() {
        if (sessionManager.isLoggedIn()) {
            Log.i(TAG, "Active session found. Navigating to Dashboard.")
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            Log.i(TAG, "No active session. Navigating to Auth.")
            startActivity(Intent(this, AuthActivity::class.java))
        }
        finish()
    }
}
