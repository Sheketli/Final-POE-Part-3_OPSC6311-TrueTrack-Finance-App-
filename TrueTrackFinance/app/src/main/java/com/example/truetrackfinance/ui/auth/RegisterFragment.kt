package com.example.truetrackfinance.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.truetrackfinance.databinding.FragmentRegisterBinding
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthState
import dagger.hilt.android.AndroidEntryPoint

/**
 * RegisterFragment handles new user account creation with Full Name, Username, Email, and Password.
 */
@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("RegisterFragment", "onViewCreated: Initializing detailed registration form")

        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            if (validate(fullName, username, email, password, confirm)) {
                Log.i("RegisterFragment", "Submitting registration: $username ($fullName)")
                viewModel.register(fullName, username, email, password)
            }
        }

        binding.btnTabSignin.setOnClickListener {
            Log.v("RegisterFragment", "Action: Returning to login")
            parentFragmentManager.popBackStack()
        }

        observeViewModel()
    }

    /**
     * Input validation logic for all mandatory registration fields.
     */
    private fun validate(name: String, user: String, email: String, pass: String, confirm: String): Boolean {
        if (name.length < 3) {
            Toast.makeText(requireContext(), "Full Name is too short", Toast.LENGTH_SHORT).show()
            return false
        }
        if (user.length < 6) {
            Toast.makeText(requireContext(), "Username must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show()
            return false
        }
        if (pass.length < 8) {
            Toast.makeText(requireContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return false
        }
        if (pass != confirm) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.RegisterSuccess -> {
                    Log.d("RegisterFragment", "Success: User registered and logged in")
                    startActivity(android.content.Intent(requireContext(), com.example.truetrackfinance.ui.MainActivity::class.java))
                    requireActivity().finish()
                }
                is AuthState.UsernameTaken -> {
                    Toast.makeText(requireContext(), "Username is already taken", Toast.LENGTH_SHORT).show()
                }
                is AuthState.EmailTaken -> {
                    Toast.makeText(requireContext(), "Email is already registered", Toast.LENGTH_SHORT).show()
                }
                is AuthState.Error -> {
                    Log.e("RegisterFragment", "Error: ${state.message}")
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
