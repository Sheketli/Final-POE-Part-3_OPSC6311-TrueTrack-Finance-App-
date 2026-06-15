package com.example.truetrackfinance.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.truetrackfinance.R
import com.example.truetrackfinance.databinding.ActivityMainBinding
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.util.BiometricHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"
private const val INACTIVITY_TIMEOUT_MS = 60_000L // 60 seconds

/**
 * MainActivity handles core navigation and security (Biometric Lock).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing application shell")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        biometricHelper = BiometricHelper(this)
        setupNavigation()
        setupSecurityObserver()
    }

    private fun setupSecurityObserver() {
        authViewModel.getActiveUser().observe(this) { user ->
            user ?: return@observe
            
            // Only check security when the app is in the foreground
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                val now = System.currentTimeMillis()
                val diff = now - user.lastActive
                
                if (diff > INACTIVITY_TIMEOUT_MS && user.biometricEnabled) {
                    Log.i(TAG, "Security Gate: Inactivity timeout reached ($diff ms)")
                    biometricHelper.showBiometricPrompt(
                        onSuccess = {
                            Log.d(TAG, "Security Gate: Biometric success")
                            updateActivityTimestamp(user.id)
                        }
                    )
                } else {
                    // Update timestamp periodically but avoid triggering the observer 
                    // in a tight loop by only updating if a certain amount of time has passed
                    if (diff > 10_000L) { 
                        updateActivityTimestamp(user.id)
                    }
                }
            }
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation?.setupWithNavController(navController)
        binding.navigationRail.setupWithNavController(navController)

        binding.fabAddGlobal.setOnClickListener {
            Log.d(TAG, "Action: Global Add Expense triggered")
            navController.navigate(R.id.addExpenseFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.dashboardFragment, R.id.expensesFragment -> binding.fabAddGlobal.show()
                else -> binding.fabAddGlobal.hide()
            }
            binding.currentScreenName.text = destination.label
        }
    }

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume: Checking security status")
    }

    private fun updateActivityTimestamp(userId: Long) {
        authViewModel.updateActivityTimestamp(userId)
    }
}
