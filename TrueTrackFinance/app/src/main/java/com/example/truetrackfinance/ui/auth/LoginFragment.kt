package com.example.truetrackfinance.ui.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.truetrackfinance.R
import com.example.truetrackfinance.databinding.FragmentLoginBinding
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthState
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "LoginFragment"

/**
 * LoginFragment handles existing user authentication.
 * Includes a security lockout mechanism with a countdown timer after 3 failed attempts.
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private var lockoutTimer: CountDownTimer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing login shell")
        
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            
            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Please enter username and password", Toast.LENGTH_SHORT).show()
            } else {
                Log.i(TAG, "Action: Submitting login for user '$username'")
                viewModel.login(username, password)
            }
        }

        binding.btnTabRegister.setOnClickListener {
            Log.v(TAG, "Action: Switching to Register view")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_auth, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            Log.v(TAG, "UI Update: AuthState shifted to ${state::class.simpleName}")
            
            // Standard state handling
            binding.btnLogin.isEnabled = state !is AuthState.Loading && state !is AuthState.AccountLocked

            when (state) {
                is AuthState.LoginSuccess -> {
                    Log.d(TAG, "Navigate: Entering main application")
                    startActivity(android.content.Intent(requireContext(), com.example.truetrackfinance.ui.MainActivity::class.java))
                    requireActivity().finish()
                }
                is AuthState.LoginFailed -> {
                    Toast.makeText(requireContext(), getString(R.string.login_failed, state.attemptsRemaining), Toast.LENGTH_LONG).show()
                }
                is AuthState.AccountLocked -> {
                    startLockoutCountdown(state.lockedUntilMs)
                }
                is AuthState.UserNotFound -> {
                    Toast.makeText(requireContext(), "Account not found. Please register first.", Toast.LENGTH_SHORT).show()
                }
                is AuthState.Error -> {
                    Toast.makeText(requireContext(), "Login Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    /**
     * Requirement: Show a countdown when the account is locked for 60 seconds.
     */
    private fun startLockoutCountdown(lockedUntilMs: Long) {
        val now = System.currentTimeMillis()
        val duration = (lockedUntilMs - now).coerceAtLeast(0)
        
        lockoutTimer?.cancel()
        lockoutTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                binding.btnLogin.text = getString(R.string.locked_countdown, seconds)
                binding.btnLogin.isEnabled = false
            }

            override fun onFinish() {
                binding.btnLogin.text = getString(R.string.login)
                binding.btnLogin.isEnabled = true
                viewModel.resetState() // Allow user to try again
                Log.i(TAG, "Security: Lockout period expired")
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lockoutTimer?.cancel()
        _binding = null
    }
}
