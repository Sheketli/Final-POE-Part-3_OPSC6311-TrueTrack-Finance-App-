package com.example.truetrackfinance.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.truetrackfinance.R
import com.example.truetrackfinance.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * AuthActivity hosts the Login and Registration flows.
 * Optimized to prevent system UI (3-button nav) from overlapping the "Create Account" button.
 */
@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AuthActivity", "onCreate: Initializing secure auth shell")

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Requirement Fix: Ensure the Create Account button is not overlapped by 3-button nav ---
        // Dynamically apply system bar insets as padding to the root layout.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_auth, com.example.truetrackfinance.ui.auth.LoginFragment())
                .commit()
        }
    }
}
